package com.r3.corda.lib.reissuance.dummy_flows.simpleDummyState

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.reissuance.dummy_contracts.SimpleDummyStateContract
import com.r3.corda.lib.reissuance.dummy_states.SimpleDummyState
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.requireThat
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class UpdateSimpleDummyState(
    private val simpleDummyStateStateAndRef: StateAndRef<SimpleDummyState>,
    private val newOwner: Party
): FlowLogic<SecureHash>() {
    @Suspendable
    override fun call(): SecureHash {
        val owner = simpleDummyStateStateAndRef.state.data.owner
        require(owner == ourIdentity) { "Only current owner can trigger the flow" }

        val signers = setOf(owner.owningKey, newOwner.owningKey).toList() // old and new owner might be the same

        val transactionBuilder = TransactionBuilder(notary = getPreferredNotary(serviceHub))
        transactionBuilder.addInputState(simpleDummyStateStateAndRef)
        transactionBuilder.addOutputState(SimpleDummyState(newOwner))
        transactionBuilder.addCommand(SimpleDummyStateContract.Commands.Update(), signers)

        transactionBuilder.verify(serviceHub)
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)

        val signersSessions = if(owner != newOwner) listOf(initiateFlow(newOwner)) else listOf()

        val fullySignedTransaction = subFlow(CollectSignaturesFlow(signedTransaction, signersSessions))

        return subFlow(
            FinalityFlow(
                transaction = fullySignedTransaction,
                sessions = signersSessions
            )
        ).id
    }
}


@InitiatedBy(UpdateSimpleDummyState::class)
class SimpleDummyStateResponder(
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
