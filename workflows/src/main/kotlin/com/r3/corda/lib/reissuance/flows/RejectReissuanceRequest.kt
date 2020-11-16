package com.r3.corda.lib.reissuance.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.reissuance.contracts.ReissuanceRequestContract
import com.r3.corda.lib.reissuance.states.ReissuanceRequest
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class RejectReissuanceRequest<T>(
    private val reissuanceRequestStateAndRef: StateAndRef<ReissuanceRequest>
): FlowLogic<SecureHash>() where T: ContractState {

    @Suspendable
    override fun call(): SecureHash {
        val reissuanceRequest = reissuanceRequestStateAndRef.state.data

        val issuer = reissuanceRequest.issuer
        val issuerHost = serviceHub.identityService.partyFromKey(issuer.owningKey)!!
        require(issuerHost == ourIdentity) { "Issuer is not a valid account for the host" }

        val requester = reissuanceRequest.requester
        val requesterHost = serviceHub.identityService.partyFromKey(requester.owningKey)!!

        val notary = getPreferredNotary(serviceHub)
        val signers = listOf(issuer.owningKey)

        val transactionBuilder = TransactionBuilder(notary = notary)
        transactionBuilder.addInputState(reissuanceRequestStateAndRef)
        transactionBuilder.addCommand(ReissuanceRequestContract.Commands.Reject(), signers)

        transactionBuilder.verify(serviceHub)
        var signedTransaction = serviceHub.signInitialTransaction(transactionBuilder, signers)

        // don't create a session if issuer and requester are accounts on the same node
        val sessions = if(requesterHost == ourIdentity) listOf() else listOf(initiateFlow(requesterHost))

        return subFlow(
            FinalityFlow(
                transaction = signedTransaction,
                sessions = sessions
            )
        ).id
    }

}

@InitiatedBy(RejectReissuanceRequest::class)
class RejectReissuanceRequestResponder(
    private val otherSession: FlowSession
) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        return subFlow(ReceiveFinalityFlow(otherSideSession = otherSession))
    }
}
