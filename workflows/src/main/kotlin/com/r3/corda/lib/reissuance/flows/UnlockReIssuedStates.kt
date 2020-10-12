package com.r3.corda.lib.reissuance.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import com.r3.corda.lib.reissuance.contracts.ReIssuanceLockContract
import com.r3.corda.lib.reissuance.states.ReIssuanceLock
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
class UnlockReIssuedStates<T>(
    private val reIssuedStateAndRefs: List<StateAndRef<T>>,
    private val reIssuanceLock: StateAndRef<ReIssuanceLock<T>>,
    private val deletedStateTransactionHashes: List<SecureHash>,
    private val assetUpdateCommand: CommandData, // unencumber state command
    private val extraAssetUpdateSigners: List<AbstractParty> = listOf()
): FlowLogic<SecureHash>() where T: ContractState {
    @Suspendable
    override fun call(): SecureHash {
        val requester = reIssuanceLock.state.data.requester
        val requesterHost = serviceHub.identityService.partyFromKey(requester.owningKey)!!
        require(requesterHost == ourIdentity) { "Requester is not a valid account for the host" }

        val notary = getPreferredNotary(serviceHub)
        val lockSigners = listOf(requester.owningKey)

        val assetUpdateSigners = listOf(reIssuanceLock.state.data.requester) + extraAssetUpdateSigners
        val reIssuedStatesSigners = assetUpdateSigners.map { it.owningKey }

        val transactionBuilder = TransactionBuilder(notary)

        reIssuedStateAndRefs.forEach { reIssuedStateAndRef ->
            transactionBuilder.addInputState(reIssuedStateAndRef)
            transactionBuilder.addOutputState(reIssuedStateAndRef.state.data)
        }
        transactionBuilder.addCommand(assetUpdateCommand, reIssuedStatesSigners)

        var inactiveReIssuanceLock = (reIssuanceLock.state.data).copy(
            status = ReIssuanceLock.ReIssuanceLockStatus.INACTIVE)

        transactionBuilder.addInputState(reIssuanceLock)
        transactionBuilder.addOutputState(inactiveReIssuanceLock)
        transactionBuilder.addCommand(ReIssuanceLockContract.Commands.Use(), lockSigners)

        deletedStateTransactionHashes.forEach { deletedStateTransactionHash ->
            transactionBuilder.addAttachment(deletedStateTransactionHash)
        }

        val localSigners = (lockSigners + reIssuedStatesSigners)
            .distinct()
            .filter { serviceHub.identityService.partyFromKey(it)!! == ourIdentity }

        transactionBuilder.verify(serviceHub)
        var signedTransaction = serviceHub.signInitialTransaction(transactionBuilder, localSigners)

        val signers = (assetUpdateSigners + reIssuanceLock.state.data.issuer).distinct()
        val otherParticipants = reIssuanceLock.state.data.participants.filter { !signers.contains(it) }

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

@InitiatedBy(UnlockReIssuedStates::class)
class UnlockReIssuedStatesResponder(
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
