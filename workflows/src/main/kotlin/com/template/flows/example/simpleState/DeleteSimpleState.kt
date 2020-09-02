package com.template.flows.example.simpleState

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import com.template.contracts.example.SimpleStateContract
import com.template.states.example.SimpleState
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class DeleteSimpleState(
    private val originalStateAndRef: StateAndRef<SimpleState>,
    private val issuer: Party
): FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val signers = listOf(ourIdentity.owningKey)
        val notary = getPreferredNotary(serviceHub)

        val transactionBuilder = TransactionBuilder(notary = notary)

        transactionBuilder.addInputState(originalStateAndRef)
        transactionBuilder.addCommand(SimpleStateContract.Commands.Delete(), signers)

        transactionBuilder.verify(serviceHub)
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)

        val issuerSession = initiateFlow(issuer)
        subFlow(
            FinalityFlow(
                transaction = signedTransaction,
                sessions = listOf(issuerSession)
            )
        )
    }

}

@InitiatedBy(DeleteSimpleState::class)
class DeleteSimpleStateResponder(
    private val otherSession: FlowSession
) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(
            ReceiveFinalityFlow(
                otherSession,
                statesToRecord = StatesToRecord.ALL_VISIBLE
            )
        )
    }
}

