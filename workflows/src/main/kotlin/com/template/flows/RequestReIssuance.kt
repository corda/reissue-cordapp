package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import com.template.contracts.ReIssuanceRequestContract
import com.template.states.ReIssuanceRequest
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateRef
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class RequestReIssuance<T>(
    private val issuer: AbstractParty,
    private val stateRefsToReIssue: List<StateRef>,
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

        val signers = listOf(requesterAbstractParty.owningKey).distinct()

        val reIssuanceRequest = ReIssuanceRequest(issuer, requesterAbstractParty, stateRefsToReIssue, issuanceCommand, issuanceSigners)

        val transactionBuilder = TransactionBuilder(notary = getPreferredNotary(serviceHub))
        transactionBuilder.addOutputState(reIssuanceRequest)
        transactionBuilder.addCommand(ReIssuanceRequestContract.Commands.Create(), signers)

        transactionBuilder.verify(serviceHub)
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder, signers)

        val issuerHost: Party = serviceHub.identityService.partyFromKey(issuer.owningKey)!!
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

@InitiatedBy(RequestReIssuance::class)
class RequestReIssuanceResponder(
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
