package com.r3.corda.lib.reissuance.flows.example.simpleDummyState

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.reissuance.contracts.example.SimpleDummyStateContract
import com.r3.corda.lib.reissuance.states.example.SimpleDummyState
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class CreateSimpleDummyState(
    private val owner: Party
): FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val issuer = ourIdentity
        val signers = listOf(issuer.owningKey)

        val transactionBuilder = TransactionBuilder(notary = getPreferredNotary(serviceHub))
        transactionBuilder.addOutputState(SimpleDummyState(owner))
        transactionBuilder.addCommand(SimpleDummyStateContract.Commands.Create(), signers)

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


@InitiatedBy(CreateSimpleDummyState::class)
class CreateSimpleDummyStateResponder(
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
