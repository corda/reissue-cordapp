package com.r3.corda.lib.reissuance.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.ourIdentity
import com.r3.corda.lib.reissuance.constants.Constants.REISSUANCE_LOCK_STATE_UNTIL_ONLY_SECONDS
import com.r3.corda.lib.reissuance.constants.Constants.REISSUANCE_LOCK_TX_UNTIL_ONLY_SECONDS
import com.r3.corda.lib.reissuance.contracts.ReissuanceLockContract
import com.r3.corda.lib.reissuance.contracts.ReissuanceRequestContract
import com.r3.corda.lib.reissuance.schemas.ReissuanceDirection
import com.r3.corda.lib.reissuance.services.ReissuedStatesService
import com.r3.corda.lib.reissuance.states.*
import net.corda.core.contracts.*
import net.corda.core.contracts.Requirements.using
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.SignableData
import net.corda.core.crypto.SignatureMetadata
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.internal.requiredContractClassName
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.serialization.deserialize
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap
import java.time.Instant

@InitiatingFlow
@StartableByRPC
class ReissueStates<T>(
    private val reissuanceRequestStateAndRef: StateAndRef<ReissuanceRequest>,
    private val txAttachmentId: SecureHash,
    private val assetCreateCommand: CommandData,
    private val extraAssetCreateSigners: List<AbstractParty> = listOf(reissuanceRequestStateAndRef.state.data.issuer) // issuer is always a signer
): FlowLogic<SecureHash>() where T: ContractState {

    @Suspendable
    override fun call(): SecureHash {
        val tx = serviceHub.attachments.openAttachment(txAttachmentId)?.let { attachment ->
            attachment.openAsJAR().use {
                var nextEntry = it.nextEntry
                while (nextEntry != null && !nextEntry.name.startsWith("SignedTransaction")) {
                    // Calling `attachmentJar.nextEntry` causes us to scroll through the JAR.
                    nextEntry = it.nextEntry
                }
                if(nextEntry != null) {
                    it.readBytes().deserialize<SignedTransaction>()
                } else throw IllegalArgumentException("Transaction with id $txAttachmentId not found")
            }
        } ?: throw IllegalArgumentException("Transaction with id $txAttachmentId not found")

        val reissuedStatesService = serviceHub.cordaService(ReissuedStatesService::class.java)

        val reissuanceRequest = reissuanceRequestStateAndRef.state.data

        val notary = reissuanceRequestStateAndRef.state.notary
        val requester = reissuanceRequest.requester
        val issuer = reissuanceRequest.issuer
        val issuerHost = serviceHub.identityService.partyFromKey(issuer.owningKey)!!
        require(issuerHost == ourIdentity) { "Issuer is not a valid account for the host" }

        // we don't use withExternalIds when querying a vault as a transaction can be only shared with a host
        @Suppress("UNCHECKED_CAST")
        val statesToReissue: List<StateAndRef<T>> = serviceHub.vaultService.queryBy<ContractState>(
            criteria=QueryCriteria.VaultQueryCriteria(stateRefs = reissuanceRequest.stateRefsToReissue.map { it.ref })
        ).states as List<StateAndRef<T>>

        reissuanceRequest.stateRefsToReissue.forEach {
            val dir = ReissuanceDirection.RECEIVED
            require( !reissuedStatesService.hasStateRef(it.ref, dir)) { "State ${it} has been already re-issued" }
            reissuedStatesService.storeStateRef(it.ref, dir)
        }

        require(statesToReissue.size == reissuanceRequest.stateRefsToReissue.size) {
            "Cannot validate states to re-issue" }

        require(!extraAssetCreateSigners.contains(notary)) {
            "Notary is always a signer and shouldn't be passed in as a part of extraAssetCreateCommandSigners" }
        require(!extraAssetCreateSigners.contains(requester)) {
            "Requester is always a signer and shouldn't be passed in as a part of extraAssetCreateCommandSigners" }
        val reissuanceLock = ReissuanceLock(
            reissuanceRequest.issuer,
            reissuanceRequest.requester,
            SignableData(tx.id, SignatureMetadata(serviceHub.myInfo.platformVersion, Crypto.findSignatureScheme
                (serviceHub.ourIdentity.owningKey).schemeNumberID)),
            timeWindow = TimeWindow.untilOnly(Instant.now().plusSeconds(REISSUANCE_LOCK_STATE_UNTIL_ONLY_SECONDS))
        )

        val lockSigners = listOf(issuer.owningKey, requester.owningKey)
        val reissuedStatesSigners = reissuanceRequest.assetDestroySigners.map { it.owningKey }

        val transactionBuilder = TransactionBuilder(notary = notary)
        transactionBuilder.addInputState(reissuanceRequestStateAndRef)
        transactionBuilder.addCommand(ReissuanceRequestContract.Commands.Accept(), lockSigners)
        transactionBuilder.addAttachment(txAttachmentId)

        var encumbrance = 1
        statesToReissue
            .map { it.state.data }
            .forEach {
                val outputState = if (it is ReissuableState<*>) {
                    it.createReissuance()
                } else {
                    it
                }
                transactionBuilder.addOutputState(
                    state = outputState,
                    contract = it.requiredContractClassName!!,
                    notary = notary,
                    encumbrance = encumbrance)
                encumbrance += 1
            }

        transactionBuilder.addCommand(assetCreateCommand, extraAssetCreateSigners.map { it
            .owningKey }.plus(requester.owningKey))

        transactionBuilder.addOutputState(
            state = reissuanceLock,
            contract = ReissuanceLockContract.contractId,
            notary = notary,
            encumbrance = 0
        )
        transactionBuilder.addCommand(ReissuanceLockContract.Commands.Create(), lockSigners)
        transactionBuilder.setTimeWindow(TimeWindow.untilOnly(Instant.now().plusSeconds(REISSUANCE_LOCK_TX_UNTIL_ONLY_SECONDS)))

        val localSigners = (lockSigners + reissuedStatesSigners + serviceHub.ourIdentity.owningKey)
            .distinct()
            .filter { serviceHub.identityService.partyFromKey(it)!! == ourIdentity }
        transactionBuilder.verify(serviceHub)
        var signedTransaction = serviceHub.signInitialTransaction(transactionBuilder, localSigners)

        // as some of the participants might be signers and some might not, we are sending them a flag which informs
        // them if they are expected to sign the transaction or not
        val signers = (reissuanceRequest.assetDestroySigners + issuer + requester).distinct()
        val otherParticipants = reissuanceRequest.participants.filter { !signers.contains(it) }

        val signersSessions = subFlow(GenerateRequiredFlowSessions(signers))
        val otherParticipantsSessions = subFlow(GenerateRequiredFlowSessions(otherParticipants))
        subFlow(SendSignerFlags(signersSessions, otherParticipantsSessions))

        if(signersSessions.isNotEmpty()) {
            signedTransaction = subFlow(CollectSignaturesFlow(signedTransaction, signersSessions))
        }

        return subFlow(
            FinalityFlow(
                transaction = signedTransaction,
                sessions = signersSessions + otherParticipantsSessions
            )
        ).id
    }

}

abstract class ReissueStatesResponder(
    private val otherSession: FlowSession
) : FlowLogic<SignedTransaction>() {

    lateinit var reissuanceRequest: ReissuanceRequest
    lateinit var reissuanceLock: ReissuanceLock
    lateinit var otherOutputs: List<TransactionState<*>>

    fun checkBasicReissuanceConstraints(stx: SignedTransaction) {
        val ledgerTransaction = stx.tx.toLedgerTransaction(serviceHub)
        requireThat {
            "There is exactly 1 input" using (ledgerTransaction.inputs.size == 1)
            "There are at least 2 outputs" using (ledgerTransaction.outputs.size > 1)

            val nullableReissuanceRequest = ledgerTransaction.inputs[0].state.data as? ReissuanceRequest
            "ReissuanceRequest is an input" using (nullableReissuanceRequest != null)
            reissuanceRequest = nullableReissuanceRequest!!

            val reissuanceLocks = ledgerTransaction.outputsOfType(ReissuanceLock::class.java)
            "ReissuanceLock is an output" using (reissuanceLocks.size == 1)
            otherOutputs = ledgerTransaction.outputs.filter { it.data !is ReissuanceLock }
            "Outputs other than ReissuanceLock are of the same type" using(
                otherOutputs.map { it.data::class.java }.toSet().size == 1)
            "Outputs other than ReissuanceLock are encumbered" using otherOutputs.none { it.encumbrance == null }
            reissuanceLock = reissuanceLocks[0]
            "Status or ReissuanceLock is ACTIVE" using (
                reissuanceLock.status == ReissuanceLock.ReissuanceLockStatus2.ACTIVE)

            val attachedTransactions = getAttachedLedgerTransaction(ledgerTransaction)

            if (attachedTransactions.singleOrNull { it.id == reissuanceLock.txHash.txId } == null) {
                throw IllegalArgumentException("Attached transaction doesn't match transaction from lock state")
            }

            val tx = attachedTransactions.single { it.id == reissuanceLock.txHash.txId }

            val stateAndRefsToReissue = tx.inputs.map {
                serviceHub.toStateAndRef<ContractState>(it)
            }

            if (stateAndRefsToReissue.first().state.data is ReissuableState<*>) {
                stateAndRefsToReissue.forEachIndexed { index, stateAndRef ->
                    val state = stateAndRef.state.data as ReissuableState<ContractState>
                    val reissuedState = otherOutputs[index].data
                    "StatesAndRef objects in ReissuanceLock must be the same as re-issued states" using (
                            state.isEqualForReissuance(reissuedState))
                }
            } else {
                "StatesAndRef objects in ReissuanceLock must be the same as re-issued states" using (
                    stateAndRefsToReissue.map { it.state.data } == otherOutputs.map { it.data })
            }
        }
    }

    abstract fun checkConstraints(stx: SignedTransaction)

    private fun getAttachedLedgerTransaction(tx: LedgerTransaction): List<SignedTransaction> {
        // Constraints on the included attachments.
        val nonContractAttachments = tx.attachments.filter { it !is ContractAttachment }
        "The transaction should have at least one non-contract attachment" using (nonContractAttachments.isNotEmpty())

        val attachedSignedTransactions = mutableListOf<SignedTransaction>()
        nonContractAttachments.forEach { attachment ->
            val attachmentJar = attachment.openAsJAR()
            var nextEntry = attachmentJar.nextEntry
            while (nextEntry != null && !nextEntry.name.startsWith("SignedTransaction")) {
                // Calling `attachmentJar.nextEntry` causes us to scroll through the JAR.
                nextEntry = attachmentJar.nextEntry
            }

            if(nextEntry != null) {
                val transactionBytes = attachmentJar.readBytes()
                attachedSignedTransactions.add(transactionBytes.deserialize<SignedTransaction>())
            }

        }

        return attachedSignedTransactions
    }

    @Suspendable
    override fun call(): SignedTransaction {
        val needsToSignTransaction = otherSession.receive<Boolean>().unwrap { it }
        // only sign if instructed to do so
        if (needsToSignTransaction) {
            subFlow(object : SignTransactionFlow(otherSession) {
                override fun checkTransaction(stx: SignedTransaction) {
                    checkBasicReissuanceConstraints(stx)
                    checkConstraints(stx)
                }
            })
        }
        // always save the transaction
        return subFlow(ReceiveFinalityFlow(otherSideSession = otherSession))
    }
}
