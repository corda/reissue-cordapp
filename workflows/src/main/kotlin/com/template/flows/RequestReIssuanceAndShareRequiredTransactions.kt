package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateRef
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty

@StartableByRPC
class RequestReIssuanceAndShareRequiredTransactions<T>(
    private val issuer: AbstractParty,
    private val stateRefsToReIssue: List<StateRef>,
    private val issuanceCommand: CommandData,
    private val issuanceSigners: List<AbstractParty> = listOf(issuer),
    private val requester: AbstractParty? = null // requester needs to be provided when using accounts
) : FlowLogic<Unit>() where T: ContractState {

    @Suspendable
    override fun call() {
        subFlow(
            RequestReIssuance<T>(issuer, stateRefsToReIssue, issuanceCommand, issuanceSigners, requester)
        )

        val transactionHashes = stateRefsToReIssue.map { it.txhash }
        // We must use SendTransactionFlow as SendStateAndRefFlow doesn't let us override StatesToRecord.
        val transactionsToSend = transactionHashes.map {
            serviceHub.validatedTransactions.getTransaction(it)
                ?: throw FlowException("Can't find transaction with hash $it")
        }

        // TODO: send only required transactions
        // - don't send anything is issuer is a participant
        // - filter transactions - some transactions can be ancestors of other transactions

        subFlow(
            SendSignedTransactions(issuer, transactionsToSend)
        )

    }
}
