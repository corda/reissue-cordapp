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
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.unwrap

@InitiatingFlow
@StartableByRPC
class RequestReissuanceAndShareRequiredTransactions<T>(
    private val issuer: AbstractParty,
    private val stateRefsToReissue: List<StateRef>,
    private val assetDestroyCommand: CommandData,
    private val extraAssetDestroySigners: List<AbstractParty> = listOf(), // issuer is always a signer
    private val requester: AbstractParty? = null // requester needs to be provided when using accounts
) : FlowLogic<SecureHash>() where T: ContractState {

    @Suspendable
    override fun call(): SecureHash {

        require(stateRefsToReissue.isNotEmpty()) {
            "stateRefsToReissue can not be empty"
        }
        val refCriteria = QueryCriteria.VaultQueryCriteria(stateRefs = stateRefsToReissue)
        val criteria = if (requester == null) refCriteria else {
            val accountUuid = serviceHub.accountService.accountIdForKey(requester.owningKey)
            require(accountUuid != null) { "UUID for $requester is not found" }
            val accountCriteria = QueryCriteria.VaultQueryCriteria().withExternalIds(listOf(accountUuid!!))
            refCriteria.and(accountCriteria)
        }

        @Suppress("UNCHECKED_CAST")
        val statesToReissue: List<StateAndRef<T>> = serviceHub.vaultService.queryBy<ContractState>(criteria).states
                as List<StateAndRef<T>>

        // there can only be a single notary in a reissuance request
        val notary = statesToReissue.map { it.state.notary }.toSet().single()

        val requesterIdentity = requester ?: ourIdentity

        // all states need to have the same participants
        val participants = statesToReissue[0].state.data.participants

        val requesterHost = serviceHub.identityService.partyFromKey(requesterIdentity.owningKey)!!
        val issuerHost = serviceHub.identityService.partyFromKey(issuer.owningKey)!!

        val transactionsToSend = mutableListOf<SignedTransaction>()
        val sessions = (listOf(issuerHost) - requesterHost).map { initiateFlow(it) }

        // if issuer is a participant, they already have access to those transactions
        if (!participants.contains(issuer) && requesterHost != issuerHost) {
            val transactionHashes = stateRefsToReissue.map { it.txhash }
            transactionsToSend.addAll(transactionHashes.map {
                serviceHub.validatedTransactions.getTransaction(it)
                    ?: throw FlowException("Can't find transaction with hash $it")
            })
        }

        sessions.forEach {
            it.send(transactionsToSend.size)

            transactionsToSend.forEach { signedTransaction ->
                subFlow(SendTransactionFlow(it, signedTransaction))
            }
        }

        return subFlow(
            RequestReissuanceNonInitiating<T>(
                sessions,
                issuer,
                stateRefsToReissue,
                assetDestroyCommand,
                extraAssetDestroySigners,
                requester,
                notary
            )
        )
    }

}

@InitiatedBy(RequestReissuanceAndShareRequiredTransactions::class)
class ReceiveSignedTransaction(val otherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {

        val numTxToReceive = otherSession.receive<Int>().unwrap { it }

        if (numTxToReceive > 0) {
            (1..numTxToReceive).forEach { _ ->
                subFlow(ReceiveTransactionFlow(
                    otherSideSession = otherSession,
                    statesToRecord = StatesToRecord.ALL_VISIBLE
                ))
            }
        }

        subFlow(RequestReissuanceNonInitiatingResponder(otherSession))
    }
}
