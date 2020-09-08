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
        val owner = simpleStateStateAndRef.state.data.owner

        val ownerHost = serviceHub.identityService.partyFromKey(owner.owningKey)!!
        require(ownerHost == ourIdentity) { "Owner is not a valid account for the host" }

        val newOwnerHost = serviceHub.identityService.partyFromKey(newOwner.owningKey)!!

        val signers = setOf(owner.owningKey, newOwner.owningKey).toList() // old and new owner might be the same
        val localSigners = listOfNotNull(
            simpleStateStateAndRef.state.data.owner.owningKey,
            if(newOwnerHost == ourIdentity && owner != newOwner) newOwner.owningKey else null
        )

        val transactionBuilder = TransactionBuilder(notary = getPreferredNotary(serviceHub))
        transactionBuilder.addInputState(simpleStateStateAndRef)
        transactionBuilder.addOutputState(SimpleState(newOwner))
        transactionBuilder.addCommand(SimpleStateContract.Commands.Update(), signers)

        transactionBuilder.verify(serviceHub)
        var signedTransaction = serviceHub.signInitialTransaction(transactionBuilder, localSigners)

        val signersSessions = listOfNotNull(
            if(newOwnerHost != ourIdentity) initiateFlow(newOwnerHost) else null
        )

        signedTransaction = subFlow(CollectSignaturesFlow(signedTransaction, signersSessions, localSigners))

        subFlow(
            FinalityFlow(
                transaction = signedTransaction,
                sessions = signersSessions
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
