package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty

@StartableByRPC
class RequestReIssuanceAndShareRequiredTransactions<T>(
    private val issuer: AbstractParty,
    private val statesToReIssue: List<StateAndRef<T>>,
    private val issuanceCommand: CommandData,
    private val issuanceSigners: List<AbstractParty> = listOf(issuer),
    private val requester: AbstractParty? = null // requester needs to be provided when using accounts
) : FlowLogic<Unit>() where T: ContractState {

    @Suspendable
    override fun call() {
        subFlow(
            RequestReIssuance<T>(issuer, statesToReIssue.map{ it.ref }, issuanceCommand, issuanceSigners, requester)
        )

        // all states need to have the same participants
        val participants = statesToReIssue[0].state.data.participants
        // if issuer is a participant, they already have access to those transactions
        if(!participants.contains(issuer)) {
            sendRequiredTransactions()
        }
    }

    fun sendRequiredTransactions() {
        val transactionHashes = statesToReIssue.map { it.ref.txhash }
        val transactionsToSend = transactionHashes.map {
            serviceHub.validatedTransactions.getTransaction(it)
                ?: throw FlowException("Can't find transaction with hash $it")
        }
        // TODO: filter transactions - some transactions can be ancestors of other transactions
        subFlow(
            SendSignedTransactions(issuer, transactionsToSend)
        )
    }
}
