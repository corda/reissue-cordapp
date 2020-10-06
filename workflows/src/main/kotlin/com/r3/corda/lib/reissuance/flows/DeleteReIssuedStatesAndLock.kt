package com.r3.corda.lib.reissuance.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import com.r3.corda.lib.reissuance.contracts.ReIssuanceLockContract
import com.r3.corda.lib.reissuance.states.ReIssuanceLock
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap

@InitiatingFlow
@StartableByRPC
class DeleteReIssuedStatesAndLock<T>(
    private val reIssuanceLockStateAndRef: StateAndRef<ReIssuanceLock<T>>,
    private val reIssuedStateAndRefs: List<StateAndRef<T>>,
    private val assetExitCommand: CommandData,
    private val assetExitSigners: List<AbstractParty> = listOf(reIssuanceLockStateAndRef.state.data.requester,
        reIssuanceLockStateAndRef.state.data.issuer)
): FlowLogic<Unit>() where T: ContractState {
    @Suspendable
    override fun call() {
        val notary = getPreferredNotary(serviceHub)

        val reIssuanceLock = reIssuanceLockStateAndRef.state.data
        val requester = reIssuanceLock.requester
        val issuer = reIssuanceLock.issuer
        val lockSigners = listOf(requester, issuer)
        val lockSignersKeys = lockSigners.map { it.owningKey }
        val reIssuedStatesSignersKeys = assetExitSigners.map { it.owningKey }

        val transactionBuilder = TransactionBuilder(notary)

        reIssuedStateAndRefs.forEach { reIssuedStateAndRef ->
            transactionBuilder.addInputState(reIssuedStateAndRef)
        }
        transactionBuilder.addCommand(assetExitCommand, reIssuedStatesSignersKeys)

        transactionBuilder.addInputState(reIssuanceLockStateAndRef)
        transactionBuilder.addCommand(ReIssuanceLockContract.Commands.Delete(), lockSignersKeys)

        val signers =(lockSigners + assetExitSigners).distinct()

        val localSigners = signers.filter { serviceHub.identityService.partyFromKey(it.owningKey)!! == ourIdentity }
        val localSignersKeys = localSigners.map { it.owningKey }

        transactionBuilder.verify(serviceHub)
        var signedTransaction = serviceHub.signInitialTransaction(transactionBuilder, localSignersKeys)

        val otherParticipants = reIssuanceLock.participants.filter { !signers.contains(it) }

        val signersSessions = subFlow(GenerateRequiredFlowSessions(signers))
        val otherParticipantsSessions = subFlow(GenerateRequiredFlowSessions(otherParticipants))
        subFlow(SendSignerFlags(signersSessions, otherParticipantsSessions))

        if(signersSessions.isNotEmpty()) {
            signedTransaction = subFlow(CollectSignaturesFlow(signedTransaction, signersSessions, localSignersKeys))
        }

        subFlow(
            FinalityFlow(
                transaction = signedTransaction,
                sessions = signersSessions + otherParticipantsSessions
            )
        )
    }

}

@InitiatedBy(DeleteReIssuedStatesAndLock::class)
class DeleteReIssuedStatesAndLockResponder(
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
