package com.template

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import com.r3.corda.lib.tokens.contracts.commands.IssueTokenCommand
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.template.contracts.example.SimpleDummyStateContract
import com.template.contracts.example.DummyStateRequiringAcceptanceContract
import com.template.contracts.example.DummyStateRequiringAllParticipantsSignaturesContract
import com.template.states.ReIssuanceLock
import com.template.states.ReIssuanceRequest
import com.template.states.example.SimpleDummyState
import com.template.states.example.DummyStateRequiringAcceptance
import com.template.states.example.DummyStateRequiringAllParticipantsSignatures
import net.corda.core.identity.AbstractParty
import net.corda.core.node.services.queryBy
import org.junit.Test

class ReIssueStatesTest: AbstractFlowTest() {

    @Test
    fun `SimpleDummyState is re-issued`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)

        val simpleState = getStateAndRefs<SimpleDummyState>(aliceNode)[0]
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(simpleState),
            SimpleDummyStateContract.Commands.Create(),
            issuerParty
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<SimpleDummyState>(issuerNode, reIssuanceRequest)

        val encumberedStates = getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = true)
        val unencumberedStates = getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = false)
        val lockStates = getStateAndRefs<ReIssuanceLock<SimpleDummyState>>(aliceNode, encumbered = true)
        assertThat(encumberedStates, hasSize(equalTo(1)))
        assertThat(unencumberedStates, hasSize(equalTo(1)))
        assertThat(lockStates, hasSize(equalTo(1)))
        assertThat(encumberedStates.map { it.state.data }, equalTo(unencumberedStates.map { it.state.data }))
        assertThat(lockStates[0].state.data.issuer, equalTo(issuerParty as AbstractParty))
        assertThat(lockStates[0].state.data.requester, equalTo(aliceParty as AbstractParty))
        assertThat(lockStates[0].state.data.originalStates, equalTo(unencumberedStates))
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
        reIssueRequestedStates<DummyStateRequiringAcceptance>(issuerNode, reIssuanceRequest)

        val encumberedStates = getStateAndRefs<DummyStateRequiringAcceptance>(aliceNode, encumbered = true)
        val unencumberedStates = getStateAndRefs<DummyStateRequiringAcceptance>(aliceNode, encumbered = false)
        val lockStates = getStateAndRefs<ReIssuanceLock<DummyStateRequiringAcceptance>>(aliceNode, encumbered = true)
        assertThat(encumberedStates, hasSize(equalTo(1)))
        assertThat(unencumberedStates, hasSize(equalTo(1)))
        assertThat(lockStates, hasSize(equalTo(1)))
        assertThat(encumberedStates.map { it.state.data }, equalTo(unencumberedStates.map { it.state.data }))
        assertThat(lockStates[0].state.data.issuer, equalTo(issuerParty as AbstractParty))
        assertThat(lockStates[0].state.data.requester, equalTo(aliceParty as AbstractParty))
        assertThat(lockStates[0].state.data.originalStates, equalTo(unencumberedStates))
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
        reIssueRequestedStates<DummyStateRequiringAllParticipantsSignatures>(issuerNode, reIssuanceRequest)

        val encumberedStates = getStateAndRefs<DummyStateRequiringAllParticipantsSignatures>(aliceNode, encumbered = true)
        val unencumberedStates = getStateAndRefs<DummyStateRequiringAllParticipantsSignatures>(aliceNode, encumbered = false)
        val lockStates = getStateAndRefs<ReIssuanceLock<DummyStateRequiringAllParticipantsSignatures>>(aliceNode, encumbered = true)
        assertThat(encumberedStates, hasSize(equalTo(1)))
        assertThat(unencumberedStates, hasSize(equalTo(1)))
        assertThat(lockStates, hasSize(equalTo(1)))
        assertThat(encumberedStates.map { it.state.data }, equalTo(unencumberedStates.map { it.state.data }))
        assertThat(lockStates[0].state.data.issuer, equalTo(issuerParty as AbstractParty))
        assertThat(lockStates[0].state.data.requester, equalTo(aliceParty as AbstractParty))
        assertThat(lockStates[0].state.data.originalStates, equalTo(unencumberedStates))
    }

    @Test
    fun `Tokens are re-issued`() {
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
        reIssueRequestedStates<FungibleToken>(issuerNode, reIssuanceRequest)

        val encumberedStates = getStateAndRefs<FungibleToken>(aliceNode, encumbered = true)
        val unencumberedStates = getStateAndRefs<FungibleToken>(aliceNode, encumbered = false)
        val lockStates = getStateAndRefs<ReIssuanceLock<FungibleToken>>(aliceNode, encumbered = true)
        assertThat(encumberedStates, hasSize(equalTo(1)))
        assertThat(unencumberedStates, hasSize(equalTo(1)))
        assertThat(lockStates, hasSize(equalTo(1)))
        assertThat(encumberedStates.map { it.state.data }, equalTo(unencumberedStates.map { it.state.data }))
        assertThat(lockStates[0].state.data.issuer, equalTo(issuerParty as AbstractParty))
        assertThat(lockStates[0].state.data.requester, equalTo(aliceParty as AbstractParty))
        assertThat(lockStates[0].state.data.originalStates, equalTo(unencumberedStates))
    }

    // TODO: many tokens are re-issued

    @Test
    fun `SimpleDummyState re-issued - accounts on the same host`() {
        initialisePartiesForAccountsOnTheSameHost()
        createSimpleDummyStateForAccount(employeeNode, employeeAliceParty)

        val simpleState = getStateAndRefs<SimpleDummyState>(employeeNode,
            accountUUID = employeeAliceAccount.identifier.id)[0]
        createReIssuanceRequestAndShareRequiredTransactions(
            employeeNode,
            listOf(simpleState),
            SimpleDummyStateContract.Commands.Create(),
            employeeIssuerParty,
            requester = employeeAliceParty
        )

        val reIssuanceRequest = employeeNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<SimpleDummyState>(employeeNode, reIssuanceRequest)

        val encumberedStates = getStateAndRefs<SimpleDummyState>(employeeNode, encumbered = true,
            accountUUID = employeeAliceAccount.identifier.id)
        val unencumberedStates = getStateAndRefs<SimpleDummyState>(employeeNode, encumbered = false,
            accountUUID = employeeAliceAccount.identifier.id)
        val lockStates = getStateAndRefs<ReIssuanceLock<SimpleDummyState>>(employeeNode, encumbered = true,
            accountUUID = employeeAliceAccount.identifier.id)
        assertThat(encumberedStates, hasSize(equalTo(1)))
        assertThat(unencumberedStates, hasSize(equalTo(1)))
        assertThat(lockStates, hasSize(equalTo(1)))
        assertThat(encumberedStates.map { it.state.data }, equalTo(unencumberedStates.map { it.state.data }))
        assertThat(lockStates[0].state.data.issuer, equalTo(employeeIssuerParty))
        assertThat(lockStates[0].state.data.requester, equalTo(employeeAliceParty))
        assertThat(lockStates[0].state.data.originalStates, equalTo(unencumberedStates))
    }

    @Test
    fun `SimpleDummyState re-issued - accounts on different hosts`() {
        initialisePartiesForAccountsOnDifferentHosts()
        createSimpleDummyStateForAccount(issuerNode, employeeAliceParty)

        val simpleState = getStateAndRefs<SimpleDummyState>(aliceNode)[0]
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(simpleState),
            SimpleDummyStateContract.Commands.Create(),
            employeeIssuerParty,
            requester = employeeAliceParty
        )

        val reIssuanceRequest = aliceNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<SimpleDummyState>(issuerNode, reIssuanceRequest)

        val encumberedStates = getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = true,
            accountUUID = employeeAliceAccount.identifier.id)
        val unencumberedStates = getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = false,
            accountUUID = employeeAliceAccount.identifier.id)
        val lockStates = getStateAndRefs<ReIssuanceLock<SimpleDummyState>>(aliceNode, encumbered = true)
        assertThat(encumberedStates, hasSize(equalTo(1)))
        assertThat(unencumberedStates, hasSize(equalTo(1)))
        assertThat(lockStates, hasSize(equalTo(1)))
        assertThat(encumberedStates.map { it.state.data }, equalTo(unencumberedStates.map { it.state.data }))
        assertThat(lockStates[0].state.data.issuer, equalTo(employeeIssuerParty))
        assertThat(lockStates[0].state.data.requester, equalTo(employeeAliceParty))
        assertThat(lockStates[0].state.data.originalStates, equalTo(unencumberedStates))
    }

    @Test(expected = IllegalArgumentException::class) // issues find state by reference
    fun `State cannot be re-issued if issuer cannot access it`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)

        val simpleStateRef = getStateAndRefs<SimpleDummyState>(aliceNode)[0].ref
        createReIssuanceRequest<SimpleDummyState>( // don't share required transactions
            aliceNode,
            listOf(simpleStateRef),
            SimpleDummyStateContract.Commands.Create(),
            issuerParty
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<SimpleDummyState>(issuerNode, reIssuanceRequest)
    }

    @Test // issuer doesn't have an information about state being consumed
    fun `Consumed state is re-issued when issuer is not a participant`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)

        val simpleState = getStateAndRefs<SimpleDummyState>(aliceNode)[0]

        updateSimpleDummyState(aliceNode, bobParty)

        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(simpleState),
            SimpleDummyStateContract.Commands.Create(),
            issuerParty
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<SimpleDummyState>(issuerNode, reIssuanceRequest)

        val encumberedStates = getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = true)
        val unencumberedStates = getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = false)
        val lockStates = getStateAndRefs<ReIssuanceLock<SimpleDummyState>>(aliceNode, encumbered = true)
        assertThat(encumberedStates, hasSize(equalTo(1)))
        assertThat(unencumberedStates, hasSize(equalTo(0)))
        assertThat(lockStates, hasSize(equalTo(1)))
        assertThat(lockStates[0].state.data.issuer, equalTo(issuerParty as AbstractParty))
        assertThat(lockStates[0].state.data.requester, equalTo(aliceParty as AbstractParty))
        assertThat(lockStates[0].state.data.originalStates.map { it.state.data },
            equalTo(encumberedStates.map { it.state.data }))
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
        reIssueRequestedStates<DummyStateRequiringAcceptance>(issuerNode, reIssuanceRequest)
    }
}
