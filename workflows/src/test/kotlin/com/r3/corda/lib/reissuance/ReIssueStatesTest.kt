package com.r3.corda.lib.reissuance

import com.r3.corda.lib.reissuance.dummy_contracts.DummyStateRequiringAcceptanceContract
import com.r3.corda.lib.reissuance.dummy_contracts.DummyStateRequiringAllParticipantsSignaturesContract
import com.r3.corda.lib.reissuance.dummy_contracts.SimpleDummyStateContract
import com.r3.corda.lib.reissuance.states.ReIssuanceLock
import com.r3.corda.lib.reissuance.states.ReIssuanceRequest
import com.r3.corda.lib.reissuance.dummy_states.DummyStateRequiringAcceptance
import com.r3.corda.lib.reissuance.dummy_states.DummyStateRequiringAllParticipantsSignatures
import com.r3.corda.lib.reissuance.dummy_states.SimpleDummyState
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import com.r3.corda.lib.tokens.contracts.commands.IssueTokenCommand
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.identity.AbstractParty
import net.corda.core.node.services.queryBy
import org.junit.Test

class ReIssueStatesTest: AbstractFlowTest() {

    @Test
    fun `SimpleDummyState is re-issued`() {
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

        val encumberedStates = getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = true)
        val unencumberedStates = getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = false)
        val lockStates = getStateAndRefs<ReIssuanceLock<SimpleDummyState>>(aliceNode, encumbered = true)
        assertThat(encumberedStates, hasSize(`is`(1)))
        assertThat(unencumberedStates, hasSize(`is`(1)))
        assertThat(lockStates, hasSize(`is`(1)))
        assertThat(encumberedStates.map { it.state.data }, `is`(unencumberedStates.map { it.state.data }))
        assertThat(lockStates[0].state.data.issuer, `is`(issuerParty as AbstractParty))
        assertThat(lockStates[0].state.data.requester, `is`(aliceParty as AbstractParty))
        assertThat(lockStates[0].state.data.originalStates, `is`(unencumberedStates))
    }

    @Test
    fun `DummyStateRequiringAcceptance is re-issued`() {
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
            issuerIsRequiredExitCommandSigner = false)

        val encumberedStates = getStateAndRefs<DummyStateRequiringAcceptance>(aliceNode, encumbered = true)
        val unencumberedStates = getStateAndRefs<DummyStateRequiringAcceptance>(aliceNode, encumbered = false)
        val lockStates = getStateAndRefs<ReIssuanceLock<DummyStateRequiringAcceptance>>(aliceNode, encumbered = true)
        assertThat(encumberedStates, hasSize(`is`(1)))
        assertThat(unencumberedStates, hasSize(`is`(1)))
        assertThat(lockStates, hasSize(`is`(1)))
        assertThat(encumberedStates.map { it.state.data }, `is`(unencumberedStates.map { it.state.data }))
        assertThat(lockStates[0].state.data.issuer, `is`(issuerParty as AbstractParty))
        assertThat(lockStates[0].state.data.requester, `is`(aliceParty as AbstractParty))
        assertThat(lockStates[0].state.data.originalStates, `is`(unencumberedStates))
    }

    @Test
    fun `DummyStateRequiringAllParticipantsSignatures is re-issued`() {
        initialiseParties()
        createDummyStateRequiringAllParticipantsSignatures(aliceParty)

        val dummyStateRequiringAllParticipantsSignatures = getStateAndRefs<DummyStateRequiringAllParticipantsSignatures>(aliceNode)[0]
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(dummyStateRequiringAllParticipantsSignatures),
            DummyStateRequiringAllParticipantsSignaturesContract.Commands.Create(),
            issuerParty,
            listOf(aliceParty, issuerParty, acceptorParty)
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<DummyStateRequiringAllParticipantsSignatures>(issuerNode, reIssuanceRequest,
            issuerIsRequiredExitCommandSigner = true)

        val encumberedStates = getStateAndRefs<DummyStateRequiringAllParticipantsSignatures>(aliceNode, encumbered = true)
        val unencumberedStates = getStateAndRefs<DummyStateRequiringAllParticipantsSignatures>(aliceNode, encumbered = false)
        val lockStates = getStateAndRefs<ReIssuanceLock<DummyStateRequiringAllParticipantsSignatures>>(aliceNode, encumbered = true)
        assertThat(encumberedStates, hasSize(`is`(1)))
        assertThat(unencumberedStates, hasSize(`is`(1)))
        assertThat(lockStates, hasSize(`is`(1)))
        assertThat(encumberedStates.map { it.state.data }, `is`(unencumberedStates.map { it.state.data }))
        assertThat(lockStates[0].state.data.issuer, `is`(issuerParty as AbstractParty))
        assertThat(lockStates[0].state.data.requester, `is`(aliceParty as AbstractParty))
        assertThat(lockStates[0].state.data.originalStates, `is`(unencumberedStates))
    }

    @Test
    fun `Token is re-issued`() {
        initialiseParties()
        issueTokens(aliceParty, 50)

        val tokens = getTokens(aliceNode)

        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            tokens,
            IssueTokenCommand(issuedTokenType, tokens.indices.toList()),
            issuerParty
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<FungibleToken>(issuerNode, reIssuanceRequest,
            issuerIsRequiredExitCommandSigner = true)

        val encumberedStates = getStateAndRefs<FungibleToken>(aliceNode, encumbered = true)
        val unencumberedStates = getStateAndRefs<FungibleToken>(aliceNode, encumbered = false)
        val lockStates = getStateAndRefs<ReIssuanceLock<FungibleToken>>(aliceNode, encumbered = true)
        assertThat(encumberedStates, hasSize(`is`(1)))
        assertThat(unencumberedStates, hasSize(`is`(1)))
        assertThat(lockStates, hasSize(`is`(1)))
        assertThat(encumberedStates.map { it.state.data }, `is`(unencumberedStates.map { it.state.data }))
        assertThat(lockStates[0].state.data.issuer, `is`(issuerParty as AbstractParty))
        assertThat(lockStates[0].state.data.requester, `is`(aliceParty as AbstractParty))
        assertThat(lockStates[0].state.data.originalStates, `is`(unencumberedStates))
    }

    @Test
    fun `Tokens are re-issued`() {
        initialiseParties()
        issueTokens(aliceParty, 25)
        issueTokens(aliceParty, 25)

        val tokens = getTokens(aliceNode)

        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            tokens,
            IssueTokenCommand(issuedTokenType, tokens.indices.toList()),
            issuerParty
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<FungibleToken>(issuerNode, reIssuanceRequest,
            issuerIsRequiredExitCommandSigner = true)

        val encumberedStates = getStateAndRefs<FungibleToken>(aliceNode, encumbered = true)
        val unencumberedStates = getStateAndRefs<FungibleToken>(aliceNode, encumbered = false)
        val lockStates = getStateAndRefs<ReIssuanceLock<FungibleToken>>(aliceNode, encumbered = true)
        assertThat(encumberedStates, hasSize(`is`(2)))
        assertThat(unencumberedStates, hasSize(`is`(2)))
        assertThat(lockStates, hasSize(`is`(1)))
        assertThat(encumberedStates.map { it.state.data }, `is`(unencumberedStates.map { it.state.data }))
        assertThat(lockStates[0].state.data.issuer, `is`(issuerParty as AbstractParty))
        assertThat(lockStates[0].state.data.requester, `is`(aliceParty as AbstractParty))
        assertThat(lockStates[0].state.data.originalStates, `is`(unencumberedStates))
    }

    @Test
    fun `SimpleDummyState re-issued - accounts on the same host`() {
        initialisePartiesForAccountsOnTheSameHost()
        createSimpleDummyStateForAccount(employeeNode, employeeAliceParty)

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
            issuerIsRequiredExitCommandSigner = true)

        val encumberedStates = getStateAndRefs<SimpleDummyState>(employeeNode, encumbered = true,
            accountUUID = employeeAliceAccount.identifier.id)
        val unencumberedStates = getStateAndRefs<SimpleDummyState>(employeeNode, encumbered = false,
            accountUUID = employeeAliceAccount.identifier.id)
        val lockStates = getStateAndRefs<ReIssuanceLock<SimpleDummyState>>(employeeNode, encumbered = true,
            accountUUID = employeeAliceAccount.identifier.id)
        assertThat(encumberedStates, hasSize(`is`(1)))
        assertThat(unencumberedStates, hasSize(`is`(1)))
        assertThat(lockStates, hasSize(`is`(1)))
        assertThat(encumberedStates.map { it.state.data }, `is`(unencumberedStates.map { it.state.data }))
        assertThat(lockStates[0].state.data.issuer, `is`(employeeIssuerParty))
        assertThat(lockStates[0].state.data.requester, `is`(employeeAliceParty))
        assertThat(lockStates[0].state.data.originalStates, `is`(unencumberedStates))
    }

    @Test
    fun `SimpleDummyState re-issued - accounts on different hosts`() {
        initialisePartiesForAccountsOnDifferentHosts()
        createSimpleDummyStateForAccount(issuerNode, employeeAliceParty)

        val simpleDummyState = getStateAndRefs<SimpleDummyState>(aliceNode)[0]
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

        val encumberedStates = getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = true,
            accountUUID = employeeAliceAccount.identifier.id)
        val unencumberedStates = getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = false,
            accountUUID = employeeAliceAccount.identifier.id)
        val lockStates = getStateAndRefs<ReIssuanceLock<SimpleDummyState>>(aliceNode, encumbered = true)
        assertThat(encumberedStates, hasSize(`is`(1)))
        assertThat(unencumberedStates, hasSize(`is`(1)))
        assertThat(lockStates, hasSize(`is`(1)))
        assertThat(encumberedStates.map { it.state.data }, `is`(unencumberedStates.map { it.state.data }))
        assertThat(lockStates[0].state.data.issuer, `is`(employeeIssuerParty))
        assertThat(lockStates[0].state.data.requester, `is`(employeeAliceParty))
        assertThat(lockStates[0].state.data.originalStates, `is`(unencumberedStates))
    }

    @Test(expected = IllegalArgumentException::class) // issues find state by reference
    fun `State cannot be re-issued if issuer cannot access it`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)

        val simpleDummyStateRef = getStateAndRefs<SimpleDummyState>(aliceNode)[0].ref
        createReIssuanceRequest<SimpleDummyState>( // don't share required transactions
            aliceNode,
            listOf(simpleDummyStateRef),
            SimpleDummyStateContract.Commands.Create(),
            issuerParty
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<SimpleDummyState>(issuerNode, reIssuanceRequest,
            issuerIsRequiredExitCommandSigner = false)
    }

    @Test // issuer doesn't have an information about state being consumed
    fun `Consumed state is re-issued when issuer is not a participant`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)

        val simpleDummyState = getStateAndRefs<SimpleDummyState>(aliceNode)[0]
        updateSimpleDummyState(aliceNode, bobParty)

        createReIssuanceRequest<SimpleDummyState>(
            aliceNode,
            listOf(simpleDummyState.ref),
            SimpleDummyStateContract.Commands.Create(),
            issuerParty
        )
        shareTransaction(aliceNode, issuerParty, simpleDummyState.ref.txhash)

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<SimpleDummyState>(issuerNode, reIssuanceRequest,
            issuerIsRequiredExitCommandSigner = false)

        val encumberedStates = getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = true)
        val unencumberedStates = getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = false)
        val lockStates = getStateAndRefs<ReIssuanceLock<SimpleDummyState>>(aliceNode, encumbered = true)
        assertThat(encumberedStates, hasSize(`is`(1)))
        assertThat(unencumberedStates, empty())
        assertThat(lockStates, hasSize(`is`(1)))
        assertThat(lockStates[0].state.data.issuer, `is`(issuerParty as AbstractParty))
        assertThat(lockStates[0].state.data.requester, `is`(aliceParty as AbstractParty))
        assertThat(lockStates[0].state.data.originalStates.map { it.state.data },
            `is`(encumberedStates.map { it.state.data }))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Consumed state isn't re-issued when issuer is a participant`() {
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

        updateDummyStateRequiringAcceptance(aliceNode, bobParty)

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<DummyStateRequiringAcceptance>(issuerNode, reIssuanceRequest,
            issuerIsRequiredExitCommandSigner = false)
    }

    @Test
    fun `Another party can re-issue SimpleDummyState because it doesn't store issuer information`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)

        val simpleDummyState = getStateAndRefs<SimpleDummyState>(aliceNode)[0]
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(simpleDummyState),
            SimpleDummyStateContract.Commands.Create(),
            bobParty
        )

        val reIssuanceRequest = bobNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<SimpleDummyState>(bobNode, reIssuanceRequest,
            issuerIsRequiredExitCommandSigner = false)

        val encumberedStates = getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = true)
        val unencumberedStates = getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = false)
        val lockStates = getStateAndRefs<ReIssuanceLock<SimpleDummyState>>(aliceNode, encumbered = true)
        assertThat(encumberedStates, hasSize(`is`(1)))
        assertThat(unencumberedStates, hasSize(`is`(1)))
        assertThat(lockStates, hasSize(`is`(1)))
        assertThat(encumberedStates.map { it.state.data }, `is`(unencumberedStates.map { it.state.data }))
        assertThat(lockStates[0].state.data.issuer, `is`(bobParty as AbstractParty))
        assertThat(lockStates[0].state.data.requester, `is`(aliceParty as AbstractParty))
        assertThat(lockStates[0].state.data.originalStates, `is`(unencumberedStates))
    }

    @Test(expected = TransactionVerificationException::class)
    fun `Another party can't re-issue DummyStateRequiringAcceptance as issuer is a participant`() {
        initialiseParties()
        createDummyStateRequiringAcceptance(aliceParty)

        val dummyStateRequiringAcceptance = getStateAndRefs<DummyStateRequiringAcceptance>(aliceNode)[0]
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(dummyStateRequiringAcceptance),
            DummyStateRequiringAcceptanceContract.Commands.Create(),
            bobParty,
            listOf(bobParty, acceptorParty)
        )

        val reIssuanceRequest = bobNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<DummyStateRequiringAcceptance>(bobNode, reIssuanceRequest,
            issuerIsRequiredExitCommandSigner = false)
    }

    @Test(expected = TransactionVerificationException::class)
    fun `Another party can't re-issue DummyStateRequiringAllParticipantsSignatures as issuer is a participant`() {
        initialiseParties()
        createDummyStateRequiringAllParticipantsSignatures(aliceParty)

        val dummyStateRequiringAllParticipantsSignatures = getStateAndRefs<DummyStateRequiringAllParticipantsSignatures>(aliceNode)[0]
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(dummyStateRequiringAllParticipantsSignatures),
            DummyStateRequiringAllParticipantsSignaturesContract.Commands.Create(),
            bobParty,
            listOf(aliceParty, bobParty, acceptorParty)
        )

        val reIssuanceRequest = bobNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<DummyStateRequiringAllParticipantsSignatures>(bobNode, reIssuanceRequest,
            issuerIsRequiredExitCommandSigner = true)
    }

    @Test(expected = TransactionVerificationException::class)
    fun `Another party can't re-issue tokens as issuer information is stored in IssuedTokenType`() {
        initialiseParties()
        issueTokens(aliceParty, 50)

        val tokens = getTokens(aliceNode)

        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            tokens,
            IssueTokenCommand(issuedTokenType, tokens.indices.toList()),
            bobParty
        )

        val reIssuanceRequest = bobNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<FungibleToken>(bobNode, reIssuanceRequest,
            issuerIsRequiredExitCommandSigner = true)

    }

    @Test(expected = IllegalArgumentException::class)
    fun `State can't be re-issued twice`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)

        val simpleDummyState = getStateAndRefs<SimpleDummyState>(aliceNode)[0]
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(simpleDummyState),
            SimpleDummyStateContract.Commands.Create(),
            issuerParty
        )
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(simpleDummyState),
            SimpleDummyStateContract.Commands.Create(),
            issuerParty
        )

        val reIssuanceRequest1 = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        val reIssuanceRequest2 = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[1]

        reIssueRequestedStates<SimpleDummyState>(issuerNode, reIssuanceRequest1,
            issuerIsRequiredExitCommandSigner = false)
        reIssueRequestedStates<SimpleDummyState>(issuerNode, reIssuanceRequest2,
            issuerIsRequiredExitCommandSigner = false)

    }
}
