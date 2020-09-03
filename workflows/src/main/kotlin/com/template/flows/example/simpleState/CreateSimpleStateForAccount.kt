package com.template.flows.example.simpleState

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import com.template.contracts.example.SimpleStateContract
import com.template.states.example.SimpleState
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class CreateSimpleStateForAccount(
    private val issuer: AbstractParty,
    private val owner: AbstractParty
): FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val host = serviceHub.identityService.partyFromKey(issuer.owningKey)!!

        val signers = listOf(host.owningKey, issuer.owningKey)

        val transactionBuilder = TransactionBuilder(notary = getPreferredNotary(serviceHub))
        transactionBuilder.addOutputState(SimpleState(owner))
        transactionBuilder.addCommand(SimpleStateContract.Commands.Create(), signers)

        transactionBuilder.verify(serviceHub)
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder, signers)

        subFlow(
            FinalityFlow(
                transaction = signedTransaction,
                sessions = listOf()
            )
        )
    }
}


@InitiatedBy(CreateSimpleStateForAccount::class)
class CreateSimpleStateForAccountResponder(
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
