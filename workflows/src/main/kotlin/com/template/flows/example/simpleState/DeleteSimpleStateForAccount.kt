package com.template.flows.example.simpleState

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import com.template.contracts.example.SimpleStateContract
import com.template.states.example.SimpleState
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class DeleteSimpleStateForAccount(
    private val originalStateAndRef: StateAndRef<SimpleState>
): FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val owner = originalStateAndRef.state.data.owner
        val ownerHost = serviceHub.identityService.partyFromKey(owner.owningKey)!!
        require(ownerHost == ourIdentity) { "Owner is not a valid account for the host" }

        val signers = listOf(owner.owningKey)
        val notary = getPreferredNotary(serviceHub)

        val transactionBuilder = TransactionBuilder(notary = notary)

        transactionBuilder.addInputState(originalStateAndRef)
        transactionBuilder.addCommand(SimpleStateContract.Commands.Delete(), signers)

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

@InitiatedBy(DeleteSimpleStateForAccount::class)
class DeleteSimpleStateForAccountResponder(
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

