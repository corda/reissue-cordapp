package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import com.template.contracts.ReIssuanceRequestContract
import com.template.states.ReIssuanceRequest
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class CreateReIssuanceRequest<T>(
    private val issuer: Party,
    private val statesToReIssue: List<StateAndRef<T>>,
    private val issuanceCommand: CommandData,
    private val issuanceSigners: List<Party> = listOf(issuer)
) : FlowLogic<Unit>() where T: ContractState {

    @Suspendable
    override fun call() {
        val requester: Party = ourIdentity
        val signers = listOf(requester.owningKey)

        val reIssuanceRequest = ReIssuanceRequest(issuer, requester, statesToReIssue, issuanceCommand, issuanceSigners)

        val transactionBuilder = TransactionBuilder(notary = getPreferredNotary(serviceHub))
        transactionBuilder.addOutputState(reIssuanceRequest)
        transactionBuilder.addCommand(ReIssuanceRequestContract.Commands.Create(), signers)

        transactionBuilder.verify(serviceHub)
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)

        val issuerSession = initiateFlow(issuer)
        subFlow(
            FinalityFlow(
                transaction = signedTransaction,
                sessions = listOf(issuerSession)
            )
        )
    }
}

@InitiatedBy(CreateReIssuanceRequest::class)
class CreateReIssuanceRequestResponder(
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
