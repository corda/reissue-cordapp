package com.r3.corda.lib.reissuance.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.reissuance.contracts.ReIssuanceRequestContract
import com.r3.corda.lib.reissuance.states.ReIssuanceRequest
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class RejectReIssuanceRequest<T>(
    private val reIssuanceRequestStateAndRef: StateAndRef<ReIssuanceRequest>
): FlowLogic<Unit>() where T: ContractState {

    @Suspendable
    override fun call() {
        val reIssuanceRequest = reIssuanceRequestStateAndRef.state.data

        val issuer = reIssuanceRequest.issuer
        val issuerHost = serviceHub.identityService.partyFromKey(issuer.owningKey)!!
        require(issuerHost == ourIdentity) { "Issuer is not a valid account for the host" }

        val requester = reIssuanceRequest.requester
        val requesterHost = serviceHub.identityService.partyFromKey(requester.owningKey)!!

        val notary = getPreferredNotary(serviceHub)
        val signers = listOf(issuer.owningKey)

        val transactionBuilder = TransactionBuilder(notary = notary)
        transactionBuilder.addInputState(reIssuanceRequestStateAndRef)
        transactionBuilder.addCommand(ReIssuanceRequestContract.Commands.Reject(), signers)

        transactionBuilder.verify(serviceHub)
        var signedTransaction = serviceHub.signInitialTransaction(transactionBuilder, signers)

        val sessions = if(requesterHost == ourIdentity) listOf() else listOf(initiateFlow(requesterHost))

        subFlow(
            FinalityFlow(
                transaction = signedTransaction,
                sessions = sessions
            )
        )
    }

}

@InitiatedBy(RejectReIssuanceRequest::class)
class RejectReIssuanceRequestResponder(
    private val otherSession: FlowSession
) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        return subFlow(ReceiveFinalityFlow(otherSideSession = otherSession))
    }
}
