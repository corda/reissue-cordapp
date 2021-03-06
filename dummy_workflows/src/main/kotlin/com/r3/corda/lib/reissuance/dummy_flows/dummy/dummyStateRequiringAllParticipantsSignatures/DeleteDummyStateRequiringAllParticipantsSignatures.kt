package com.r3.corda.lib.reissuance.dummy_flows.dummy.dummyStateRequiringAllParticipantsSignatures

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.reissuance.dummy_contracts.DummyStateRequiringAllParticipantsSignaturesContract
import com.r3.corda.lib.reissuance.dummy_states.DummyStateRequiringAllParticipantsSignatures
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.requireThat
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class DeleteDummyStateRequiringAllParticipantsSignatures(
    private val DummyStateRequiringAllParticipantsSignaturesStateAndRef: StateAndRef<DummyStateRequiringAllParticipantsSignatures>
): FlowLogic<SecureHash>() {
    @Suspendable
    override fun call(): SecureHash {
        val owner = ourIdentity
        val issuer = DummyStateRequiringAllParticipantsSignaturesStateAndRef.state.data.issuer
        val other = DummyStateRequiringAllParticipantsSignaturesStateAndRef.state.data.other
        val signers = setOf(owner.owningKey, issuer.owningKey, other.owningKey).toList()

        val transactionBuilder = TransactionBuilder(notary = getPreferredNotary(serviceHub))
        transactionBuilder.addInputState(DummyStateRequiringAllParticipantsSignaturesStateAndRef)
        transactionBuilder.addCommand(DummyStateRequiringAllParticipantsSignaturesContract.Commands.Delete(), signers)

        transactionBuilder.verify(serviceHub)
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)

        val sessions = listOf(issuer, other).map{ initiateFlow(it) }
        val fullySignedTransaction = subFlow(CollectSignaturesFlow(signedTransaction, sessions))

        return subFlow(
            FinalityFlow(
                transaction = fullySignedTransaction,
                sessions = sessions
            )
        ).id
    }
}


@InitiatedBy(DeleteDummyStateRequiringAllParticipantsSignatures::class)
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
