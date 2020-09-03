package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import com.template.contracts.ReIssuanceRequestContract
import com.template.states.ReIssuanceRequest
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class CreateReIssuanceRequest<T>(
    private val issuer: AbstractParty,
    private val statesToReIssue: List<StateAndRef<T>>,
    private val issuanceCommand: CommandData,
    private val issuanceSigners: List<AbstractParty> = listOf(issuer),
    private val requester: AbstractParty? = null // requester needs to be provided when using accounts
) : FlowLogic<Unit>() where T: ContractState {

    @Suspendable
    override fun call() {
        if(requester != null) {
            val requesterHost = serviceHub.identityService.partyFromKey(requester.owningKey)!!
            require(requesterHost == ourIdentity) { "Requester is not a valid account for the host" }
        }
        val requesterAbstractParty: AbstractParty = requester ?: ourIdentity

        val issuerHost: Party = serviceHub.identityService.partyFromKey(issuer.owningKey)!!

        val signers = listOf(requesterAbstractParty.owningKey)

        val reIssuanceRequest = ReIssuanceRequest(issuer, requesterAbstractParty, statesToReIssue, issuanceCommand, issuanceSigners)

        val transactionBuilder = TransactionBuilder(notary = getPreferredNotary(serviceHub))
        transactionBuilder.addOutputState(reIssuanceRequest)
        transactionBuilder.addCommand(ReIssuanceRequestContract.Commands.Create(), signers)

        transactionBuilder.verify(serviceHub)
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder, signers)

        val sessions = listOfNotNull(
            if(ourIdentity != issuerHost) initiateFlow(issuerHost) else null
        )
        subFlow(
            FinalityFlow(
                transaction = signedTransaction,
                sessions = sessions
            )
        )
    }
}

@InitiatedBy(CreateReIssuanceRequest::class)
class CreateReIssuanceRequestResponder(
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
