package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import com.template.contracts.ReIssuanceLockContract
import com.template.contracts.ReIssuanceRequestContract
import com.template.states.ReIssuanceLock
import com.template.states.ReIssuanceRequest
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.internal.requiredContractClassName
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap

@InitiatingFlow
@StartableByRPC
class ReIssueStates<T>(
    private val reIssuanceRequestStateAndRef: StateAndRef<ReIssuanceRequest>
): FlowLogic<Unit>() where T: ContractState {

    @Suspendable
    override fun call() {
        val reIssuanceRequest = reIssuanceRequestStateAndRef.state.data

        val issuer = reIssuanceRequest.issuer
        val issuerHost = serviceHub.identityService.partyFromKey(issuer.owningKey)!!
        require(issuerHost == ourIdentity) { "Issuer is not a valid account for the host" }

        // we don't use withExternalIds as transaction can be only shared with a host
        val criteria = QueryCriteria.VaultQueryCriteria(stateRefs = reIssuanceRequest.stateRefsToReIssue)
        @Suppress("UNCHECKED_CAST")
        val statesToReIssue: List<StateAndRef<T>> = serviceHub.vaultService.queryBy<ContractState>(
            criteria=criteria).states as List<StateAndRef<T>>

        require(statesToReIssue.size == reIssuanceRequest.stateRefsToReIssue.size) { "Cannot validate states to re-issue" }

        val reIssuanceLock = ReIssuanceLock(reIssuanceRequest.issuer, reIssuanceRequest.requester, statesToReIssue)

        val notary = getPreferredNotary(serviceHub)

        val lockSigners = listOf(issuer.owningKey)
        val reIssuedStatesSigners = reIssuanceRequest.issuanceSigners.map { it.owningKey }

        val transactionBuilder = TransactionBuilder(notary = notary)
        transactionBuilder.addInputState(reIssuanceRequestStateAndRef)
        transactionBuilder.addCommand(ReIssuanceRequestContract.Commands.Accept(), lockSigners)

        var encumbrance = 1
        statesToReIssue
            .map { it.state.data }
            .forEach {
                transactionBuilder.addOutputState(
                    state = it,
                    contract = it.requiredContractClassName!!,
                    notary = notary,
                    encumbrance = encumbrance)
                encumbrance += 1
            }
        transactionBuilder.addCommand(reIssuanceRequest.issuanceCommand, reIssuedStatesSigners)

        transactionBuilder.addOutputState(
            state = reIssuanceLock,
            contract = ReIssuanceLockContract.contractId,
            notary = notary,
            encumbrance = 0)
        transactionBuilder.addCommand(ReIssuanceLockContract.Commands.Create(), lockSigners)

        val localSigners = (lockSigners + reIssuedStatesSigners)
            .distinct()
            .filter { serviceHub.identityService.partyFromKey(it)!! == ourIdentity }
        transactionBuilder.verify(serviceHub)
        var signedTransaction = serviceHub.signInitialTransaction(transactionBuilder, localSigners)

        val signers = (reIssuanceRequest.issuanceSigners + issuer).distinct()
        val otherParticipants = reIssuanceRequest.participants.filter { !signers.contains(it) }

        val signersSessions = subFlow(GenerateRequiredFlowSessions(signers))
        val otherParticipantsSessions = subFlow(GenerateRequiredFlowSessions(otherParticipants))
        subFlow(SendSignerFlags(signersSessions, otherParticipantsSessions))

        if(signersSessions.isNotEmpty()) {
            signedTransaction = subFlow(CollectSignaturesFlow(signedTransaction, signersSessions))
        }

        subFlow(
            FinalityFlow(
                transaction = signedTransaction,
                sessions = signersSessions + otherParticipantsSessions
            )
        )
    }

}

@InitiatedBy(ReIssueStates::class)
class ReIssueStatesResponder(
    private val otherSession: FlowSession
) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val needsToSignTransaction = otherSession.receive<Boolean>().unwrap { it }
        // only sign if instructed to do so
        if (needsToSignTransaction) {
            subFlow(object : SignTransactionFlow(otherSession) {
                override fun checkTransaction(stx: SignedTransaction) { }
            })
        }
        // always save the transaction
        return subFlow(ReceiveFinalityFlow(otherSideSession = otherSession))
    }
}
