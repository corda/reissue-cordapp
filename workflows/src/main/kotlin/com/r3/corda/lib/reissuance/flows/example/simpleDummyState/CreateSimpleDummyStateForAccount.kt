package com.r3.corda.lib.reissuance.flows.example.simpleDummyState

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.reissuance.contracts.example.SimpleDummyStateContract
import com.r3.corda.lib.reissuance.states.example.SimpleDummyState
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class CreateSimpleDummyStateForAccount(
    private val issuer: AbstractParty,
    private val owner: AbstractParty
): FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val issuerHost = serviceHub.identityService.partyFromKey(issuer.owningKey)!!
        require(issuerHost == ourIdentity) { "Issuer is not a valid account for the host" }

        val localSigners = listOf(issuer.owningKey)

        val transactionBuilder = TransactionBuilder(notary = getPreferredNotary(serviceHub))
        transactionBuilder.addOutputState(SimpleDummyState(owner))
        transactionBuilder.addCommand(SimpleDummyStateContract.Commands.Create(), localSigners)

        transactionBuilder.verify(serviceHub)
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder, localSigners)

        val ownerHost = serviceHub.identityService.partyFromKey(owner.owningKey)!!
        val sessions = if(ownerHost != ourIdentity) listOf( initiateFlow(ownerHost) ) else listOf()

        subFlow(
            FinalityFlow(
                transaction = signedTransaction,
                sessions = sessions
            )
        )
    }
}


@InitiatedBy(CreateSimpleDummyStateForAccount::class)
class CreateSimpleDummyStateForAccountResponder(
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