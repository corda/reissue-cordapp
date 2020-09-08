package com.template.flows.example.simpleState

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import com.template.contracts.example.SimpleStateContract
import com.template.states.example.SimpleState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class UpdateSimpleState(
    private val simpleStateStateAndRef: StateAndRef<SimpleState>,
    private val newOwner: Party
): FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val owner = simpleStateStateAndRef.state.data.owner
        require(owner == ourIdentity) { "Only current owner can trigger the flow" }

        val signers = setOf(owner.owningKey, newOwner.owningKey).toList() // old and new owner might be the same

        val transactionBuilder = TransactionBuilder(notary = getPreferredNotary(serviceHub))
        transactionBuilder.addInputState(simpleStateStateAndRef)
        transactionBuilder.addOutputState(SimpleState(newOwner))
        transactionBuilder.addCommand(SimpleStateContract.Commands.Update(), signers)

        transactionBuilder.verify(serviceHub)
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)

        val signersSessions = if(owner != newOwner) listOf(initiateFlow(newOwner)) else listOf()

        val fullySignedTransaction = subFlow(CollectSignaturesFlow(signedTransaction, signersSessions))

        subFlow(
            FinalityFlow(
                transaction = fullySignedTransaction,
                sessions = signersSessions
            )
        )
    }
}


@InitiatedBy(UpdateSimpleState::class)
class UpdateSimpleStateResponder(
    private val otherSession: FlowSession
) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val signTransactionFlow = object : SignTransactionFlow(otherSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
            }
        }
        val transaction = subFlow(signTransactionFlow)
        subFlow(
            ReceiveFinalityFlow(
                otherSession,
                expectedTxId = transaction.id,
                statesToRecord = StatesToRecord.ALL_VISIBLE
            )
        )
    }
}
