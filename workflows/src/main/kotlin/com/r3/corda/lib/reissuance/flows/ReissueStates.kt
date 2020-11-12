package com.r3.corda.lib.reissuance.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import com.r3.corda.lib.reissuance.contracts.ReissuanceLockContract
import com.r3.corda.lib.reissuance.contracts.ReissuanceRequestContract
import com.r3.corda.lib.reissuance.states.ReissuanceLock
import com.r3.corda.lib.reissuance.states.ReissuanceRequest
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.internal.requiredContractClassName
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap

@InitiatingFlow
@StartableByRPC
class ReissueStates<T>(
    private val reissuanceRequestStateAndRef: StateAndRef<ReissuanceRequest>,
    private val extraAssetExitCommandSigners: List<AbstractParty> = listOf(reissuanceRequestStateAndRef.state.data.issuer) // requester and notary signatures are always required
): FlowLogic<SecureHash>() where T: ContractState {

    @Suspendable
    override fun call(): SecureHash {
        val reissuanceRequest = reissuanceRequestStateAndRef.state.data

        val notary = getPreferredNotary(serviceHub)
        val requester = reissuanceRequest.requester
        val issuer = reissuanceRequest.issuer
        val issuerHost = serviceHub.identityService.partyFromKey(issuer.owningKey)!!
        require(issuerHost == ourIdentity) { "Issuer is not a valid account for the host" }

        // we don't use withExternalIds when querying a vault as a transaction can be only shared with a host

        @Suppress("UNCHECKED_CAST")
        val statesToReissue: List<StateAndRef<T>> = serviceHub.vaultService.queryBy<ContractState>(
            criteria=QueryCriteria.VaultQueryCriteria(stateRefs = reissuanceRequest.stateRefsToReissue)
        ).states as List<StateAndRef<T>>

        @Suppress("UNCHECKED_CAST")
        val locks: List<StateAndRef<ReissuanceLock<T>>> =
            serviceHub.vaultService.queryBy<ReissuanceLock<ContractState>>().states
                as List<StateAndRef<ReissuanceLock<T>>>
        val reissuedStatesRefs = locks.flatMap { it.state.data.originalStates }.map { it.ref }
        reissuanceRequest.stateRefsToReissue.forEach {
            require(!reissuedStatesRefs.contains(it)) { "State ${it} has been already re-issued" }
        }

        require(statesToReissue.size == reissuanceRequest.stateRefsToReissue.size) {
            "Cannot validate states to re-issue" }

        require(!extraAssetExitCommandSigners.contains(notary)) {
            "Notary is always a signer and shouldn't be passed in as a part of extraAssetExitCommandSigners" }
        require(!extraAssetExitCommandSigners.contains(requester)) {
            "Requester is always a signer and shouldn't be passed in as a part of extraAssetExitCommandSigners" }
        val reissuanceLock = ReissuanceLock(
            reissuanceRequest.issuer,
            reissuanceRequest.requester,
            statesToReissue,
            extraAssetExitCommandSigners = extraAssetExitCommandSigners
        )

        val lockSigners = listOf(issuer.owningKey)
        val reissuedStatesSigners = reissuanceRequest.assetIssuanceSigners.map { it.owningKey }

        val transactionBuilder = TransactionBuilder(notary = notary)
        transactionBuilder.addInputState(reissuanceRequestStateAndRef)
        transactionBuilder.addCommand(ReissuanceRequestContract.Commands.Accept(), lockSigners)

        var encumbrance = 1
        statesToReissue
            .map { it.state.data }
            .forEach {
                transactionBuilder.addOutputState(
                    state = it,
                    contract = it.requiredContractClassName!!,
                    notary = notary,
                    encumbrance = encumbrance)
                encumbrance += 1
            }
        transactionBuilder.addCommand(reissuanceRequest.assetIssuanceCommand, reissuedStatesSigners)

        transactionBuilder.addOutputState(
            state = reissuanceLock,
            contract = ReissuanceLockContract.contractId,
            notary = notary,
            encumbrance = 0)
        transactionBuilder.addCommand(ReissuanceLockContract.Commands.Create(), lockSigners)

        val localSigners = (lockSigners + reissuedStatesSigners)
            .distinct()
            .filter { serviceHub.identityService.partyFromKey(it)!! == ourIdentity }
        transactionBuilder.verify(serviceHub)
        var signedTransaction = serviceHub.signInitialTransaction(transactionBuilder, localSigners)

        // as some of the participants might be signers and some might not, we are sending them a flag which informs
        // them if they are expected to sign the transaction or not
        val signers = (reissuanceRequest.assetIssuanceSigners + issuer).distinct()
        val otherParticipants = reissuanceRequest.participants.filter { !signers.contains(it) }

        val signersSessions = subFlow(GenerateRequiredFlowSessions(signers))
        val otherParticipantsSessions = subFlow(GenerateRequiredFlowSessions(otherParticipants))
        subFlow(SendSignerFlags(signersSessions, otherParticipantsSessions))

        if(signersSessions.isNotEmpty()) {
            signedTransaction = subFlow(CollectSignaturesFlow(signedTransaction, signersSessions))
        }

        return subFlow(
            FinalityFlow(
                transaction = signedTransaction,
                sessions = signersSessions + otherParticipantsSessions
            )
        ).id
    }

}

@InitiatedBy(ReissueStates::class)
class ReissueStatesResponder(
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
