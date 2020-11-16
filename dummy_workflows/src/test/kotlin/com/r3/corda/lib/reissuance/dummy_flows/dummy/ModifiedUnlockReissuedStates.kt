package com.r3.corda.lib.reissuance.dummy_flows.dummy

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.reissuance.contracts.ReissuanceLockContract
import com.r3.corda.lib.reissuance.flows.GenerateRequiredFlowSessions
import com.r3.corda.lib.reissuance.flows.SendSignerFlags
import com.r3.corda.lib.reissuance.states.ReissuanceLock
import com.r3.corda.lib.reissuance.utils.convertSignedTransactionToByteArray
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap


@InitiatingFlow
@StartableByRPC
class ModifiedUnlockReissuedStates<T>(
    private val reissuedStateAndRefs: List<StateAndRef<T>>,
    private val reissuanceLock: StateAndRef<ReissuanceLock<T>>,
    private val transactionByteArrays: List<ByteArray>,
    private val assetUnencumberCommand: CommandData,
    private val extraAssetUnencumberCommandSigners: List<AbstractParty> = listOf() // requester is always a signer
): FlowLogic<SecureHash>() where T: ContractState {
    @Suspendable
    override fun call(): SecureHash {

        val assetExitAttachments = transactionByteArrays.map { transactionByteArray ->
            serviceHub.attachments.importAttachment(transactionByteArray.inputStream(), ourIdentity.toString(), null)
        }

        // make it possible for other party to pretend to be the requester
        val requester = ourIdentity

        val notary = getPreferredNotary(serviceHub)
        val lockSigners = listOf(requester.owningKey)

        require(!extraAssetUnencumberCommandSigners.contains(requester)) {
            "Requester is always a signer and shouldn't be passed in as a part of extraAssetUnencumberCommandSigners" }
        val assetUpdateSigners = listOf(requester) + extraAssetUnencumberCommandSigners
        val reissuedStatesSigners = assetUpdateSigners.map { it.owningKey }

        val transactionBuilder = TransactionBuilder(notary)

        reissuedStateAndRefs.forEach { reissuedStateAndRef ->
            transactionBuilder.addInputState(reissuedStateAndRef)
            transactionBuilder.addOutputState(reissuedStateAndRef.state.data)
        }
        transactionBuilder.addCommand(assetUnencumberCommand, reissuedStatesSigners)

        var inactiveReissuanceLock = (reissuanceLock.state.data).copy(
            status = ReissuanceLock.ReissuanceLockStatus.INACTIVE)

        transactionBuilder.addInputState(reissuanceLock)
        transactionBuilder.addOutputState(inactiveReissuanceLock)
        transactionBuilder.addCommand(ReissuanceLockContract.Commands.Deactivate(), lockSigners)

        assetExitAttachments.forEach { deletedStateTransactionHash ->
            transactionBuilder.addAttachment(deletedStateTransactionHash)
        }

        val localSigners = (lockSigners + reissuedStatesSigners)
            .distinct()
            .filter { serviceHub.identityService.partyFromKey(it)!! == ourIdentity }

        transactionBuilder.verify(serviceHub)
        var signedTransaction = serviceHub.signInitialTransaction(transactionBuilder, localSigners)

        // as some of the participants might be signers and some might not, we are sending them a flag which informs
        // them if they are expected to sign the transaction or not
        val signers = (assetUpdateSigners + reissuanceLock.state.data.issuer).distinct()
        val otherParticipants = reissuanceLock.state.data.participants.filter { !signers.contains(it) }

        val signersSessions = subFlow(GenerateRequiredFlowSessions(signers))
        val otherParticipantsSessions = subFlow(GenerateRequiredFlowSessions(otherParticipants))
        subFlow(SendSignerFlags(signersSessions, otherParticipantsSessions))

        if(signersSessions.isNotEmpty()) {
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

@InitiatedBy(ModifiedUnlockReissuedStates::class)
class ModifiedUnlockReissuedStatesResponder(
    private val otherSession: FlowSession
) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val needsToSignTransaction = otherSession.receive<Boolean>().unwrap { it }
        // only sign if instructed to do so
        if (needsToSignTransaction) {
            subFlow(object : SignTransactionFlow(otherSession) {
                override fun checkTransaction(stx: SignedTransaction) { }
            })
        }
        // always save the transaction
        return subFlow(ReceiveFinalityFlow(otherSideSession = otherSession))
    }
}
