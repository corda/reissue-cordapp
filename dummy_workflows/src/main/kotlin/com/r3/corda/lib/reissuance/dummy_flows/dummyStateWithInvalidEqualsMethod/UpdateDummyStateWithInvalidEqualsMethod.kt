package com.r3.corda.lib.reissuance.dummy_flows.dummyStateWithInvalidEqualsMethod

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.reissuance.dummy_contracts.DummyStateWithInvalidEqualsMethodContract
import com.r3.corda.lib.reissuance.dummy_states.DummyStateWithInvalidEqualsMethod
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
class UpdateDummyStateWithInvalidEqualsMethod(
    private val dummyStateWithInvalidEqualsMethodStateAndRef: StateAndRef<DummyStateWithInvalidEqualsMethod>,
    private val newOwner: Party
): FlowLogic<SecureHash>() {
    @Suspendable
    override fun call(): SecureHash {
        val owner = ourIdentity
        val issuer = dummyStateWithInvalidEqualsMethodStateAndRef.state.data.issuer
        val signers = setOf(owner.owningKey, newOwner.owningKey, issuer.owningKey).toList()

        var dummyStateRequiringAllParticipantsSignatures = dummyStateWithInvalidEqualsMethodStateAndRef.state.data
            .copy(owner = newOwner)

        val transactionBuilder = TransactionBuilder(notary = getPreferredNotary(serviceHub))
        transactionBuilder.addInputState(dummyStateWithInvalidEqualsMethodStateAndRef)
        transactionBuilder.addOutputState(dummyStateRequiringAllParticipantsSignatures)
        transactionBuilder.addCommand(DummyStateWithInvalidEqualsMethodContract.Commands.Update(), signers)

        transactionBuilder.verify(serviceHub)
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)

        val sessions = listOf(newOwner, issuer).map{ initiateFlow(it) }
        val fullySignedTransaction = subFlow(CollectSignaturesFlow(signedTransaction, sessions))

        return subFlow(
            FinalityFlow(
                transaction = fullySignedTransaction,
                sessions = sessions
            )
        ).id
    }
}


@InitiatedBy(UpdateDummyStateWithInvalidEqualsMethod::class)
class UpdateDummyStateWithInvalidEqualsMethodResponder(
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
