package com.r3.corda.lib.reissuance.dummy_flows.simpleDummyState

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.reissuance.dummy_contracts.DummyStateWithInvalidEqualsMethodContract
import com.r3.corda.lib.reissuance.dummy_contracts.SimpleDummyStateContract
import com.r3.corda.lib.reissuance.dummy_states.DummyStateWithInvalidEqualsMethod
import com.r3.corda.lib.reissuance.dummy_states.SimpleDummyState
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import net.corda.core.contracts.StateAndRef
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class DeleteSimpleDummyStateAndCreateDummyStateWithInvalidEqualsMethod(
    private val originalStateAndRef: StateAndRef<SimpleDummyState>,
    private val issuer: Party
): FlowLogic<SecureHash>() {
    @Suspendable
    override fun call(): SecureHash {
        val signers = listOf(ourIdentity.owningKey)
        val notary = getPreferredNotary(serviceHub)

        val transactionBuilder = TransactionBuilder(notary = notary)

        transactionBuilder.addInputState(originalStateAndRef)
        transactionBuilder.addCommand(SimpleDummyStateContract.Commands.Delete(), signers)

        transactionBuilder.addOutputState(DummyStateWithInvalidEqualsMethod(ourIdentity, issuer, 1))
        transactionBuilder.addCommand(DummyStateWithInvalidEqualsMethodContract.Commands.Create(), signers)

        transactionBuilder.verify(serviceHub)
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)

        val issuerSession = initiateFlow(issuer)
        return subFlow(
            FinalityFlow(
                transaction = signedTransaction,
                sessions = listOf(issuerSession)
            )
        ).id
    }

}

@InitiatedBy(DeleteSimpleDummyStateAndCreateDummyStateWithInvalidEqualsMethod::class)
class DeleteSimpleDummyStateAndCreateDummyStateWithInvalidEqualsMethodResponder(
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

