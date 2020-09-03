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
import net.corda.core.identity.Party
import net.corda.core.internal.requiredContractClassName
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap

@InitiatingFlow
@StartableByRPC
class ReIssueState<T>(
    private val reIssuanceRequestStateAndRef: StateAndRef<ReIssuanceRequest<T>>
): FlowLogic<Unit>() where T: ContractState {

    @Suspendable
    override fun call() {
        val reIssuanceRequest = reIssuanceRequestStateAndRef.state.data
        val reIssuanceLock = ReIssuanceLock(reIssuanceRequest.issuer, reIssuanceRequest.requester,
            reIssuanceRequest.statesToReIssue)

        val notary = getPreferredNotary(serviceHub)
        val issuer = ourIdentity
        val lockSigners = listOf(issuer.owningKey)
        val reIssuedStatesSigners = reIssuanceRequest.issuanceSigners.map { it.owningKey }

        val transactionBuilder = TransactionBuilder(notary = notary)

        transactionBuilder.addInputState(reIssuanceRequestStateAndRef)
        transactionBuilder.addCommand(ReIssuanceRequestContract.Commands.Accept(), lockSigners)

        var encumbrance = 1
        reIssuanceRequest.statesToReIssue
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

        transactionBuilder.verify(serviceHub)
        var signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)

        val signers = (reIssuanceRequest.issuanceSigners + issuer).distinct()
        val otherParticipants = reIssuanceRequest.participants.filter { !signers.contains(it) }
//        val signersSessions = signers.filter { it != ourIdentity }.map { initiateFlow(it) }
//        val otherParticipantsSessions = otherParticipants.filter { it != ourIdentity }.map { initiateFlow(it as Party) }
//
//        signersSessions.forEach {
//            it.send(true)
//        }
//        otherParticipantsSessions.forEach {
//            it.send(false)
//        }
//
//        if(signersSessions.isNotEmpty()) {
//            signedTransaction = subFlow(CollectSignaturesFlow(signedTransaction, signersSessions))
//        }
//
//        subFlow(
//            FinalityFlow(
//                transaction = signedTransaction,
//                sessions = signersSessions + otherParticipantsSessions
//            )
//        )
    }
}

@InitiatedBy(ReIssueState::class)
class ReIssueStateResponder(
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
