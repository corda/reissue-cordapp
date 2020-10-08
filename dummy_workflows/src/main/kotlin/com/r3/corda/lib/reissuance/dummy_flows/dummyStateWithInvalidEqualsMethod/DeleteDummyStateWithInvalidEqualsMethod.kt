package com.r3.corda.lib.reissuance.dummy_flows.dummyStateWithInvalidEqualsMethod

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.reissuance.dummy_contracts.DummyStateWithInvalidEqualsMethodContract
import com.r3.corda.lib.reissuance.dummy_states.DummyStateWithInvalidEqualsMethod
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class DeleteDummyStateWithInvalidEqualsMethod(
    private val DummyStateRequiringAllParticipantsSignaturesStateAndRef: StateAndRef<DummyStateWithInvalidEqualsMethod>
): FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val owner = ourIdentity
        val issuer = DummyStateRequiringAllParticipantsSignaturesStateAndRef.state.data.issuer
        val signers = setOf(owner.owningKey, issuer.owningKey).toList()

        val transactionBuilder = TransactionBuilder(notary = getPreferredNotary(serviceHub))
        transactionBuilder.addInputState(DummyStateRequiringAllParticipantsSignaturesStateAndRef)
        transactionBuilder.addCommand(DummyStateWithInvalidEqualsMethodContract.Commands.Delete(), signers)

        transactionBuilder.verify(serviceHub)
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)

        val sessions = listOf(initiateFlow(issuer))
        val fullySignedTransaction = subFlow(CollectSignaturesFlow(signedTransaction, sessions))

        subFlow(
            FinalityFlow(
                transaction = fullySignedTransaction,
                sessions = sessions
            )
        )
    }
}


@InitiatedBy(DeleteDummyStateWithInvalidEqualsMethod::class)
class DeleteDummyStateRequiringAllParticipantsSignaturesResponder(
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
