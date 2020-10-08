package com.r3.corda.lib.reissuance.flows.example.dummyStateRequiringAllParticipantsSignatures

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.reissuance.dummy_contracts.DummyStateRequiringAllParticipantsSignaturesContract
import com.r3.corda.lib.reissuance.dummy_states.DummyStateRequiringAllParticipantsSignatures
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class CreateDummyStateRequiringAllParticipantsSignatures(
    private val owner: Party,
    private val other: Party
): FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val issuer = ourIdentity
        val signers = listOf(owner.owningKey, issuer.owningKey, other.owningKey)

        val transactionBuilder = TransactionBuilder(notary = getPreferredNotary(serviceHub))
        transactionBuilder.addOutputState(DummyStateRequiringAllParticipantsSignatures(owner, issuer, other))
        transactionBuilder.addCommand(DummyStateRequiringAllParticipantsSignaturesContract.Commands.Create(), signers)

        transactionBuilder.verify(serviceHub)
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)

        val sessions = listOf(owner, other).map{ initiateFlow(it) }
        val fullySignedTransaction = subFlow(CollectSignaturesFlow(signedTransaction, sessions))

        subFlow(
            FinalityFlow(
                transaction = fullySignedTransaction,
                sessions = sessions
            )
        )
    }
}


@InitiatedBy(CreateDummyStateRequiringAllParticipantsSignatures::class)
class CreateDummyStateRequiringAllParticipantsSignaturesResponder(
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
