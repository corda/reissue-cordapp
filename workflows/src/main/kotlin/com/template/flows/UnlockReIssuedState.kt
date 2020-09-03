package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import com.template.contracts.ReIssuanceLockContract
import com.template.states.ReIssuanceLock
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap

@InitiatingFlow
@StartableByRPC
class UnlockReIssuedState<T>(
    private val reIssuedStateAndRefs: List<StateAndRef<T>>,
    private val reIssuanceLock: StateAndRef<ReIssuanceLock<T>>,
    private val deletedStateTransactionHash: SecureHash,
    private val updateCommand: CommandData,
    private val updateSigners: List<AbstractParty> = listOf(reIssuanceLock.state.data.requester)
): FlowLogic<Unit>() where T: ContractState {
    @Suspendable
    override fun call() {
        val notary = getPreferredNotary(serviceHub)
        val lockSigners = listOf(ourIdentity.owningKey)
        val issuer = reIssuanceLock.state.data.issuer
        val reIssuedStatesSigners = updateSigners.map { it.owningKey }

        val transactionBuilder = TransactionBuilder(notary)

        reIssuedStateAndRefs.forEach { reIssuedStateAndRef ->
            transactionBuilder.addInputState(reIssuedStateAndRef)
            transactionBuilder.addOutputState(reIssuedStateAndRef.state.data)
        }
        transactionBuilder.addCommand(updateCommand, reIssuedStatesSigners)

        transactionBuilder.addInputState(reIssuanceLock)
        transactionBuilder.addCommand(ReIssuanceLockContract.Commands.Delete(), lockSigners)

        transactionBuilder.addAttachment(deletedStateTransactionHash)

        transactionBuilder.verify(serviceHub)
        var signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)

//        val signers = (updateSigners + reIssuanceLock.state.data.issuer).distinct()
//        val otherParticipants = reIssuanceLock.state.data.participants.filter { !signers.contains(it) }
//        val signersSessions = signers.filter { it != ourIdentity }.map { initiateFlow(it) }
//        val otherParticipantsSessions = otherParticipants.filter { it != ourIdentity }.map { initiateFlow(it as Party) }
//
//        signersSessions.forEach {
//            it.send(true)
//        }
//        otherParticipantsSessions.forEach {
//            it.send(false)
//        }
//
//        if(signersSessions.isNotEmpty()) {
//            signedTransaction = subFlow(CollectSignaturesFlow(signedTransaction, signersSessions))
//        }
//
//        subFlow(
//            FinalityFlow(
//                transaction = signedTransaction,
//                sessions = signersSessions + otherParticipantsSessions
//            )
//        )
    }

}

@InitiatedBy(UnlockReIssuedState::class)
class UnlockReIssuedStateResponder(
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
