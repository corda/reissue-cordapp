package com.r3.corda.lib.reissuance.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.reissuance.contracts.ReissuanceLockContract
import com.r3.corda.lib.reissuance.states.ReissuanceLock
import net.corda.core.contracts.*
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.serialization.deserialize
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap

@InitiatingFlow
@StartableByRPC
class UnlockReissuedStates<T>(
    private val reissuedStateAndRefs: List<StateAndRef<T>>,
    private val reissuanceLock: StateAndRef<ReissuanceLock>,
    private val assetExitTransactionIds: List<SecureHash>,
    private val assetUnencumberCommand: CommandData,
    private val extraAssetUnencumberCommandSigners: List<AbstractParty> = listOf() // requester is always a signer
) : FlowLogic<SecureHash>() where T : ContractState {
    @Suspendable
    override fun call(): SecureHash {
        assetExitTransactionIds.map { hash ->
            serviceHub.attachments.openAttachment(hash)?.let { attachment ->
                attachment.openAsJAR().use {
                    var nextEntry = it.nextEntry
                    while (nextEntry != null && !nextEntry.name.startsWith("SignedTransaction")) {
                        nextEntry = it.nextEntry
                    }
                    if (nextEntry != null) {
                        it.readBytes().deserialize<SignedTransaction>()
                    } else throw IllegalArgumentException("Transaction with id $hash not found")
                }
            }
        }

        val requester = reissuanceLock.state.data.requester
        val requesterHost = serviceHub.identityService.partyFromKey(requester.owningKey)!!
        require(requesterHost == ourIdentity) { "Requester is not a valid account for the host" }

        val notary = reissuanceLock.state.notary
        val lockSigners = listOf(requester.owningKey)

        require(!extraAssetUnencumberCommandSigners.contains(requester)) {
            "Requester is always a signer and shouldn't be passed in as a part of extraAssetUnencumberCommandSigners"
        }
        val signers = listOf(requester) + extraAssetUnencumberCommandSigners
        val reissuedStatesSigners = signers.map { it.owningKey }

        val transactionBuilder = TransactionBuilder(notary)

        reissuedStateAndRefs.forEach { reissuedStateAndRef ->
            transactionBuilder.addInputState(reissuedStateAndRef)
            transactionBuilder.addOutputState(reissuedStateAndRef.state.data)
        }
        transactionBuilder.addCommand(assetUnencumberCommand, reissuedStatesSigners)

        serviceHub.validatedTransactions.getTransaction(reissuanceLock.ref.txhash)?.let { signedTx ->
            signedTx.references.forEach {
                transactionBuilder.addReferenceState(serviceHub.toStateAndRef<ContractState>(it).referenced())
            }
        }

        val inactiveReissuanceLock = (reissuanceLock.state.data).copy(
            status = ReissuanceLock.ReissuanceLockStatus.INACTIVE
        )

        transactionBuilder.addInputState(reissuanceLock)
        transactionBuilder.addOutputState(inactiveReissuanceLock)
        transactionBuilder.addCommand(ReissuanceLockContract.Commands.Deactivate(), lockSigners)

        assetExitTransactionIds.forEach {
            transactionBuilder.addAttachment(it)
        }

        val localSigners = (lockSigners + reissuedStatesSigners)
            .distinct()
            .filter { serviceHub.identityService.partyFromKey(it)!! == ourIdentity }

        transactionBuilder.verify(serviceHub)
        var signedTransaction = serviceHub.signInitialTransaction(transactionBuilder, localSigners)

        // as some of the participants might be signers and some might not, we are sending them a flag which informs
        // them if they are expected to sign the transaction or not
        val otherParticipants = reissuanceLock.state.data.participants.filter { !signers.contains(it) }

        val signersSessions = subFlow(GenerateRequiredFlowSessions(signers))
        val otherParticipantsSessions = subFlow(GenerateRequiredFlowSessions(otherParticipants))
        subFlow(SendSignerFlags(signersSessions, otherParticipantsSessions))

        if (signersSessions.isNotEmpty()) {
            signedTransaction = subFlow(CollectSignaturesFlow(signedTransaction, signersSessions, localSigners))
        }

        return subFlow(
            FinalityFlow(
                transaction = signedTransaction,
                sessions = signersSessions + otherParticipantsSessions
            )
        ).id
    }

}

abstract class UnlockReissuedStatesResponder(
    private val otherSession: FlowSession
) : FlowLogic<SignedTransaction>() {

    lateinit var reissuanceLockInput: ReissuanceLock
    lateinit var reissuanceLockOutput: ReissuanceLock
    lateinit var otherInputs: List<StateAndRef<*>>
    lateinit var otherOutputs: List<TransactionState<*>>
    lateinit var nonContractAttachments: List<Attachment>

    fun checkBasicReissuanceConstraints(stx: SignedTransaction) {
        val ledgerTransaction = stx.tx.toLedgerTransaction(serviceHub)
        requireThat {
            "There are at least 2 inputs" using (ledgerTransaction.inputs.size > 1)
            "There are at least 2 outputs" using (ledgerTransaction.outputs.size > 1)

            nonContractAttachments = ledgerTransaction.attachments.filter { it !is ContractAttachment }
            "There is at least 1 non contract attachment" using (nonContractAttachments.isNotEmpty())

            val reissuanceLockInputs = ledgerTransaction.inputsOfType<ReissuanceLock>()
            val reissuanceLockOutputs = ledgerTransaction.outputsOfType<ReissuanceLock>()
            "There is exactly one input of type ReissuanceLock" using (reissuanceLockInputs.size == 1)
            "There is exactly one output of type ReissuanceLock" using (reissuanceLockOutputs.size == 1)
            reissuanceLockInput = reissuanceLockInputs[0]
            reissuanceLockOutput = reissuanceLockOutputs[0]
            "Status of input ReissuanceLock is ACTIVE" using (
                reissuanceLockInput.status == ReissuanceLock.ReissuanceLockStatus.ACTIVE)
            "Status of output ReissuanceLock is INACTIVE" using (
                reissuanceLockOutput.status == ReissuanceLock.ReissuanceLockStatus.INACTIVE)

            otherInputs = ledgerTransaction.inputs.filter { it.state.data !is ReissuanceLock }
            "Inputs other than ReissuanceLock are of the same type" using (
                otherInputs.map { it.state.data::class.java }.toSet().size == 1)
            "Inputs other than ReissuanceLock are encumbered" using otherInputs.none { it.state.encumbrance == null }

            otherOutputs = ledgerTransaction.outputs.filter { it.data !is ReissuanceLock }
            "Outputs other than ReissuanceLock are of the same type" using (
                otherOutputs.map { it.data::class.java }.toSet().size == 1)
            "Outputs other than ReissuanceLock are unencumbered" using otherOutputs.none { it.encumbrance != null }
        }
    }

    abstract fun checkConstraints(stx: SignedTransaction)

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
