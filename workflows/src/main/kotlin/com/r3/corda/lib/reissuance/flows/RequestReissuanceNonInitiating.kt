package com.r3.corda.lib.reissuance.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.reissuance.contracts.ReissuanceRequestContract
import com.r3.corda.lib.reissuance.states.ReissuanceRequest
import com.r3.corda.lib.reissuance.utils.convertSignedTransactionToByteArray
import com.r3.corda.lib.reissuance.utils.convertWireTransactionToByteArray
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

class RequestReissuanceNonInitiating<T>(
    private val sessions: List<FlowSession>,
    private val issuer: AbstractParty,
    private val stateRefsToReissue: List<StateRef>,
    private val assetDestroyCommand: CommandData,
    private val extraAssetDestroySigners: List<AbstractParty> = listOf(), // issuer is always a signer
    private val requester: AbstractParty? = null, // requester needs to be provided when using accounts
    private val notary : Party? = null
) : FlowLogic<SecureHash>() where T: ContractState {

    @Suspendable
    override fun call(): SecureHash {
        if(requester != null) {
            val requesterHost = serviceHub.identityService.partyFromKey(requester.owningKey)!!
            require(requesterHost == ourIdentity) { "Requester is not a valid account for the host" }
        }
        val requesterAbstractParty: AbstractParty = requester ?: ourIdentity

        require(!extraAssetDestroySigners.contains(issuer)) {
            "Issuer is always a signer and shouldn't be passed in as a part of extraAssetIssuanceSigners" }
        val issuanceSigners = listOf(issuer) + extraAssetDestroySigners

        val signers = listOf(requesterAbstractParty.owningKey)

        val reissuanceRequest = ReissuanceRequest(issuer, requesterAbstractParty, stateRefsToReissue.map {
            serviceHub.toStateAndRef<ContractState>(it) }, assetDestroyCommand, issuanceSigners)

        val notaryToUse = notary ?: getPreferredNotary(serviceHub)

        val destroyTransactionBuilder = TransactionBuilder(notaryToUse)
        stateRefsToReissue.forEach {
            destroyTransactionBuilder.addInputState(serviceHub.toStateAndRef<ContractState>(it))
        }
        destroyTransactionBuilder.addCommand(assetDestroyCommand, extraAssetDestroySigners.map { it.owningKey }.plus
            (signers).plus(issuer.owningKey).distinct())

        val destroyTx = destroyTransactionBuilder.toWireTransaction(serviceHub)

        val transactionBuilder = TransactionBuilder(notaryToUse)
        transactionBuilder.addOutputState(reissuanceRequest)
        transactionBuilder.addCommand(ReissuanceRequestContract.Commands.Create(), signers)
        val transactionByteArray = convertWireTransactionToByteArray(destroyTx)
        val attachmentId = serviceHub.attachments.importAttachment(transactionByteArray.inputStream(), ourIdentity.toString(), null)
        transactionBuilder.addAttachment(attachmentId)

        transactionBuilder.verify(serviceHub)
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder, signers)

        subFlow(
            FinalityFlow(
                transaction = signedTransaction,
                sessions = sessions
            )
        )
        return attachmentId
    }
}

class RequestReissuanceNonInitiatingResponder(
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
