package com.template.flows.example.simpleState

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import com.template.contracts.example.SimpleStateContract
import com.template.states.example.SimpleState
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class UpdateSimpleStateForAccount(
    private val simpleStateStateAndRef: StateAndRef<SimpleState>,
    private val newOwner: AbstractParty
): FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val employeeHost = ourIdentity
        val signers = listOf(employeeHost.owningKey, simpleStateStateAndRef.state.data.owner.owningKey, newOwner.owningKey)

        var updateSimpleState = SimpleState(newOwner)

        val transactionBuilder = TransactionBuilder(notary = getPreferredNotary(serviceHub))
        transactionBuilder.addInputState(simpleStateStateAndRef)
        transactionBuilder.addOutputState(updateSimpleState)
        transactionBuilder.addCommand(SimpleStateContract.Commands.Update(), signers)

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


@InitiatedBy(UpdateSimpleStateForAccount::class)
class UUpdateSimpleStateForAccountResponder(
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
