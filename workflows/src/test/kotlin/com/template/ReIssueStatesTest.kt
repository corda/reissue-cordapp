package com.template

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import com.r3.corda.lib.tokens.contracts.commands.IssueTokenCommand
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.template.contracts.example.SimpleStateContract
import com.template.contracts.example.StateNeedingAcceptanceContract
import com.template.contracts.example.StateNeedingAllParticipantsToSignContract
import com.template.states.ReIssuanceLock
import com.template.states.ReIssuanceRequest
import com.template.states.example.SimpleState
import com.template.states.example.StateNeedingAcceptance
import com.template.states.example.StateNeedingAllParticipantsToSign
import net.corda.core.identity.AbstractParty
import net.corda.core.node.services.queryBy
import org.junit.Test

class ReIssueStatesTest: AbstractFlowTest() {

    @Test
    fun `SimpleState is re-issued`() {
        initialiseParties()
        createSimpleState(aliceParty)

        val simpleState = getStateAndRefs<SimpleState>(aliceNode)[0]
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(simpleState),
            SimpleStateContract.Commands.Create(),
            issuerParty
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<SimpleState>(issuerNode, reIssuanceRequest)

        val encumberedStates = getStateAndRefs<SimpleState>(aliceNode, encumbered = true)
        val unencumberedStates = getStateAndRefs<SimpleState>(aliceNode, encumbered = false)
        val lockStates = getStateAndRefs<ReIssuanceLock<SimpleState>>(aliceNode, encumbered = true)
        assertThat(encumberedStates, hasSize(equalTo(1)))
        assertThat(unencumberedStates, hasSize(equalTo(1)))
        assertThat(lockStates, hasSize(equalTo(1)))
        assertThat(encumberedStates.map { it.state.data }, equalTo(unencumberedStates.map { it.state.data }))
        assertThat(lockStates[0].state.data.issuer, equalTo(issuerParty as AbstractParty))
        assertThat(lockStates[0].state.data.requester, equalTo(aliceParty as AbstractParty))
        assertThat(lockStates[0].state.data.originalStates, equalTo(unencumberedStates))
    }

    @Test
    fun `StateNeedingAcceptance is re-issued`() {
        initialiseParties()
        createStateNeedingAcceptance(aliceParty)

        val stateNeedingAcceptance = getStateAndRefs<StateNeedingAcceptance>(aliceNode)[0]
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(stateNeedingAcceptance),
            StateNeedingAcceptanceContract.Commands.Create(),
            issuerParty,
            listOf(issuerParty, acceptorParty)
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<StateNeedingAcceptance>(issuerNode, reIssuanceRequest)

        val encumberedStates = getStateAndRefs<StateNeedingAcceptance>(aliceNode, encumbered = true)
        val unencumberedStates = getStateAndRefs<StateNeedingAcceptance>(aliceNode, encumbered = false)
        val lockStates = getStateAndRefs<ReIssuanceLock<StateNeedingAcceptance>>(aliceNode, encumbered = true)
        assertThat(encumberedStates, hasSize(equalTo(1)))
        assertThat(unencumberedStates, hasSize(equalTo(1)))
        assertThat(lockStates, hasSize(equalTo(1)))
        assertThat(encumberedStates.map { it.state.data }, equalTo(unencumberedStates.map { it.state.data }))
        assertThat(lockStates[0].state.data.issuer, equalTo(issuerParty as AbstractParty))
        assertThat(lockStates[0].state.data.requester, equalTo(aliceParty as AbstractParty))
        assertThat(lockStates[0].state.data.originalStates, equalTo(unencumberedStates))
    }

    @Test
    fun `StateNeedingAllParticipantsToSign is re-issued`() {
        initialiseParties()
        createStateNeedingAllParticipantsToSign(aliceParty)

        val stateNeedingAllParticipantsToSign = getStateAndRefs<StateNeedingAllParticipantsToSign>(aliceNode)[0]
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(stateNeedingAllParticipantsToSign),
            StateNeedingAllParticipantsToSignContract.Commands.Create(),
            issuerParty,
            listOf(aliceParty, issuerParty, acceptorParty)
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<StateNeedingAllParticipantsToSign>(issuerNode, reIssuanceRequest)

        val encumberedStates = getStateAndRefs<StateNeedingAllParticipantsToSign>(aliceNode, encumbered = true)
        val unencumberedStates = getStateAndRefs<StateNeedingAllParticipantsToSign>(aliceNode, encumbered = false)
        val lockStates = getStateAndRefs<ReIssuanceLock<StateNeedingAllParticipantsToSign>>(aliceNode, encumbered = true)
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
    fun `SimpleState re-issued - accounts on the same host`() {
        initialisePartiesForAccountsOnTheSameHost()
        createSimpleStateForAccount(employeeNode, employeeAliceParty)

        val simpleState = getStateAndRefs<SimpleState>(employeeNode)[0]
        createReIssuanceRequestAndShareRequiredTransactions(
            employeeNode,
            listOf(simpleState),
            SimpleStateContract.Commands.Create(),
            employeeIssuerParty,
            requester = employeeAliceParty
        )

        val reIssuanceRequest = employeeNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<SimpleState>(employeeNode, reIssuanceRequest)

        val encumberedStates = getStateAndRefs<SimpleState>(employeeNode, encumbered = true)
        val unencumberedStates = getStateAndRefs<SimpleState>(employeeNode, encumbered = false)
        val lockStates = getStateAndRefs<ReIssuanceLock<SimpleState>>(employeeNode, encumbered = true)
        assertThat(encumberedStates, hasSize(equalTo(1)))
        assertThat(unencumberedStates, hasSize(equalTo(1)))
        assertThat(lockStates, hasSize(equalTo(1)))
        assertThat(encumberedStates.map { it.state.data }, equalTo(unencumberedStates.map { it.state.data }))
        assertThat(lockStates[0].state.data.issuer, equalTo(employeeIssuerParty))
        assertThat(lockStates[0].state.data.requester, equalTo(employeeAliceParty))
        assertThat(lockStates[0].state.data.originalStates, equalTo(unencumberedStates))
    }

    @Test
    fun `SimpleState re-issued - accounts on different hosts`() {
        initialisePartiesForAccountsOnDifferentHosts()
        createSimpleStateForAccount(issuerNode, employeeAliceParty)

        val simpleState = getStateAndRefs<SimpleState>(aliceNode)[0]
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(simpleState),
            SimpleStateContract.Commands.Create(),
            employeeIssuerParty,
            requester = employeeAliceParty
        )

        val reIssuanceRequest = aliceNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<SimpleState>(issuerNode, reIssuanceRequest)

        val encumberedStates = getStateAndRefs<SimpleState>(aliceNode, encumbered = true)
        val unencumberedStates = getStateAndRefs<SimpleState>(aliceNode, encumbered = false)
        val lockStates = getStateAndRefs<ReIssuanceLock<SimpleState>>(aliceNode, encumbered = true)
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
        createSimpleState(aliceParty)

        val simpleStateRef = getStateAndRefs<SimpleState>(aliceNode)[0].ref
        createReIssuanceRequest<SimpleState>( // don't share required transactions
            aliceNode,
            listOf(simpleStateRef),
            SimpleStateContract.Commands.Create(),
            issuerParty
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<SimpleState>(issuerNode, reIssuanceRequest)
    }

    @Test // issuer doesn't have an information about state being consumed
    fun `Consumed state is re-issued when issuer is not a participant`() {
        initialiseParties()
        createSimpleState(aliceParty)

        val simpleState = getStateAndRefs<SimpleState>(aliceNode)[0]

        updateSimpleState(aliceNode, bobParty)

        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(simpleState),
            SimpleStateContract.Commands.Create(),
            issuerParty
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<SimpleState>(issuerNode, reIssuanceRequest)

        val encumberedStates = getStateAndRefs<SimpleState>(aliceNode, encumbered = true)
        val unencumberedStates = getStateAndRefs<SimpleState>(aliceNode, encumbered = false)
        val lockStates = getStateAndRefs<ReIssuanceLock<SimpleState>>(aliceNode, encumbered = true)
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
        createStateNeedingAcceptance(aliceParty)

        val stateNeedingAcceptance = getStateAndRefs<StateNeedingAcceptance>(aliceNode)[0]
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(stateNeedingAcceptance),
            StateNeedingAcceptanceContract.Commands.Create(),
            issuerParty,
            listOf(issuerParty, acceptorParty)
        )

        updateStateNeedingAcceptance(aliceNode, bobParty)

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<StateNeedingAcceptance>(issuerNode, reIssuanceRequest)
    }
}
