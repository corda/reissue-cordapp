package com.r3.corda.lib.reissuance.dummy_flows

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import com.r3.corda.lib.reissuance.dummy_contracts.DummyStateRequiringAcceptanceContract
import com.r3.corda.lib.reissuance.dummy_contracts.DummyStateRequiringAllParticipantsSignaturesContract
import com.r3.corda.lib.reissuance.dummy_contracts.DummyStateWithInvalidEqualsMethodContract
import com.r3.corda.lib.reissuance.dummy_contracts.SimpleDummyStateContract
import com.r3.corda.lib.reissuance.states.ReIssuanceRequest
import com.r3.corda.lib.reissuance.dummy_states.DummyStateRequiringAcceptance
import com.r3.corda.lib.reissuance.dummy_states.DummyStateRequiringAllParticipantsSignatures
import com.r3.corda.lib.reissuance.dummy_states.DummyStateWithInvalidEqualsMethod
import com.r3.corda.lib.reissuance.dummy_states.SimpleDummyState
import com.r3.corda.lib.reissuance.states.ReIssuanceLock
import com.r3.corda.lib.reissuance.utils.convertSignedTransactionToByteArray
import com.r3.corda.lib.tokens.contracts.commands.IssueTokenCommand
import com.r3.corda.lib.tokens.contracts.commands.MoveTokenCommand
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import net.corda.core.contracts.*
import net.corda.core.crypto.*
import net.corda.core.node.services.queryBy
import net.corda.core.transactions.*
import net.corda.testing.core.singleIdentity
import org.junit.Test

class UnlockReIssuedStatesTest: AbstractFlowTest() {

    @Test
    fun `Re-issued SimpleDummyState is unencumbered after the original state is deleted`() {
        initialiseParties()
        val issuanceTransactionId = createSimpleDummyState(aliceParty)
        val update1TransactionId = updateSimpleDummyState(aliceNode, bobParty)
        val update2TransactionId = updateSimpleDummyState(bobNode, charlieParty)
        val update3TransactionId = updateSimpleDummyState(charlieNode, aliceParty)
        val update4TransactionId = updateSimpleDummyState(aliceNode, bobParty)
        val update5TransactionId = updateSimpleDummyState(bobNode, charlieParty)
        val update6TransactionId = updateSimpleDummyState(charlieNode, aliceParty)

        val transactionBackChainBeforeReIssuance = getTransactionBackChain(aliceNode, update6TransactionId)
        val expectedBackChainBeforeReIssuance = setOf(issuanceTransactionId, update1TransactionId,
            update2TransactionId, update3TransactionId, update4TransactionId, update5TransactionId,
            update6TransactionId)
        assertThat(transactionBackChainBeforeReIssuance, `is`(expectedBackChainBeforeReIssuance))

        val simpleDummyState = getStateAndRefs<SimpleDummyState>(aliceNode)[0]
        val requestReIssuanceTransactionId = createReIssuanceRequestAndShareRequiredTransactions(aliceNode,
            listOf(simpleDummyState), SimpleDummyStateContract.Commands.Create(), issuerParty)

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]

        val reIssueStatesTransactionId = reIssueRequestedStates<SimpleDummyState>(issuerNode, reIssuanceRequest,
            issuerIsRequiredExitCommandSigner = false)

        val exitTransactionId = deleteSimpleDummyState(aliceNode)

        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode, exitTransactionId)

        val unlockReIssuedStatesTransactionId = unlockReIssuedState(
            aliceNode, listOf(attachmentSecureHash), SimpleDummyStateContract.Commands.Update(),
            getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = true),
            getStateAndRefs<ReIssuanceLock<SimpleDummyState>>(aliceNode, encumbered = true)[0]
        )

        val encumberedStates = getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = true)
        val unencumberedStates = getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = false)
        assertThat(encumberedStates, empty())
        assertThat(unencumberedStates, hasSize(`is`(1)))
        assertThat(unencumberedStates[0].state.data, `is`(simpleDummyState.state.data))

        val transactionBackChainAfterReIssuance = getTransactionBackChain(aliceNode, unlockReIssuedStatesTransactionId)
        val expectedTransactionBackChainAfterReIssuance = setOf(requestReIssuanceTransactionId,
            reIssueStatesTransactionId, unlockReIssuedStatesTransactionId)
        assertThat(transactionBackChainAfterReIssuance, `is`(expectedTransactionBackChainAfterReIssuance))
    }

    @Test
    fun `Re-issued DummyStateRequiringAcceptance is unencumbered after the original state is deleted`() {
        initialiseParties()
        val issuanceTransactionId = createDummyStateRequiringAcceptance(aliceParty)
        val update1TransactionId = updateDummyStateRequiringAcceptance(aliceNode, bobParty)
        val update2TransactionId = updateDummyStateRequiringAcceptance(bobNode, charlieParty)
        val update3TransactionId = updateDummyStateRequiringAcceptance(charlieNode, aliceParty)
        val update4TransactionId = updateDummyStateRequiringAcceptance(aliceNode, bobParty)
        val update5TransactionId = updateDummyStateRequiringAcceptance(bobNode, charlieParty)
        val update6TransactionId = updateDummyStateRequiringAcceptance(charlieNode, aliceParty)

        val transactionBackChainBeforeReIssuance = getTransactionBackChain(aliceNode, update6TransactionId)
        val expectedBackChainBeforeReIssuance = setOf(issuanceTransactionId, update1TransactionId,
            update2TransactionId, update3TransactionId, update4TransactionId, update5TransactionId,
            update6TransactionId)
        assertThat(transactionBackChainBeforeReIssuance, `is`(expectedBackChainBeforeReIssuance))

        val dummyStateRequiringAcceptance = getStateAndRefs<DummyStateRequiringAcceptance>(aliceNode)[0]
        val requestReIssuanceTransactionId = createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(dummyStateRequiringAcceptance),
            DummyStateRequiringAcceptanceContract.Commands.Create(),
            issuerParty,
            listOf(issuerParty, acceptorParty)
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]

        val reIssueStatesTransactionId = reIssueRequestedStates<DummyStateRequiringAcceptance>(issuerNode,
            reIssuanceRequest, issuerIsRequiredExitCommandSigner = false)

        val exitTransactionId = deleteDummyStateRequiringAcceptance(aliceNode)

        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode, exitTransactionId)

        val unlockReIssuedStatesTransactionId = unlockReIssuedState<DummyStateRequiringAcceptance>(aliceNode,
            listOf(attachmentSecureHash), DummyStateRequiringAcceptanceContract.Commands.Update(),
            getStateAndRefs<DummyStateRequiringAcceptance>(aliceNode, encumbered = true),
            getStateAndRefs<ReIssuanceLock<DummyStateRequiringAcceptance>>(aliceNode, encumbered = true)[0],
            listOf(aliceParty, issuerParty, acceptorParty))

        val encumberedStates = getStateAndRefs<DummyStateRequiringAcceptance>(aliceNode, encumbered = true)
        val unencumberedStates = getStateAndRefs<DummyStateRequiringAcceptance>(aliceNode, encumbered = false)
        assertThat(encumberedStates, empty())
        assertThat(unencumberedStates, hasSize(`is`(1)))
        assertThat(unencumberedStates[0].state.data, `is`(dummyStateRequiringAcceptance.state.data))

        val transactionBackChainAfterReIssuance = getTransactionBackChain(aliceNode, unlockReIssuedStatesTransactionId)
        val expectedTransactionBackChainAfterReIssuance = setOf(requestReIssuanceTransactionId,
            reIssueStatesTransactionId, unlockReIssuedStatesTransactionId)
        assertThat(transactionBackChainAfterReIssuance, `is`(expectedTransactionBackChainAfterReIssuance))
    }

    @Test
    fun `DummyStateRequiringAllParticipantsSignatures is re-issued`() {
        initialiseParties()
        val issuanceTransactionId = createDummyStateRequiringAllParticipantsSignatures(aliceParty)
        val update1TransactionId = updateDummyStateRequiringAllParticipantsSignatures(aliceNode, bobParty)
        val update2TransactionId = updateDummyStateRequiringAllParticipantsSignatures(bobNode, charlieParty)
        val update3TransactionId = updateDummyStateRequiringAllParticipantsSignatures(charlieNode, aliceParty)
        val update4TransactionId = updateDummyStateRequiringAllParticipantsSignatures(aliceNode, bobParty)
        val update5TransactionId = updateDummyStateRequiringAllParticipantsSignatures(bobNode, charlieParty)
        val update6TransactionId = updateDummyStateRequiringAllParticipantsSignatures(charlieNode, aliceParty)

        val transactionBackChainBeforeReIssuance = getTransactionBackChain(aliceNode, update6TransactionId)
        val expectedBackChainBeforeReIssuance = setOf(issuanceTransactionId, update1TransactionId,
            update2TransactionId, update3TransactionId, update4TransactionId, update5TransactionId,
            update6TransactionId)
        assertThat(transactionBackChainBeforeReIssuance, `is`(expectedBackChainBeforeReIssuance))

        val dummyStateRequiringAllParticipantsSignatures = getStateAndRefs<DummyStateRequiringAllParticipantsSignatures>(aliceNode)[0]

        val requestReIssuanceTransactionId = createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(dummyStateRequiringAllParticipantsSignatures),
            DummyStateRequiringAllParticipantsSignaturesContract.Commands.Create(),
            issuerParty,
            listOf(aliceParty, issuerParty, acceptorParty)
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]

        val reIssueStatesTransactionId = reIssueRequestedStates<DummyStateRequiringAllParticipantsSignatures>(
            issuerNode, reIssuanceRequest, issuerIsRequiredExitCommandSigner = true)

        val exitTransactionId = deleteDummyStateRequiringAllParticipantsSignatures(aliceNode)

        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode, exitTransactionId)

        val unlockReIssuedStatesTransactionId = unlockReIssuedState<DummyStateRequiringAllParticipantsSignatures>(
            aliceNode, listOf(attachmentSecureHash),
            DummyStateRequiringAllParticipantsSignaturesContract.Commands.Update(),
            getStateAndRefs<DummyStateRequiringAllParticipantsSignatures>(aliceNode, encumbered = true),
            getStateAndRefs<ReIssuanceLock<DummyStateRequiringAllParticipantsSignatures>>(aliceNode, encumbered = true)[0],
            listOf(aliceParty, issuerParty, acceptorParty)
        )

        val encumberedStates = getStateAndRefs<DummyStateRequiringAllParticipantsSignatures>(aliceNode, encumbered = true)
        val unencumberedStates = getStateAndRefs<DummyStateRequiringAllParticipantsSignatures>(aliceNode, encumbered = false)
        assertThat(encumberedStates, empty())
        assertThat(unencumberedStates, hasSize(`is`(1)))
        assertThat(unencumberedStates[0].state.data, `is`(dummyStateRequiringAllParticipantsSignatures.state.data))

        val transactionBackChainAfterReIssuance = getTransactionBackChain(aliceNode, unlockReIssuedStatesTransactionId)
        val expectedTransactionBackChainAfterReIssuance = setOf(requestReIssuanceTransactionId,
            reIssueStatesTransactionId, unlockReIssuedStatesTransactionId)
        assertThat(transactionBackChainAfterReIssuance, `is`(expectedTransactionBackChainAfterReIssuance))
    }

    @Test
    fun `Re-issued token is unencumbered after the original state is deleted`() {
        initialiseParties()
        val issuanceTransactionId = issueTokens(aliceParty, 50)
        val update1TransactionId = transferTokens(aliceNode, bobParty, 50)
        val update2TransactionId = transferTokens(bobNode, charlieParty, 50)
        val update3TransactionId = transferTokens(charlieNode, aliceParty, 50)
        val update4TransactionId = transferTokens(aliceNode, bobParty, 50)
        val update5TransactionId = transferTokens(bobNode, charlieParty, 50)
        val update6TransactionId = transferTokens(charlieNode, aliceParty, 50)

        val transactionBackChainBeforeReIssuance = getTransactionBackChain(aliceNode, update6TransactionId)
        val expectedBackChainBeforeReIssuance = setOf(issuanceTransactionId, update1TransactionId,
            update2TransactionId, update3TransactionId, update4TransactionId, update5TransactionId,
            update6TransactionId)
        assertThat(transactionBackChainBeforeReIssuance, `is`(expectedBackChainBeforeReIssuance))

        val tokens = getTokens(aliceNode)
        val tokenIndices = tokens.indices.toList()

        val requestReIssuanceTransactionId = createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            tokens,
            IssueTokenCommand(issuedTokenType, tokenIndices),
            issuerParty
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]

        val reIssueStatesTransactionId = reIssueRequestedStates<FungibleToken>(issuerNode, reIssuanceRequest,
            issuerIsRequiredExitCommandSigner = true)

        val exitTransactionId = redeemTokens(aliceNode, tokens)

        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode, exitTransactionId)

        val unlockReIssuedStatesTransactionId = unlockReIssuedState<FungibleToken>(aliceNode,
            listOf(attachmentSecureHash), MoveTokenCommand(issuedTokenType, tokenIndices, tokenIndices),
            getStateAndRefs<FungibleToken>(aliceNode, encumbered = true),
            getStateAndRefs<ReIssuanceLock<FungibleToken>>(aliceNode, encumbered = true)[0]
        )

        val encumberedStates = getStateAndRefs<FungibleToken>(aliceNode, encumbered = true)
        val unencumberedStates = getStateAndRefs<FungibleToken>(aliceNode, encumbered = false)
        assertThat(encumberedStates, empty())
        assertThat(unencumberedStates, hasSize(`is`(1)))
        assertThat(unencumberedStates[0].state.data, `is`(tokens[0].state.data))

        val transactionBackChainAfterReIssuance = getTransactionBackChain(aliceNode, unlockReIssuedStatesTransactionId)
        val expectedTransactionBackChainAfterReIssuance = setOf(requestReIssuanceTransactionId,
            reIssueStatesTransactionId, unlockReIssuedStatesTransactionId)
        assertThat(transactionBackChainAfterReIssuance, `is`(expectedTransactionBackChainAfterReIssuance))
    }


    @Test
    fun `Re-issue just part of tokens`() {
        initialiseParties()
        val issuanceTransactionId = issueTokens(aliceParty, 50)
        val update1TransactionId = transferTokens(aliceNode, bobParty, 40)
        val update2TransactionId = transferTokens(bobNode, charlieParty, 30)
        val update3TransactionId = transferTokens(charlieNode, aliceParty, 30)
        val update4TransactionId = transferTokens(aliceNode, bobParty, 30)
        val update5TransactionId = transferTokens(bobNode, charlieParty, 30)
        val update6TransactionId = transferTokens(charlieNode, aliceParty, 30)

        val transactionBackChainBeforeReIssuance = getTransactionBackChain(aliceNode, update6TransactionId)
        val expectedBackChainBeforeReIssuance = setOf(issuanceTransactionId, update1TransactionId,
            update2TransactionId, update3TransactionId, update4TransactionId, update5TransactionId,
            update6TransactionId)
        assertThat(transactionBackChainBeforeReIssuance, `is`(expectedBackChainBeforeReIssuance))

        val tokensToReIssue = listOf(getTokens(aliceNode)[1]) // 30 tokens
        val indicesList = listOf(0)

        val requestReIssuanceTransactionId =  createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            tokensToReIssue,
            IssueTokenCommand(issuedTokenType, indicesList),
            issuerParty
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]

        val reIssueStatesTransactionId = reIssueRequestedStates<FungibleToken>(issuerNode, reIssuanceRequest,
            issuerIsRequiredExitCommandSigner = true)

        val exitTransactionId = redeemTokens(aliceNode, tokensToReIssue)

        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode, exitTransactionId)

        val unlockReIssuedStatesTransactionId = unlockReIssuedState<FungibleToken>(aliceNode,
            listOf(attachmentSecureHash), MoveTokenCommand(issuedTokenType, indicesList, indicesList),
            getStateAndRefs<FungibleToken>(aliceNode, encumbered = true),
            getStateAndRefs<ReIssuanceLock<FungibleToken>>(aliceNode, encumbered = true)[0]
        )

        val encumberedStates = getTokens(aliceNode, encumbered = true)
        val unencumberedStates = getTokens(aliceNode, encumbered = false)
        assertThat(encumberedStates, empty())
        assertThat(unencumberedStates, hasSize(`is`(2)))
        assertThat(unencumberedStates.map { it.state.data }, hasItem(tokensToReIssue[0].state.data))

        val transactionBackChainAfterReIssuance = getTransactionBackChain(aliceNode, unlockReIssuedStatesTransactionId)
        val expectedTransactionBackChainAfterReIssuance = setOf(requestReIssuanceTransactionId,
            reIssueStatesTransactionId, unlockReIssuedStatesTransactionId)
        assertThat(transactionBackChainAfterReIssuance, `is`(expectedTransactionBackChainAfterReIssuance))
    }


    @Test
    fun `Re-issued tokens are unencumbered after the original state is deleted`() {
        initialiseParties()
        val issuanceTransactionId = issueTokens(aliceParty, 50)
        val update1TransactionId = transferTokens(aliceNode, bobParty, 40)
        val update2TransactionId = transferTokens(bobNode, charlieParty, 30)
        val update3TransactionId = transferTokens(charlieNode, aliceParty, 30)
        val update4TransactionId = transferTokens(aliceNode, bobParty, 30)
        val update5TransactionId = transferTokens(bobNode, charlieParty, 30)
        val update6TransactionId = transferTokens(charlieNode, aliceParty, 30)

        val transactionBackChainBeforeReIssuance = getTransactionBackChain(aliceNode, update6TransactionId)
        val expectedBackChainBeforeReIssuance = setOf(issuanceTransactionId, update1TransactionId,
            update2TransactionId, update3TransactionId, update4TransactionId, update5TransactionId,
            update6TransactionId)
        assertThat(transactionBackChainBeforeReIssuance, `is`(expectedBackChainBeforeReIssuance))

        val tokensToReIssue = getTokens(aliceNode)
        val tokenIndices = tokensToReIssue.indices.toList()

        val requestReIssuanceTransactionId = createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            tokensToReIssue,
            IssueTokenCommand(issuedTokenType, tokenIndices),
            issuerParty
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]

        val reIssueStatesTransactionId = reIssueRequestedStates<FungibleToken>(issuerNode, reIssuanceRequest,
            issuerIsRequiredExitCommandSigner = true)

        val exitTransactionId = redeemTokens(aliceNode, tokensToReIssue)

        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode, exitTransactionId)

        val unlockReIssuedStatesTransactionId = unlockReIssuedState<FungibleToken>(aliceNode,
            listOf(attachmentSecureHash), MoveTokenCommand(issuedTokenType, tokenIndices, tokenIndices),
            getStateAndRefs<FungibleToken>(aliceNode, encumbered = true),
            getStateAndRefs<ReIssuanceLock<FungibleToken>>(aliceNode, encumbered = true)[0]
        )

        val encumberedStates = getTokens(aliceNode, encumbered = true)
        val unencumberedStates = getTokens(aliceNode, encumbered = false)
        assertThat(encumberedStates, empty())
        assertThat(unencumberedStates, hasSize(`is`(2)))
        assertThat(unencumberedStates.map { it.state.data }, `is`(tokensToReIssue.map { it.state.data }))

        val transactionBackChainAfterReIssuance = getTransactionBackChain(aliceNode, unlockReIssuedStatesTransactionId)
        val expectedTransactionBackChainAfterReIssuance = setOf(requestReIssuanceTransactionId,
            reIssueStatesTransactionId, unlockReIssuedStatesTransactionId)
        assertThat(transactionBackChainAfterReIssuance, `is`(expectedTransactionBackChainAfterReIssuance))
    }

    @Test
    fun `Many exit transactions`() {
        initialiseParties()
        val issuanceTransactionId = issueTokens(aliceParty, 50)
        val update1TransactionId = transferTokens(aliceNode, bobParty, 30)
        val update2TransactionId = transferTokens(bobNode, charlieParty, 30)
        val update3TransactionId = transferTokens(charlieNode, aliceParty, 30)
        val update4TransactionId = transferTokens(aliceNode, bobParty, 30)
        val update5TransactionId = transferTokens(bobNode, charlieParty, 30)
        val update6TransactionId = transferTokens(charlieNode, aliceParty, 30)

        val transactionBackChainBeforeReIssuance = getTransactionBackChain(aliceNode, update6TransactionId)
        val expectedBackChainBeforeReIssuance = setOf(issuanceTransactionId, update1TransactionId,
            update2TransactionId, update3TransactionId, update4TransactionId, update5TransactionId,
            update6TransactionId)
        assertThat(transactionBackChainBeforeReIssuance, `is`(expectedBackChainBeforeReIssuance))

        val tokensToReIssue = getTokens(aliceNode)
        val indicesList = tokensToReIssue.indices.toList()

        val requestReIssuanceTransactionId = createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            tokensToReIssue,
            IssueTokenCommand(issuedTokenType, indicesList),
            issuerParty
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]

        val reIssueStatesTransactionId = reIssueRequestedStates<FungibleToken>(issuerNode, reIssuanceRequest,
            issuerIsRequiredExitCommandSigner = true)

        val exitTransactionIds = listOf(
            redeemTokens(aliceNode, listOf(tokensToReIssue[0])),
            redeemTokens(aliceNode, listOf(tokensToReIssue[1]))
        )

        val attachmentSecureHashes = exitTransactionIds.map {
            uploadDeletedStateAttachment(aliceNode, it)
        }

        val unlockReIssuedStatesTransactionId = unlockReIssuedState<FungibleToken>(aliceNode,
            attachmentSecureHashes, MoveTokenCommand(issuedTokenType, indicesList, indicesList),
            getStateAndRefs<FungibleToken>(aliceNode, encumbered = true),
            getStateAndRefs<ReIssuanceLock<FungibleToken>>(aliceNode, encumbered = true)[0]
        )

        val encumberedStates = getTokens(aliceNode, encumbered = true)
        val unencumberedStates = getTokens(aliceNode, encumbered = false)
        assertThat(encumberedStates, empty())
        assertThat(unencumberedStates, hasSize(`is`(2)))
        assertThat(unencumberedStates.map { it.state.data }, hasItem(tokensToReIssue[0].state.data))

        val transactionBackChainAfterReIssuance = getTransactionBackChain(aliceNode, unlockReIssuedStatesTransactionId)
        val expectedTransactionBackChainAfterReIssuance = setOf(requestReIssuanceTransactionId,
            reIssueStatesTransactionId, unlockReIssuedStatesTransactionId)
        assertThat(transactionBackChainAfterReIssuance, `is`(expectedTransactionBackChainAfterReIssuance))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Only requester can unlock re-issued state`() {
        initialiseParties()
        createDummyStateRequiringAcceptance(aliceParty)

        val dummyStateRequiringAcceptance = getStateAndRefs<DummyStateRequiringAcceptance>(aliceNode)[0]

        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(dummyStateRequiringAcceptance),
            DummyStateRequiringAcceptanceContract.Commands.Create(),
            issuerParty,
            listOf(issuerParty, acceptorParty)
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]

        reIssueRequestedStates<DummyStateRequiringAcceptance>(issuerNode, reIssuanceRequest,
            issuerIsRequiredExitCommandSigner = true)

        val exitTransactionId = deleteDummyStateRequiringAcceptance(aliceNode)

        // issuer creates attachment and tries to unlock state
        val attachmentSecureHash = uploadDeletedStateAttachment(issuerNode, exitTransactionId)

        unlockReIssuedState<DummyStateRequiringAcceptance>(
            issuerNode,
            listOf(attachmentSecureHash),
            DummyStateRequiringAcceptanceContract.Commands.Update(),
            getStateAndRefs<DummyStateRequiringAcceptance>(aliceNode, encumbered = true),
            getStateAndRefs<ReIssuanceLock<DummyStateRequiringAcceptance>>(aliceNode, encumbered = true)[0],
            listOf(aliceParty, issuerParty, acceptorParty)
            )
    }

    @Test
    fun `SimpleDummyState re-issued - accounts on the same host`() {
        initialisePartiesForAccountsOnTheSameHost()
        createSimpleDummyStateForAccount(employeeNode, employeeAliceParty)
        updateSimpleDummyStateForAccount(employeeNode, employeeBobParty)
        updateSimpleDummyStateForAccount(employeeNode, employeeCharlieParty)
        updateSimpleDummyStateForAccount(employeeNode, employeeAliceParty)
        updateSimpleDummyStateForAccount(employeeNode, employeeBobParty)
        updateSimpleDummyStateForAccount(employeeNode, employeeCharlieParty)
        updateSimpleDummyStateForAccount(employeeNode, employeeAliceParty)

        val transactionsBeforeReIssuance = getLedgerTransactions(employeeNode)
        assertThat(transactionsBeforeReIssuance.size, `is`(12)) // including 5 create account transactions

        val simpleDummyState = getStateAndRefs<SimpleDummyState>(employeeNode,
            accountUUID = employeeAliceAccount.identifier.id)[0]
        createReIssuanceRequestAndShareRequiredTransactions(
            employeeNode,
            listOf(simpleDummyState),
            SimpleDummyStateContract.Commands.Create(),
            employeeIssuerParty,
            requester = employeeAliceParty
        )

        val reIssuanceRequest = employeeNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]

        reIssueRequestedStates<SimpleDummyState>(employeeNode, reIssuanceRequest,
            issuerIsRequiredExitCommandSigner = false)

        val exitTransactionId = deleteSimpleDummyStateForAccount(employeeNode)

        val attachmentSecureHash = uploadDeletedStateAttachment(employeeNode, exitTransactionId)

        unlockReIssuedState<SimpleDummyState>(
            employeeNode,
            listOf(attachmentSecureHash),
            SimpleDummyStateContract.Commands.Update(),
            getStateAndRefs<SimpleDummyState>(employeeNode, encumbered = true),
            getStateAndRefs<ReIssuanceLock<SimpleDummyState>>(employeeNode, encumbered = true)[0]
        )

        val encumberedStates = getStateAndRefs<SimpleDummyState>(employeeNode, encumbered = true,
            accountUUID = employeeAliceAccount.identifier.id)
        val unencumberedStates = getStateAndRefs<SimpleDummyState>(employeeNode, encumbered = false,
            accountUUID = employeeAliceAccount.identifier.id)
        assertThat(encumberedStates, empty())
        assertThat(unencumberedStates, hasSize(`is`(1)))
        assertThat(unencumberedStates[0].state.data, `is`(simpleDummyState.state.data))

        updateSimpleDummyStateForAccount(employeeNode, employeeDebbieParty)

        val transactionsAfterReIssuance = getLedgerTransactions(employeeNode)
        assertThat(transactionsAfterReIssuance.size, `is`(17)) // transactions available to all accounts

        val backChainTransactionsIds = getTransactionBackChain(employeeNode, transactionsAfterReIssuance.last().id)
        assertThat(backChainTransactionsIds.size, `is`(4)) // transactions available the given account
        assertThat(transactionsAfterReIssuance.map { it.id }, hasItems(*backChainTransactionsIds.toTypedArray()))
    }

    @Test
    fun `SimpleDummyState re-issued - accounts on different hosts`() {
        initialisePartiesForAccountsOnDifferentHosts()
        createSimpleDummyStateForAccount(issuerNode, employeeAliceParty)
        updateSimpleDummyStateForAccount(aliceNode, employeeBobParty)
        updateSimpleDummyStateForAccount(bobNode, employeeCharlieParty)
        updateSimpleDummyStateForAccount(charlieNode, employeeAliceParty)
        updateSimpleDummyStateForAccount(aliceNode, employeeBobParty)
        updateSimpleDummyStateForAccount(bobNode, employeeCharlieParty)
        updateSimpleDummyStateForAccount(charlieNode, employeeAliceParty)

        val simpleDummyState = getStateAndRefs<SimpleDummyState>(aliceNode, accountUUID = employeeAliceAccount.identifier.id)[0]
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(simpleDummyState),
            SimpleDummyStateContract.Commands.Create(),
            employeeIssuerParty,
            requester = employeeAliceParty
        )

        val reIssuanceRequest = aliceNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<SimpleDummyState>(issuerNode, reIssuanceRequest,
            issuerIsRequiredExitCommandSigner = false)

        val exitTransactionId = deleteSimpleDummyStateForAccount(aliceNode)
        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode, exitTransactionId)

        unlockReIssuedState<SimpleDummyState>(
            aliceNode,
            listOf(attachmentSecureHash),
            SimpleDummyStateContract.Commands.Update(),
            getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = true),
            getStateAndRefs<ReIssuanceLock<SimpleDummyState>>(aliceNode, encumbered = true)[0]
        )

        val encumberedStates = getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = true,
            accountUUID = employeeAliceAccount.identifier.id)
        val unencumberedStates = getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = false,
            accountUUID = employeeAliceAccount.identifier.id)
        assertThat(encumberedStates, empty())
        assertThat(unencumberedStates, hasSize(`is`(1)))
        assertThat(unencumberedStates[0].state.data, `is`(simpleDummyState.state.data))

        updateSimpleDummyStateForAccount(aliceNode, employeeDebbieParty)

        val transactionsAfterReIssuance = getLedgerTransactions(debbieNode)
        assertThat(transactionsAfterReIssuance.size, `is`(9)) // transactions available to all accounts

        val backChainTransactionsIds = getTransactionBackChain(debbieNode, transactionsAfterReIssuance.last().id)
        assertThat(backChainTransactionsIds.size, `is`(4)) // transactions available the given account
        assertThat(transactionsAfterReIssuance.map { it.id }, hasItems(*backChainTransactionsIds.toTypedArray()))
    }

    @Test(expected = TransactionVerificationException::class)
    fun `Attached transaction needs to be notarised`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)

        val simpleDummyState = getStateAndRefs<SimpleDummyState>(aliceNode)[0]
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(simpleDummyState),
            SimpleDummyStateContract.Commands.Create(),
            issuerParty
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<SimpleDummyState>(issuerNode, reIssuanceRequest,
            issuerIsRequiredExitCommandSigner = true)

        val originalStateAndRef = getStateAndRefs<SimpleDummyState>(aliceNode)[0]
        val transactionBuilder = TransactionBuilder(notary = notaryParty)
        transactionBuilder.addInputState(originalStateAndRef)
        transactionBuilder.addCommand(SimpleDummyStateContract.Commands.Delete(), listOf(aliceParty.owningKey))
        var signedTransaction = aliceNode.services.signInitialTransaction(transactionBuilder)

        val transactionByteArray = convertSignedTransactionToByteArray(signedTransaction)
        val attachmentSecureHash = aliceNode.services.attachments.importAttachment(
            transactionByteArray.inputStream(), aliceNode.info.singleIdentity().toString(), null)

        unlockReIssuedState<SimpleDummyState>(
            aliceNode, 
            listOf(attachmentSecureHash),
            SimpleDummyStateContract.Commands.Update(),
            getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = true),
            getStateAndRefs<ReIssuanceLock<SimpleDummyState>>(aliceNode, encumbered = true)[0]
        )
    }

    @Test(expected = TransactionVerificationException::class)
    fun `Re-issued SimpleDummyState cannot be unlocked if the original state is consumed`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)

        val simpleDummyState = getStateAndRefs<SimpleDummyState>(aliceNode)[0]
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(simpleDummyState),
            SimpleDummyStateContract.Commands.Create(),
            issuerParty
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<SimpleDummyState>(issuerNode, reIssuanceRequest,
            issuerIsRequiredExitCommandSigner = true)

        val updateTransactionId = updateSimpleDummyState(aliceNode, bobParty)
        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode, updateTransactionId)

        unlockReIssuedState<SimpleDummyState>(
            aliceNode,
            listOf(attachmentSecureHash),
            SimpleDummyStateContract.Commands.Update(),
            getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = true),
            getStateAndRefs<ReIssuanceLock<SimpleDummyState>>(aliceNode, encumbered = true)[0]
        )
    }


    @Test(expected = TransactionVerificationException::class)
    fun `Re-issued DummyStateRequiringAcceptance cannot be unlocked if the original state is consumed`() {
        initialiseParties()
        createDummyStateRequiringAcceptance(aliceParty)

        val dummyStateRequiringAcceptance = getStateAndRefs<DummyStateRequiringAcceptance>(aliceNode)[0]
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(dummyStateRequiringAcceptance),
            DummyStateRequiringAcceptanceContract.Commands.Create(),
            issuerParty,
            listOf(issuerParty, acceptorParty)
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]

        reIssueRequestedStates<DummyStateRequiringAcceptance>(issuerNode, reIssuanceRequest,
            issuerIsRequiredExitCommandSigner = true)

        val updateTransactionId = updateDummyStateRequiringAcceptance(aliceNode, bobParty)

        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode, updateTransactionId)

        unlockReIssuedState<DummyStateRequiringAcceptance>(
            aliceNode, 
            listOf(attachmentSecureHash),
            DummyStateRequiringAcceptanceContract.Commands.Update(),
            getStateAndRefs<DummyStateRequiringAcceptance>(aliceNode, encumbered = true),
            getStateAndRefs<ReIssuanceLock<DummyStateRequiringAcceptance>>(aliceNode, encumbered = true)[0],
            listOf(aliceParty, issuerParty, acceptorParty)
        )
    }

    @Test(expected = TransactionVerificationException::class)
    fun `Invalid equals method doesn't allow cheating by attaching another state exit as reference is checked`() {
        initialiseParties()
        createDummyStateWithInvalidEqualsMethod(aliceParty, 10)

        val dummyStateWithInvalidEqualsMethod = getStateAndRefs<DummyStateWithInvalidEqualsMethod>(aliceNode)[0]
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(dummyStateWithInvalidEqualsMethod),
            DummyStateWithInvalidEqualsMethodContract.Commands.Create(),
            issuerParty
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<DummyStateWithInvalidEqualsMethod>(issuerNode, reIssuanceRequest,
            issuerIsRequiredExitCommandSigner = true)


        createDummyStateWithInvalidEqualsMethod(aliceParty, 5)
        val stateAndRefToDelete = getStateAndRefs<DummyStateWithInvalidEqualsMethod>(aliceNode)

        val exitTransactionId = deleteDummyStateWithInvalidEqualsMethod(aliceNode, stateAndRefToDelete.last())
        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode, exitTransactionId)

        unlockReIssuedState<DummyStateWithInvalidEqualsMethod>(
            aliceNode,
            listOf(attachmentSecureHash),
            DummyStateWithInvalidEqualsMethodContract.Commands.Update(),
            getStateAndRefs<DummyStateWithInvalidEqualsMethod>(aliceNode, encumbered = true),
            getStateAndRefs<ReIssuanceLock<DummyStateWithInvalidEqualsMethod>>(aliceNode, encumbered = true)[0]
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `State can't be re-issued again after the re-issued state has been unlocked`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)

        val simpleDummyState = getStateAndRefs<SimpleDummyState>(aliceNode)[0]
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(simpleDummyState),
            SimpleDummyStateContract.Commands.Create(),
            issuerParty
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]

        reIssueRequestedStates<SimpleDummyState>(issuerNode, reIssuanceRequest,
            issuerIsRequiredExitCommandSigner = false)

        val exitTransactionId = deleteSimpleDummyState(aliceNode)

        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode, exitTransactionId)

        unlockReIssuedState<SimpleDummyState>(
            aliceNode,
            listOf(attachmentSecureHash),
            SimpleDummyStateContract.Commands.Update(),
            getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = true),
            getStateAndRefs<ReIssuanceLock<SimpleDummyState>>(aliceNode, encumbered = true)[0]
        )

        createReIssuanceRequest<SimpleDummyState>(
            aliceNode,
            listOf(simpleDummyState.ref),
            SimpleDummyStateContract.Commands.Create(),
            issuerParty
        )
        val reIssuanceRequest2 = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<SimpleDummyState>(issuerNode, reIssuanceRequest2,
            issuerIsRequiredExitCommandSigner = true)

    }

    @Test(expected = IllegalArgumentException::class)
    fun `Requester can't pretend to be a notary to forge exit transaction`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)

        val simpleDummyState = getStateAndRefs<SimpleDummyState>(aliceNode)[0]
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(simpleDummyState),
            SimpleDummyStateContract.Commands.Create(),
            issuerParty
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]

        reIssueRequestedStates<SimpleDummyState>(issuerNode, reIssuanceRequest,
            issuerIsRequiredExitCommandSigner = true)

        val transactionBuilder = TransactionBuilder(notary = aliceParty)
        transactionBuilder.addInputState(simpleDummyState)
        transactionBuilder.addCommand(DummyStateRequiringAcceptanceContract.Commands.Update(), listOf(aliceParty.owningKey))
        val signedTransaction = aliceNode.services.signInitialTransaction(transactionBuilder)
    }

    @Test(expected = TransactionVerificationException::class)
    fun `Requester can't forge signed transaction by creating another class derived from TraversableTransaction`() {
        initialiseParties()

        createSimpleDummyState(aliceParty)
        val exitTransactionId = deleteSimpleDummyState(aliceNode)
        val signedDeleteTransaction = getSignedTransactions(aliceNode).last()

        createSimpleDummyState(aliceParty)

        val simpleDummyState = getStateAndRefs<SimpleDummyState>(aliceNode)[0]
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(simpleDummyState),
            SimpleDummyStateContract.Commands.Create(),
            issuerParty
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]

        reIssueRequestedStates<SimpleDummyState>(issuerNode, reIssuanceRequest,
            issuerIsRequiredExitCommandSigner = true)

        val transactionBuilder = TransactionBuilder(notary = notaryParty)
        transactionBuilder.addInputState(simpleDummyState)
        transactionBuilder.addCommand(DummyStateRequiringAcceptanceContract.Commands.Update(), listOf(aliceParty.owningKey))
        val initiallySignedTransaction = aliceNode.services.signInitialTransaction(transactionBuilder)

        val wireTransaction = initiallySignedTransaction.coreTransaction as WireTransaction
        val testWireTransaction = TestWireTransaction(wireTransaction.componentGroups, wireTransaction.privacySalt, id=signedDeleteTransaction.id)

        val forgedSignedTransaction = SignedTransaction(testWireTransaction, signedDeleteTransaction.sigs)

        val signedTransactionByteArray = convertSignedTransactionToByteArray(forgedSignedTransaction)
        val attachmentSecureHash = aliceNode.services.attachments.importAttachment(signedTransactionByteArray.inputStream(), aliceParty.toString(), null)

        unlockReIssuedState<SimpleDummyState>(
            aliceNode,
            listOf(attachmentSecureHash),
            SimpleDummyStateContract.Commands.Update(),
            getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = true),
            getStateAndRefs<ReIssuanceLock<SimpleDummyState>>(aliceNode, encumbered = true)[0]
        )
    }
}

class TestWireTransaction(componentGroups: List<ComponentGroup>,
                          val privacySalt: PrivacySalt = PrivacySalt(),
                          override val id: SecureHash
): TraversableTransaction(componentGroups)
