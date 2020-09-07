package com.template.flows.example.simpleState

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import com.template.contracts.example.SimpleStateContract
import com.template.states.example.SimpleState
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class UpdateSimpleStateForAccount(
    private val simpleStateStateAndRef: StateAndRef<SimpleState>,
    private val newOwner: AbstractParty
): FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val ownerHost = serviceHub.identityService.partyFromKey(simpleStateStateAndRef.state.data.owner.owningKey)!!
        require(ownerHost == ourIdentity) { "Owner is not a valid account for the host" }

        val newOwnerHost = serviceHub.identityService.partyFromKey(newOwner.owningKey)!!

        val localSigners = listOfNotNull(
            simpleStateStateAndRef.state.data.owner.owningKey,
            if(newOwnerHost == ourIdentity) newOwner.owningKey else null
        )
        val otherSigners = listOfNotNull(
            if(newOwnerHost != ourIdentity) newOwner.owningKey else null
        )

        val transactionBuilder = TransactionBuilder(notary = getPreferredNotary(serviceHub))
        transactionBuilder.addInputState(simpleStateStateAndRef)
        transactionBuilder.addOutputState(SimpleState(newOwner))
        transactionBuilder.addCommand(SimpleStateContract.Commands.Update(), localSigners)

        transactionBuilder.verify(serviceHub)
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder, localSigners)

        val otherSignersSessions = listOfNotNull(
            if(newOwnerHost != ourIdentity) initiateFlow(newOwnerHost) else null
        )

        subFlow(
            FinalityFlow(
                transaction = signedTransaction,
                sessions = otherSignersSessions
            )
        )
    }
}


@InitiatedBy(UpdateSimpleStateForAccount::class)
class UUpdateSimpleStateForAccountResponder(
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
