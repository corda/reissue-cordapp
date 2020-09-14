package com.template.flows.example.simpleDummyState

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import com.template.contracts.example.SimpleDummyStateContract
import com.template.states.example.SimpleDummyState
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class DeleteSimpleDummyStateForAccount(
    private val originalStateAndRef: StateAndRef<SimpleDummyState>
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
        transactionBuilder.addCommand(SimpleDummyStateContract.Commands.Delete(), signers)

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

@InitiatedBy(DeleteSimpleDummyStateForAccount::class)
class DeleteSimpleDummyStateForAccountResponder(
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

