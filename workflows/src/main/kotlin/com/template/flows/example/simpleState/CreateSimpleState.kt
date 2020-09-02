package com.template.flows.example.simpleState

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import com.template.contracts.example.SimpleStateContract
import com.template.states.example.SimpleState
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class CreateSimpleState(
    private val owner: Party
): FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val issuer = ourIdentity
        val signers = listOf(issuer.owningKey)

        val transactionBuilder = TransactionBuilder(notary = getPreferredNotary(serviceHub))
        transactionBuilder.addOutputState(SimpleState(owner))
        transactionBuilder.addCommand(SimpleStateContract.Commands.Create(), signers)

        transactionBuilder.verify(serviceHub)
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)

        val ownerSession = initiateFlow(owner)
        subFlow(
            FinalityFlow(
                transaction = signedTransaction,
                sessions = listOf(ownerSession)
            )
        )
    }
}


@InitiatedBy(CreateSimpleState::class)
class CreateSimpleStateResponder(
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
