package com.r3.corda.lib.reissuance.dummy_flows

import com.r3.corda.lib.reissuance.dummy_contracts.DummyStateRequiringAcceptanceContract
import com.r3.corda.lib.reissuance.dummy_contracts.DummyStateRequiringAllParticipantsSignaturesContract
import com.r3.corda.lib.reissuance.dummy_contracts.SimpleDummyStateContract
import com.r3.corda.lib.reissuance.dummy_states.DummyStateRequiringAcceptance
import com.r3.corda.lib.reissuance.dummy_states.DummyStateRequiringAllParticipantsSignatures
import com.r3.corda.lib.reissuance.dummy_states.SimpleDummyState
import com.r3.corda.lib.reissuance.flows.FinalizeDestroyTransaction
import com.r3.corda.lib.reissuance.states.ReissuanceLock
import com.r3.corda.lib.reissuance.states.ReissuanceRequest
import com.r3.corda.lib.reissuance.utils.convertSignedTransactionToByteArray
import com.r3.corda.lib.tokens.contracts.commands.IssueTokenCommand
import com.r3.corda.lib.tokens.contracts.commands.RedeemTokenCommand
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import net.corda.core.contracts.TransactionVerificationException
import net.corda.testing.node.internal.TestStartedNode
import org.junit.Test
import java.util.*

class DeleteReissuedStatesAndLockTest: AbstractFlowTest() {

    private inline fun <reified T> verifyDeletedReissuedStatesAndLock(
        statesToReissue: List<StateAndRef<T>>,
        accountUUID: UUID? = null,
        node: TestStartedNode = aliceNode,
        extraUnencumberedStates: Int = 0
    ) where T: ContractState {
        val encumberedStates = getStateAndRefs<T>(node, encumbered = true, accountUUID = accountUUID)
        val unencumberedStates = getStateAndRefs<T>(node, encumbered = false, accountUUID = accountUUID)
        assertThat(encumberedStates, empty())
        assertThat(unencumberedStates, hasSize(`is`(statesToReissue.size + extraUnencumberedStates)))
        assertThat(unencumberedStates, hasItems(*statesToReissue.toTypedArray()))
    }

    @Test
    fun `Re-issued SimpleDummyState and corresponding ReissuanceLock are deleted`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)

        val statesToReissue = getStateAndRefs<SimpleDummyState>(aliceNode) // there is just 1
        val requestReissuanceTransactionId = createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            statesToReissue,
            SimpleDummyStateContract.Commands.Delete(),
            issuerParty
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]
        reissueRequestedStates<SimpleDummyState>(issuerNode, reissuanceRequest, SimpleDummyStateContract.Commands
            .Create(), listOf(), requestReissuanceTransactionId)

        val reissuedSimpleDummyStates = getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = true)
        val lockState = getStateAndRefs<ReissuanceLock>(aliceNode)[0]
        deleteReissuedStatesAndLock(
            aliceNode,
            lockState,
            reissuedSimpleDummyStates,
            SimpleDummyStateContract.Commands.Delete(),
            txAttachmentId = requestReissuanceTransactionId
        )

        verifyDeletedReissuedStatesAndLock(statesToReissue)
    }

    @Test
    fun `Re-issued SimpleDummyState and corresponding ReissuanceLock are deleted on notary 2`() {
        initialiseParties()
        createSimpleDummyStateOnNotary(aliceParty, notary2Party)

        val statesToReissue = getStateAndRefs<SimpleDummyState>(aliceNode) // there is just 1
        val requestReissuanceTransactionId = createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            statesToReissue,
            SimpleDummyStateContract.Commands.Delete(),
            issuerParty
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]
        reissueRequestedStates<SimpleDummyState>(issuerNode, reissuanceRequest, SimpleDummyStateContract.Commands.Create(),
            listOf(), requestReissuanceTransactionId)

        val reissuedSimpleDummyStates = getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = true)
        val lockState = getStateAndRefs<ReissuanceLock>(aliceNode)[0]
        deleteReissuedStatesAndLock(
            aliceNode,
            lockState,
            reissuedSimpleDummyStates,
            SimpleDummyStateContract.Commands.Delete(),
            txAttachmentId = requestReissuanceTransactionId
        )

        verifyDeletedReissuedStatesAndLock(statesToReissue)
    }

    @Test
    fun `Re-issued DummyStateRequiringAcceptance is unencumbered after the original state are deleted`() {
        initialiseParties()
        createDummyStateRequiringAcceptance(aliceParty)

        val statesToReissue = getStateAndRefs<DummyStateRequiringAcceptance>(aliceNode)
        val requestReissuanceTransactionId = createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            statesToReissue,
            DummyStateRequiringAcceptanceContract.Commands.Delete(),
            issuerParty,
            listOf(acceptorParty)
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]
        reissueRequestedStates<DummyStateRequiringAcceptance>(issuerNode, reissuanceRequest,
            DummyStateRequiringAcceptanceContract.Commands.Create(), listOf(acceptorParty, issuerParty), requestReissuanceTransactionId)

        val reissuedDummyStatesRequiringAcceptance = getStateAndRefs<DummyStateRequiringAcceptance>(aliceNode, encumbered = true)
        val lockState = getStateAndRefs<ReissuanceLock>(aliceNode)[0]
        deleteReissuedStatesAndLock(
            aliceNode,
            lockState,
            reissuedDummyStatesRequiringAcceptance,
            DummyStateRequiringAcceptanceContract.Commands.Delete(),
            listOf(issuerParty, aliceParty, acceptorParty),
            txAttachmentId = requestReissuanceTransactionId
        )

        verifyDeletedReissuedStatesAndLock(statesToReissue)
    }

    @Test
    fun `Re-issued DummyStateRequiringAllParticipantsSignatures and corresponding ReissuanceLock are deleted`() {
        initialiseParties()
        createDummyStateRequiringAllParticipantsSignatures(aliceParty)

        val statesToReissue = getStateAndRefs<DummyStateRequiringAllParticipantsSignatures>(aliceNode)
        val requestReissuanceTransactionId = createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            statesToReissue,
            DummyStateRequiringAllParticipantsSignaturesContract.Commands.Delete(),
            issuerParty,
            listOf(acceptorParty, aliceParty)
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]
        reissueRequestedStates<DummyStateRequiringAllParticipantsSignatures>(issuerNode, reissuanceRequest,
            DummyStateRequiringAllParticipantsSignaturesContract.Commands.Create(), listOf(issuerParty, acceptorParty),
            requestReissuanceTransactionId)

        val reissuedDummyStatesRequiringAcceptance = getStateAndRefs<DummyStateRequiringAllParticipantsSignatures>(
            aliceNode, encumbered = true)
        val lockState = getStateAndRefs<ReissuanceLock>(aliceNode)[0]
        deleteReissuedStatesAndLock(
            aliceNode,
            lockState,
            reissuedDummyStatesRequiringAcceptance,
            DummyStateRequiringAllParticipantsSignaturesContract.Commands.Delete(),
            listOf(issuerParty, aliceParty, acceptorParty),
            txAttachmentId = requestReissuanceTransactionId
        )

        verifyDeletedReissuedStatesAndLock(statesToReissue)
    }

    @Test
    fun `Re-issued tokens and corresponding ReissuanceLock are deleted`() {
        initialiseParties()
        issueTokens(aliceParty, 50)

        val statesToReissue = getTokens(aliceNode)
        val tokenIndices = statesToReissue.indices.toList()
        val requestReissuanceTransactionId = createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            statesToReissue,
            RedeemTokenCommand(issuedTokenType, tokenIndices),
            issuerParty
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]
        reissueRequestedStates<FungibleToken>(issuerNode, reissuanceRequest, IssueTokenCommand(issuedTokenType,
            tokenIndices), listOf(issuerParty), requestReissuanceTransactionId)

        val reissuedTokens = getStateAndRefs<FungibleToken>(aliceNode, encumbered = true)
        val lockState = getStateAndRefs<ReissuanceLock>(aliceNode)[0]
        deleteReissuedStatesAndLock(
            aliceNode,
            lockState,
            reissuedTokens,
            RedeemTokenCommand(issuedTokenType, tokenIndices, listOf()),
            txAttachmentId = requestReissuanceTransactionId
        )

        verifyDeletedReissuedStatesAndLock(statesToReissue)
    }

    @Test
    fun `Re-issued tokens and corresponding ReissuanceLock are deleted when only part of tokens had been re-issued`() {
        initialiseParties()
        issueTokens(aliceParty, 50)
        transferTokens(aliceNode, bobParty, 30)
        transferTokens(bobNode, aliceParty, 30)

        val tokens = getTokens(aliceNode)
        val statesToReissue = listOf(tokens[1]) // 30 tokens
        val tokenIndices = listOf(0)

        val requestReissuanceTransactionId = createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            statesToReissue,
            RedeemTokenCommand(issuedTokenType, tokenIndices),
            issuerParty
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]
        reissueRequestedStates<FungibleToken>(issuerNode, reissuanceRequest, IssueTokenCommand(issuedTokenType, tokenIndices),
            listOf(issuerParty), requestReissuanceTransactionId)

        val reissuedTokens = getStateAndRefs<FungibleToken>(aliceNode, encumbered = true)
        val lockState = getStateAndRefs<ReissuanceLock>(aliceNode)[0]
        deleteReissuedStatesAndLock(
            aliceNode,
            lockState,
            reissuedTokens,
            RedeemTokenCommand(issuedTokenType, tokenIndices, listOf()),
            txAttachmentId = requestReissuanceTransactionId
        )

        verifyDeletedReissuedStatesAndLock(statesToReissue, extraUnencumberedStates = 1)
    }

    @Test
    fun `Re-issued tokens and corresponding ReissuanceLock are deleted when many token inputs were re-issued`() {
        initialiseParties()
        issueTokens(aliceParty, 50)
        transferTokens(aliceNode, bobParty, 30)
        transferTokens(bobNode, aliceParty, 30)

        val statesToReissue = getTokens(aliceNode)
        val tokenIndices = statesToReissue.indices.toList()
        val requestReissuanceTransactionId = createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            statesToReissue,
            RedeemTokenCommand(issuedTokenType, tokenIndices),
            issuerParty
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]
        reissueRequestedStates<FungibleToken>(issuerNode, reissuanceRequest,
            IssueTokenCommand(issuedTokenType, tokenIndices), listOf(issuerParty), requestReissuanceTransactionId)

        val reissuedTokens = getStateAndRefs<FungibleToken>(aliceNode, encumbered = true)
        val lockState = getStateAndRefs<ReissuanceLock>(aliceNode)[0]
        deleteReissuedStatesAndLock(
            aliceNode,
            lockState,
            reissuedTokens,
            RedeemTokenCommand(issuedTokenType, tokenIndices, listOf()),
            txAttachmentId = requestReissuanceTransactionId
        )

        verifyDeletedReissuedStatesAndLock(statesToReissue)
    }

    @Test
    fun `Re-issued SimpleDummyState and corresponding ReissuanceLock are deleted - accounts on the same host`() {
        initialisePartiesForAccountsOnTheSameHost()
        createSimpleDummyStateForAccount(employeeNode, employeeAliceParty)

        val statesToReissue = getStateAndRefs<SimpleDummyState>(employeeNode,
            accountUUID = employeeAliceAccount.identifier.id)
        val requestReissuanceTransactionId = createReissuanceRequestAndShareRequiredTransactions(
            employeeNode,
            statesToReissue,
            SimpleDummyStateContract.Commands.Delete(),
            employeeIssuerParty,
            requester = employeeAliceParty
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(employeeNode)[0]
        reissueRequestedStates<SimpleDummyState>(employeeNode, reissuanceRequest, SimpleDummyStateContract.Commands.Create(),
            listOf(), requestReissuanceTransactionId)

        val reissuedSimpleDummyStates = getStateAndRefs<SimpleDummyState>(employeeNode, encumbered = true,
            accountUUID = employeeAliceAccount.identifier.id)
        val lockState = getStateAndRefs<ReissuanceLock>(employeeNode,
            accountUUID = employeeAliceAccount.identifier.id)[0]
        deleteReissuedStatesAndLock(
            employeeNode,
            lockState,
            reissuedSimpleDummyStates,
            SimpleDummyStateContract.Commands.Delete(),
            txAttachmentId = requestReissuanceTransactionId
        )

        verifyDeletedReissuedStatesAndLock(statesToReissue, accountUUID = employeeAliceAccount.identifier.id,
            node = employeeNode)
    }

    @Test
    fun `Re-issued SimpleDummyState and corresponding ReissuanceLock are deleted - accounts on different hosts`() {
        initialisePartiesForAccountsOnDifferentHosts()
        createSimpleDummyStateForAccount(issuerNode, employeeAliceParty)

        val statesToReissue = getStateAndRefs<SimpleDummyState>(aliceNode,
            accountUUID = employeeAliceAccount.identifier.id)
        val requestReissuanceTransactionId = createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            statesToReissue,
            SimpleDummyStateContract.Commands.Delete(),
            employeeIssuerParty,
            requester = employeeAliceParty
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(aliceNode)[0]
        reissueRequestedStates<SimpleDummyState>(issuerNode, reissuanceRequest, SimpleDummyStateContract.Commands.Create(),
            listOf(), requestReissuanceTransactionId)

        val reissuedSimpleDummyStates = getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = true,
            accountUUID = employeeAliceAccount.identifier.id)
        val lockState = getStateAndRefs<ReissuanceLock>(aliceNode,
            accountUUID = employeeAliceAccount.identifier.id)[0]
        deleteReissuedStatesAndLock(
            aliceNode,
            lockState,
            reissuedSimpleDummyStates,
            SimpleDummyStateContract.Commands.Delete(),
            txAttachmentId = requestReissuanceTransactionId
        )

        verifyDeletedReissuedStatesAndLock(statesToReissue, accountUUID = employeeAliceAccount.identifier.id)
    }

    @Test(expected = TransactionVerificationException::class)
    fun `Re-issued SimpleDummyState and corresponding ReissuanceLock can't be deleted after they are unlocked`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)

        val simpleDummyStatesToReissue = getStateAndRefs<SimpleDummyState>(aliceNode) // there is just 1
        val requestReissuanceTransactionId = createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            simpleDummyStatesToReissue,
            SimpleDummyStateContract.Commands.Delete(),
            issuerParty
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]
        reissueRequestedStates<SimpleDummyState>(issuerNode, reissuanceRequest, SimpleDummyStateContract.Commands
            .Create(), listOf(), requestReissuanceTransactionId)

        val fullySignedTransaction = runFlow(aliceNode, FinalizeDestroyTransaction(requestReissuanceTransactionId))
        val transactionByteArray = convertSignedTransactionToByteArray(fullySignedTransaction)
        val attachmentId = aliceNode.services.attachments.importAttachment(transactionByteArray.inputStream(), aliceParty.toString(),
            null)

        unlockReissuedState<SimpleDummyState>(
            aliceNode,
            listOf(attachmentId),
            SimpleDummyStateContract.Commands.Update(),
            getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = true),
            getStateAndRefs<ReissuanceLock>(aliceNode, encumbered = true)[0]
        )

        val reissuedSimpleDummyStates = getStateAndRefs<SimpleDummyState>(aliceNode)
        val lockState = getStateAndRefs<ReissuanceLock>(aliceNode)[0]
        deleteReissuedStatesAndLock(
            aliceNode,
            lockState,
            reissuedSimpleDummyStates,
            SimpleDummyStateContract.Commands.Delete(),
            txAttachmentId = requestReissuanceTransactionId
        )
    }

    @Test(expected = TransactionVerificationException::class)
    fun `Make sure delete transaction can't produce any outputs`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)

        val simpleDummyStatesToReissue = getStateAndRefs<SimpleDummyState>(aliceNode) // there is just 1
        val requestReissuanceTransactionId = createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            simpleDummyStatesToReissue,
            SimpleDummyStateContract.Commands.Delete(),
            issuerParty
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]
        reissueRequestedStates<SimpleDummyState>(issuerNode, reissuanceRequest, SimpleDummyStateContract.Commands
            .Create(), listOf(), requestReissuanceTransactionId)

        val reissuedSimpleDummyStates = getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = true)
        val lockState = getStateAndRefs<ReissuanceLock>(aliceNode)[0]
        deleteReissuedStatesAndLockUsingModifiedFlow(
            aliceNode,
            lockState,
            reissuedSimpleDummyStates,
            SimpleDummyStateContract.Commands.Delete()
        )
    }

}
