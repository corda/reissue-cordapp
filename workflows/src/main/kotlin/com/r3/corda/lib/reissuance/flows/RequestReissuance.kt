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
    private val requester: AbstractParty? = null, // requester needs to be provided when using accounts
    private val notary : Party? = null
) : FlowLogic<SecureHash>() where T: ContractState {

    @Suspendable
    override fun call(): SecureHash {

        val issuerHost: Party = serviceHub.identityService.partyFromKey(issuer.owningKey)!!
        val sessions = listOfNotNull(
            // don't create a session if issuer and requester are accounts on the same node
            if(ourIdentity != issuerHost) initiateFlow(issuerHost) else null
        )

        return subFlow(
            RequestReissuanceNonInitiating<T>(
                sessions,
                issuer,
                stateRefsToReissue,
                assetIssuanceCommand,
                extraAssetIssuanceSigners,
                requester,
                notary
            )
        )
    }
}

@InitiatedBy(RequestReissuance::class)
class RequestReissuanceResponder(
    private val otherSession: FlowSession
) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(
            RequestReissuanceNonInitiatingResponder(otherSession)
        )
    }
}
