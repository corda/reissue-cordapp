package com.r3.corda.lib.reissuance.dummy_flows.dummy.simpleDummyState

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.reissuance.dummy_contracts.SimpleDummyStateContract
import com.r3.corda.lib.reissuance.dummy_states.SimpleDummyState
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class CreateSimpleDummyState(
    private val owner: Party,
    private val notary : Party? = null
): FlowLogic<SecureHash>() {
    @Suspendable
    override fun call(): SecureHash {
        val issuer = ourIdentity
        val signers = listOf(issuer.owningKey)

        val notaryToUse = notary ?: getPreferredNotary(serviceHub)
        val transactionBuilder = TransactionBuilder(notary = notaryToUse)
        transactionBuilder.addOutputState(SimpleDummyState(owner))
        transactionBuilder.addCommand(SimpleDummyStateContract.Commands.Create(), signers)

        transactionBuilder.verify(serviceHub)
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)

        val ownerSession = initiateFlow(owner)
        return subFlow(
            FinalityFlow(
                transaction = signedTransaction,
                sessions = listOf(ownerSession)
            )
        ).id
    }
}


@InitiatedBy(CreateSimpleDummyState::class)
class CreateSimpleDummyStateResponder(
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
