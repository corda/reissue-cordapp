package com.r3.corda.lib.reissuance.flows.example.dummyStateRequiringAllParticipantsSignatures

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.reissuance.dummy_contracts.DummyStateRequiringAllParticipantsSignaturesContract
import com.r3.corda.lib.reissuance.dummy_states.DummyStateRequiringAllParticipantsSignatures
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class UpdateDummyStateRequiringAllParticipantsSignatures(
    private val dummyStateRequiringAllParticipantsSignaturesStateAndRef: StateAndRef<DummyStateRequiringAllParticipantsSignatures>,
    private val newOwner: Party
): FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val owner = ourIdentity
        val issuer = dummyStateRequiringAllParticipantsSignaturesStateAndRef.state.data.issuer
        val other = dummyStateRequiringAllParticipantsSignaturesStateAndRef.state.data.other
        val signers = setOf(owner.owningKey, newOwner.owningKey, issuer.owningKey, other.owningKey).toList()

        var dummyStateRequiringAllParticipantsSignatures = dummyStateRequiringAllParticipantsSignaturesStateAndRef
            .state.data.copy(owner = newOwner)

        val transactionBuilder = TransactionBuilder(notary = getPreferredNotary(serviceHub))
        transactionBuilder.addInputState(dummyStateRequiringAllParticipantsSignaturesStateAndRef)
        transactionBuilder.addOutputState(dummyStateRequiringAllParticipantsSignatures)
        transactionBuilder.addCommand(DummyStateRequiringAllParticipantsSignaturesContract.Commands.Update(), signers)

        transactionBuilder.verify(serviceHub)
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)

        val sessions = listOf(newOwner, issuer, other).map{ initiateFlow(it) }
        val fullySignedTransaction = subFlow(CollectSignaturesFlow(signedTransaction, sessions))

        subFlow(
            FinalityFlow(
                transaction = fullySignedTransaction,
                sessions = sessions
            )
        )
    }
}


@InitiatedBy(UpdateDummyStateRequiringAllParticipantsSignatures::class)
class UpdateDummyStateRequiringAllParticipantsSignaturesResponder(
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
