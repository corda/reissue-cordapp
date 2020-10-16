package com.r3.corda.lib.reissuance.dummy_flows.dummy.simpleDummyState

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.reissuance.dummy_contracts.SimpleDummyStateContract
import com.r3.corda.lib.reissuance.dummy_states.SimpleDummyState
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class CreateSimpleDummyStateForAccount(
    private val issuer: AbstractParty,
    private val owner: AbstractParty
): FlowLogic<SecureHash>() {
    @Suspendable
    override fun call(): SecureHash {
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

        return subFlow(
            FinalityFlow(
                transaction = signedTransaction,
                sessions = sessions
            )
        ).id
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
