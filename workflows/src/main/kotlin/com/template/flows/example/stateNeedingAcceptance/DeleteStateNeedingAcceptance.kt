package com.template.flows.example.stateNeedingAcceptance

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import com.template.contracts.example.StateNeedingAcceptanceContract
import com.template.states.example.StateNeedingAcceptance
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap

@InitiatingFlow
@StartableByRPC
class DeleteStateNeedingAcceptance(
    private val stateNeedingAcceptanceStateAndRef: StateAndRef<StateNeedingAcceptance>
): FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val owner = ourIdentity
        val issuer = stateNeedingAcceptanceStateAndRef.state.data.issuer
        val acceptor = stateNeedingAcceptanceStateAndRef.state.data.acceptor

        val signers = setOf(owner.owningKey, acceptor.owningKey).toList()

        val transactionBuilder = TransactionBuilder(notary = getPreferredNotary(serviceHub))
        transactionBuilder.addInputState(stateNeedingAcceptanceStateAndRef)
        transactionBuilder.addCommand(StateNeedingAcceptanceContract.Commands.Delete(), signers)

        transactionBuilder.verify(serviceHub)
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)

        val signersSessions = listOf(initiateFlow(acceptor))
        signersSessions.forEach {
            it.send(true)
        }

        val otherParticipantsSession = listOf(initiateFlow(issuer))
        otherParticipantsSession.forEach {
            it.send(false)
        }

        val fullySignedTransaction = subFlow(CollectSignaturesFlow(signedTransaction, signersSessions))

        subFlow(
            FinalityFlow(
                transaction = fullySignedTransaction,
                sessions = signersSessions + otherParticipantsSession
            )
        )
    }
}


@InitiatedBy(DeleteStateNeedingAcceptance::class)
class DeleteStateNeedingAcceptanceResponder(
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
