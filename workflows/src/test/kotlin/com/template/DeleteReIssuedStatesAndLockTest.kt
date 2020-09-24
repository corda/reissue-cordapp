package com.template

import com.r3.corda.lib.tokens.contracts.commands.IssueTokenCommand
import com.r3.corda.lib.tokens.contracts.commands.RedeemTokenCommand
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.template.contracts.example.DummyStateRequiringAcceptanceContract
import com.template.contracts.example.DummyStateRequiringAllParticipantsSignaturesContract
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import com.template.contracts.example.SimpleDummyStateContract
import com.template.states.ReIssuanceLock
import com.template.states.ReIssuanceRequest
import com.template.states.example.DummyStateRequiringAcceptance
import com.template.states.example.DummyStateRequiringAllParticipantsSignatures
import com.template.states.example.SimpleDummyState
import net.corda.core.node.services.queryBy
import org.junit.Test

class DeleteReIssuedStatesAndLockTest: AbstractFlowTest() {

    @Test
    fun `Re-issued SimpleDummyState and corresponding ReIssuanceLock are deleted`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)

        val simpleDummyStatesToReIssue = getStateAndRefs<SimpleDummyState>(aliceNode) // there is just 1
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            simpleDummyStatesToReIssue,
            SimpleDummyStateContract.Commands.Create(),
            issuerParty
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<SimpleDummyState>(issuerNode, reIssuanceRequest)

        val reIssuedSimpleDummyStates = getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = true)
        val lockState = getStateAndRefs<ReIssuanceLock<SimpleDummyState>>(aliceNode)[0]
        deleteReIssuedStatesAndLock(
            aliceNode,
            lockState,
            reIssuedSimpleDummyStates,
            SimpleDummyStateContract.Commands.Delete()
        )

        val encumberedStates = getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = true)
        val unencumberedStates = getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = false)
        assertThat(encumberedStates, empty())
        assertThat(unencumberedStates, hasSize(`is`(1)))
        assertThat(unencumberedStates[0], `is`(simpleDummyStatesToReIssue[0]))
    }

    @Test
    fun `Re-issued DummyStateRequiringAcceptance is unencumbered after the original state are deleted`() {
        initialiseParties()
        createDummyStateRequiringAcceptance(aliceParty)

        val dummyStatesRequiringAcceptanceToReIssue = getStateAndRefs<DummyStateRequiringAcceptance>(aliceNode)
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            dummyStatesRequiringAcceptanceToReIssue,
            DummyStateRequiringAcceptanceContract.Commands.Create(),
            issuerParty,
            listOf(issuerParty, acceptorParty)
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<DummyStateRequiringAcceptance>(issuerNode, reIssuanceRequest)

        val reIssuedDummyStatesRequiringAcceptance = getStateAndRefs<DummyStateRequiringAcceptance>(aliceNode, encumbered = true)
        val lockState = getStateAndRefs<ReIssuanceLock<DummyStateRequiringAcceptance>>(aliceNode)[0]
        deleteReIssuedStatesAndLock(
            aliceNode,
            lockState,
            reIssuedDummyStatesRequiringAcceptance,
            DummyStateRequiringAcceptanceContract.Commands.Delete(),
            listOf(issuerParty, aliceParty, acceptorParty)
        )

        val encumberedStates = getStateAndRefs<DummyStateRequiringAcceptance>(aliceNode, encumbered = true)
        val unencumberedStates = getStateAndRefs<DummyStateRequiringAcceptance>(aliceNode, encumbered = false)
        assertThat(encumberedStates, empty())
        assertThat(unencumberedStates, hasSize(`is`(1)))
        assertThat(unencumberedStates[0], `is`(dummyStatesRequiringAcceptanceToReIssue[0]))
    }

    @Test
    fun `Re-issued DummyStateRequiringAllParticipantsSignatures and corresponding ReIssuanceLock are deleted`() {
        initialiseParties()
        createDummyStateRequiringAllParticipantsSignatures(aliceParty)

        val dummyStatesRequiringAllParticipantsSignaturesToReIssue =
            getStateAndRefs<DummyStateRequiringAllParticipantsSignatures>(aliceNode)

        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            dummyStatesRequiringAllParticipantsSignaturesToReIssue,
            DummyStateRequiringAllParticipantsSignaturesContract.Commands.Create(),
            issuerParty,
            listOf(aliceParty, issuerParty, acceptorParty)
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<DummyStateRequiringAllParticipantsSignatures>(issuerNode, reIssuanceRequest)

        val reIssuedDummyStatesRequiringAcceptance = getStateAndRefs<DummyStateRequiringAllParticipantsSignatures>(
            aliceNode, encumbered = true)
        val lockState = getStateAndRefs<ReIssuanceLock<DummyStateRequiringAllParticipantsSignatures>>(aliceNode)[0]
        deleteReIssuedStatesAndLock(
            aliceNode,
            lockState,
            reIssuedDummyStatesRequiringAcceptance,
            DummyStateRequiringAllParticipantsSignaturesContract.Commands.Delete(),
            listOf(issuerParty, aliceParty, acceptorParty)
        )

        val encumberedStates = getStateAndRefs<DummyStateRequiringAllParticipantsSignatures>(
            aliceNode, encumbered = true)
        val unencumberedStates = getStateAndRefs<DummyStateRequiringAllParticipantsSignatures>(
            aliceNode, encumbered = false)
        assertThat(encumberedStates, empty())
        assertThat(unencumberedStates, hasSize(`is`(1)))
        assertThat(unencumberedStates[0], `is`(dummyStatesRequiringAllParticipantsSignaturesToReIssue[0]))
    }

    @Test
    fun `Re-issued tokens and corresponding ReIssuanceLock are deleted`() {
        initialiseParties()
        issueTokens(aliceParty, 50)

        val tokensToReIssue = getTokens(aliceNode)
        val tokenIndices = tokensToReIssue.indices.toList()

        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            tokensToReIssue,
            IssueTokenCommand(issuedTokenType, tokenIndices),
            issuerParty
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<FungibleToken>(issuerNode, reIssuanceRequest)

        val reIssuedTokens = getStateAndRefs<FungibleToken>(aliceNode, encumbered = true)
        val lockState = getStateAndRefs<ReIssuanceLock<FungibleToken>>(aliceNode)[0]
        deleteReIssuedStatesAndLock(
            aliceNode,
            lockState,
            reIssuedTokens,
            RedeemTokenCommand(issuedTokenType, tokenIndices, listOf())
        )

        val encumberedStates = getStateAndRefs<FungibleToken>(aliceNode, encumbered = true)
        val unencumberedStates = getStateAndRefs<FungibleToken>(aliceNode, encumbered = false)
        assertThat(encumberedStates, empty())
        assertThat(unencumberedStates, hasSize(`is`(1)))
        assertThat(unencumberedStates[0], `is`(tokensToReIssue[0]))
    }

    @Test
    fun `Re-issued tokens and corresponding ReIssuanceLock are deleted when only part of tokens had been re-issued`() {
        initialiseParties()
        issueTokens(aliceParty, 50)
        transferTokens(aliceNode, bobParty, 30)
        transferTokens(bobNode, aliceParty, 30)

        val tokens = getTokens(aliceNode)
        val tokensToReIssue = listOf(tokens[1]) // 30 tokens
        val tokenIndices = listOf(0)

        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            tokensToReIssue,
            IssueTokenCommand(issuedTokenType, tokenIndices),
            issuerParty
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<FungibleToken>(issuerNode, reIssuanceRequest)

        val reIssuedTokens = getStateAndRefs<FungibleToken>(aliceNode, encumbered = true)
        val lockState = getStateAndRefs<ReIssuanceLock<FungibleToken>>(aliceNode)[0]
        deleteReIssuedStatesAndLock(
            aliceNode,
            lockState,
            reIssuedTokens,
            RedeemTokenCommand(issuedTokenType, tokenIndices, listOf())
        )

        val encumberedStates = getStateAndRefs<FungibleToken>(aliceNode, encumbered = true)
        val unencumberedStates = getStateAndRefs<FungibleToken>(aliceNode, encumbered = false)
        assertThat(encumberedStates, empty())
        assertThat(unencumberedStates, hasSize(`is`(2)))
        assertThat(unencumberedStates, `is`(tokens))
    }

    @Test
    fun `Re-issued tokens and corresponding ReIssuanceLock are deleted when many token inputs were re-issued`() {
        initialiseParties()
        issueTokens(aliceParty, 50)
        transferTokens(aliceNode, bobParty, 30)
        transferTokens(bobNode, aliceParty, 30)

        val tokensToReIssue = getTokens(aliceNode)
        val tokenIndices = tokensToReIssue.indices.toList()

        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            tokensToReIssue,
            IssueTokenCommand(issuedTokenType, tokenIndices),
            issuerParty
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<FungibleToken>(issuerNode, reIssuanceRequest)

        val reIssuedTokens = getStateAndRefs<FungibleToken>(aliceNode, encumbered = true)
        val lockState = getStateAndRefs<ReIssuanceLock<FungibleToken>>(aliceNode)[0]
        deleteReIssuedStatesAndLock(
            aliceNode,
            lockState,
            reIssuedTokens,
            RedeemTokenCommand(issuedTokenType, tokenIndices, listOf())
        )

        val encumberedStates = getStateAndRefs<FungibleToken>(aliceNode, encumbered = true)
        val unencumberedStates = getStateAndRefs<FungibleToken>(aliceNode, encumbered = false)
        assertThat(encumberedStates, empty())
        assertThat(unencumberedStates, hasSize(`is`(2)))
        assertThat(unencumberedStates, `is`(tokensToReIssue))
    }

    @Test
    fun `Re-issued SimpleDummyState and corresponding ReIssuanceLock are deleted - accounts on the same host`() {
        initialisePartiesForAccountsOnTheSameHost()
        createSimpleDummyStateForAccount(employeeNode, employeeAliceParty)

        val simpleDummyStatesToReIssue = getStateAndRefs<SimpleDummyState>(employeeNode,
            accountUUID = employeeAliceAccount.identifier.id)
        createReIssuanceRequestAndShareRequiredTransactions(
            employeeNode,
            simpleDummyStatesToReIssue,
            SimpleDummyStateContract.Commands.Create(),
            employeeIssuerParty,
            requester = employeeAliceParty
        )

        val reIssuanceRequest = employeeNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<SimpleDummyState>(employeeNode, reIssuanceRequest)

        val reIssuedSimpleDummyStates = getStateAndRefs<SimpleDummyState>(employeeNode, encumbered = true,
            accountUUID = employeeAliceAccount.identifier.id)
        val lockState = getStateAndRefs<ReIssuanceLock<SimpleDummyState>>(employeeNode,
            accountUUID = employeeAliceAccount.identifier.id)[0]
        deleteReIssuedStatesAndLock(
            employeeNode,
            lockState,
            reIssuedSimpleDummyStates,
            SimpleDummyStateContract.Commands.Delete()
        )

        val encumberedStates = getStateAndRefs<SimpleDummyState>(employeeNode, encumbered = true,
            accountUUID = employeeAliceAccount.identifier.id)
        val unencumberedStates = getStateAndRefs<SimpleDummyState>(employeeNode, encumbered = false,
            accountUUID = employeeAliceAccount.identifier.id)
        assertThat(encumberedStates, empty())
        assertThat(unencumberedStates, hasSize(`is`(1)))
        assertThat(unencumberedStates[0], `is`(simpleDummyStatesToReIssue[0]))
    }

    @Test
    fun `Re-issued SimpleDummyState and corresponding ReIssuanceLock are deleted - accounts on different hosts`() {
        initialisePartiesForAccountsOnDifferentHosts()
        createSimpleDummyStateForAccount(issuerNode, employeeAliceParty)

        val simpleDummyStatesToReIssue = getStateAndRefs<SimpleDummyState>(aliceNode,
            accountUUID = employeeAliceAccount.identifier.id)
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            simpleDummyStatesToReIssue,
            SimpleDummyStateContract.Commands.Create(),
            employeeIssuerParty,
            requester = employeeAliceParty
        )

        val reIssuanceRequest = aliceNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<SimpleDummyState>(issuerNode, reIssuanceRequest)

        val reIssuedSimpleDummyStates = getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = true,
            accountUUID = employeeAliceAccount.identifier.id)
        val lockState = getStateAndRefs<ReIssuanceLock<SimpleDummyState>>(aliceNode,
            accountUUID = employeeAliceAccount.identifier.id)[0]
        deleteReIssuedStatesAndLock(
            aliceNode,
            lockState,
            reIssuedSimpleDummyStates,
            SimpleDummyStateContract.Commands.Delete()
        )

        val encumberedStates = getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = true,
            accountUUID = employeeAliceAccount.identifier.id)
        val unencumberedStates = getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = false,
            accountUUID = employeeAliceAccount.identifier.id)
        assertThat(encumberedStates, empty())
        assertThat(unencumberedStates, hasSize(`is`(1)))
        assertThat(unencumberedStates[0], `is`(simpleDummyStatesToReIssue[0]))
    }

}
