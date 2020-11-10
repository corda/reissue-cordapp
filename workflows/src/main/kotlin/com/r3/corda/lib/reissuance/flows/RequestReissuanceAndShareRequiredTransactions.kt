package com.r3.corda.lib.reissuance.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.internal.accountService
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.node.StatesToRecord
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria

@InitiatingFlow
@StartableByRPC
class RequestReissuanceAndShareRequiredTransactions<T>(
    private val issuer: AbstractParty,
    private val stateRefsToReissue: List<StateRef>,
    private val assetIssuanceCommand: CommandData,
    private val extraAssetIssuanceSigners: List<AbstractParty> = listOf(), // issuer is always a signer
    private val requester: AbstractParty? = null // requester needs to be provided when using accounts
) : FlowLogic<SecureHash>() where T: ContractState {

    @Suspendable
    override fun call(): SecureHash {
        val requestReissuanceTransactionId = subFlow(
            RequestReissuance<T>(issuer, stateRefsToReissue, assetIssuanceCommand, extraAssetIssuanceSigners, requester)
        )
        val requesterIdentity = requester ?: ourIdentity

        val refCriteria = QueryCriteria.VaultQueryCriteria(stateRefs = stateRefsToReissue)
        val criteria = if(requester == null) refCriteria else {
            val accountUuid = serviceHub.accountService.accountIdForKey(requester.owningKey)
            require(accountUuid != null) { "UUID for $requester is not found" }
            val accountCriteria = QueryCriteria.VaultQueryCriteria().withExternalIds(listOf(accountUuid!!))
            refCriteria.and(accountCriteria)
        }
        @Suppress("UNCHECKED_CAST")
        val statesToReissue: List<StateAndRef<T>> = serviceHub.vaultService.queryBy<ContractState>(criteria).states
            as List<StateAndRef<T>>

        // all states need to have the same participants
        val participants = statesToReissue[0].state.data.participants

        val requesterHost = serviceHub.identityService.partyFromKey(requesterIdentity.owningKey)!!
        val issuerHost = serviceHub.identityService.partyFromKey(issuer.owningKey)!!

        // if issuer is a participant, they already have access to those transactions
        if(!participants.contains(issuer) && requesterHost != issuerHost) {
            val transactionHashes = stateRefsToReissue.map { it.txhash }
            val transactionsToSend = transactionHashes.map {
                serviceHub.validatedTransactions.getTransaction(it)
                    ?: throw FlowException("Can't find transaction with hash $it")
            }

            transactionsToSend.forEach { signedTransaction ->
                val sendToSession = initiateFlow(issuerHost)
                subFlow(SendTransactionFlow(sendToSession, signedTransaction))
            }
        }
        return requestReissuanceTransactionId
    }

}

@InitiatedBy(RequestReissuanceAndShareRequiredTransactions::class)
class ReceiveSignedTransaction(val otherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(ReceiveTransactionFlow(
            otherSideSession = otherSession,
            statesToRecord = StatesToRecord.ALL_VISIBLE
        ))
    }
}
