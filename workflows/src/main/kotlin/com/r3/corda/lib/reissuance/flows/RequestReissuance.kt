package com.r3.corda.lib.reissuance.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.reissuance.contracts.ReissuanceRequestContract
import com.r3.corda.lib.reissuance.states.ReissuanceRequest
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class RequestReissuance<T>(
    private val issuer: AbstractParty,
    private val stateRefsToReissue: List<StateRef>,
    private val assetIssuanceCommand: CommandData,
    private val extraAssetIssuanceSigners: List<AbstractParty> = listOf(), // issuer is always a signer
    private val requester: AbstractParty? = null // requester needs to be provided when using accounts
) : FlowLogic<SecureHash>() where T: ContractState {

    @Suspendable
    override fun call(): SecureHash {
        if(requester != null) {
            val requesterHost = serviceHub.identityService.partyFromKey(requester.owningKey)!!
            require(requesterHost == ourIdentity) { "Requester is not a valid account for the host" }
        }
        val requesterAbstractParty: AbstractParty = requester ?: ourIdentity

        require(!extraAssetIssuanceSigners.contains(issuer)) {
            "Issuer is always a signer and shouldn't be passed in as a part of extraAssetIssuanceSigners" }
        val issuanceSigners = listOf(issuer) + extraAssetIssuanceSigners

        val signers = listOf(requesterAbstractParty.owningKey)

        val reissuanceRequest = ReissuanceRequest(issuer, requesterAbstractParty, stateRefsToReissue,
            assetIssuanceCommand, issuanceSigners)

        val transactionBuilder = TransactionBuilder(notary = getPreferredNotary(serviceHub))
        transactionBuilder.addOutputState(reissuanceRequest)
        transactionBuilder.addCommand(ReissuanceRequestContract.Commands.Create(), signers)

        transactionBuilder.verify(serviceHub)
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder, signers)

        val issuerHost: Party = serviceHub.identityService.partyFromKey(issuer.owningKey)!!
        val sessions = listOfNotNull(
            // don't create a session if issuer and requester are accounts on the same node
            if(ourIdentity != issuerHost) initiateFlow(issuerHost) else null
        )

        return subFlow(
            FinalityFlow(
                transaction = signedTransaction,
                sessions = sessions
            )
        ).id
    }
}

@InitiatedBy(RequestReissuance::class)
class RequestReissuanceResponder(
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
