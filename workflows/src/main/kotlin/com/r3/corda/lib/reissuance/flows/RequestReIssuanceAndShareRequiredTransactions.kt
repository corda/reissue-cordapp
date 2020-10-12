package com.r3.corda.lib.reissuance.flows

import co.paralleluniverse.fibers.Suspendable
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
class RequestReIssuanceAndShareRequiredTransactions<T>(
    private val issuer: AbstractParty,
    private val stateRefsToReIssue: List<StateRef>,
    private val assetIssuanceCommand: CommandData,
    private val extraAssetIssuanceSigners: List<AbstractParty> = listOf(issuer),
    private val requester: AbstractParty? = null // requester needs to be provided when using accounts
) : FlowLogic<SecureHash>() where T: ContractState {

    @Suspendable
    override fun call(): SecureHash {
        val requestReIssuanceTransactionId = subFlow(
            RequestReIssuance<T>(issuer, stateRefsToReIssue, assetIssuanceCommand, extraAssetIssuanceSigners, requester)
        )

        val requesterIdentity = requester ?: ourIdentity

        @Suppress("UNCHECKED_CAST")
        val statesToReIssue: List<StateAndRef<T>> = serviceHub.vaultService.queryBy<ContractState>(
            criteria= QueryCriteria.VaultQueryCriteria(stateRefs = stateRefsToReIssue)
        ).states as List<StateAndRef<T>>

        // all states need to have the same participants
        val participants = statesToReIssue[0].state.data.participants

        val requesterHost = serviceHub.identityService.partyFromKey(requesterIdentity.owningKey)!!
        val issuerHost = serviceHub.identityService.partyFromKey(issuer.owningKey)!!

        // if issuer is a participant, they already have access to those transactions
        if(!participants.contains(issuer) && requesterHost != issuerHost) {
            val transactionHashes = stateRefsToReIssue.map { it.txhash }
            val transactionsToSend = transactionHashes.map {
                serviceHub.validatedTransactions.getTransaction(it)
                    ?: throw FlowException("Can't find transaction with hash $it")
            }

            transactionsToSend.forEach { signedTransaction ->
                val sendToSession = initiateFlow(issuerHost)
                subFlow(SendTransactionFlow(sendToSession, signedTransaction))
            }
        }
        return requestReIssuanceTransactionId
    }

}

@InitiatedBy(RequestReIssuanceAndShareRequiredTransactions::class)
class ReceiveSignedTransaction(val otherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(ReceiveTransactionFlow(
            otherSideSession = otherSession,
            statesToRecord = StatesToRecord.ALL_VISIBLE
        ))
    }
}
