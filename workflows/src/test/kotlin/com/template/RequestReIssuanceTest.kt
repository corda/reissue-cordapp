package com.template

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import com.r3.corda.lib.tokens.contracts.commands.IssueTokenCommand
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.template.contracts.example.SimpleStateContract
import com.template.contracts.example.StateNeedingAcceptanceContract
import com.template.contracts.example.StateNeedingAllParticipantsToSignContract
import com.template.states.ReIssuanceRequest
import com.template.states.example.SimpleState
import com.template.states.example.StateNeedingAcceptance
import com.template.states.example.StateNeedingAllParticipantsToSign
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.identity.AbstractParty
import net.corda.core.node.services.queryBy
import org.junit.Test

class RequestReIssuanceTest: AbstractFlowTest() {

    @Test
    fun `SimpleState re-issuance request is created`() {
        initialiseParties()
        createSimpleState(aliceParty)

        val simpleState = getStateAndRefs<SimpleState>(aliceNode)[0]
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(simpleState),
            SimpleStateContract.Commands.Create(),
            issuerParty
        )

        val reIssuanceRequests = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states
        assertThat(reIssuanceRequests, hasSize(equalTo(1)))
        assertThat(reIssuanceRequests[0].state.data.issuer.owningKey, equalTo(issuerParty.owningKey))
        assertThat(reIssuanceRequests[0].state.data.requester.owningKey, equalTo(aliceParty.owningKey))
        assertThat(reIssuanceRequests[0].state.data.issuanceCommand is SimpleStateContract.Commands.Create, equalTo(true))
        assertThat(reIssuanceRequests[0].state.data.issuanceSigners, equalTo(listOf(issuerParty as AbstractParty)))
        assertThat(reIssuanceRequests[0].state.data.stateRefsToReIssue, equalTo(listOf(simpleState.ref)))

        val simpleStatesAvailableToIssuer = getStateAndRefs<SimpleState>(issuerNode)
        assertThat(simpleStatesAvailableToIssuer, hasSize(equalTo(1)))
        assertThat(simpleStatesAvailableToIssuer[0], equalTo(simpleState))
    }

    @Test
    fun `StateNeedingAcceptance re-issuance request is created`() {
        initialiseParties()
        createStateNeedingAcceptance(aliceParty)

        val stateNeedingAcceptance = getStateAndRefs<StateNeedingAcceptance>(aliceNode)[0]
        val issuanceCommandSigners = listOf(issuerParty, acceptorParty)
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(stateNeedingAcceptance),
            StateNeedingAcceptanceContract.Commands.Create(),
            issuerParty,
            issuanceCommandSigners
        )

        val reIssuanceRequests = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states
        assertThat(reIssuanceRequests, hasSize(equalTo(1)))
        assertThat(reIssuanceRequests[0].state.data.issuer.owningKey, equalTo(issuerParty.owningKey))
        assertThat(reIssuanceRequests[0].state.data.requester.owningKey, equalTo(aliceParty.owningKey))
        assertThat(reIssuanceRequests[0].state.data.issuanceCommand is StateNeedingAcceptanceContract.Commands.Create, equalTo(true))
        assertThat(reIssuanceRequests[0].state.data.issuanceSigners, equalTo(issuanceCommandSigners as List<AbstractParty>))
        assertThat(reIssuanceRequests[0].state.data.stateRefsToReIssue, equalTo(listOf(stateNeedingAcceptance.ref)))

        val statesNeedingAcceptanceAvailableToIssuer = getStateAndRefs<StateNeedingAcceptance>(issuerNode)
        assertThat(statesNeedingAcceptanceAvailableToIssuer, hasSize(equalTo(1)))
        assertThat(statesNeedingAcceptanceAvailableToIssuer[0], equalTo(stateNeedingAcceptance))
    }

    @Test
    fun `StateNeedingAllParticipantsToSign re-issuance request is created`() {
        initialiseParties()
        createStateNeedingAllParticipantsToSign(aliceParty)

        val stateNeedingAllParticipantsToSign = getStateAndRefs<StateNeedingAllParticipantsToSign>(aliceNode)[0]
        val issuanceCommandSigners = listOf(aliceParty, issuerParty, acceptorParty)
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(stateNeedingAllParticipantsToSign),
            StateNeedingAllParticipantsToSignContract.Commands.Create(),
            issuerParty,
            issuanceCommandSigners
        )

        val reIssuanceRequests = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states
        assertThat(reIssuanceRequests, hasSize(equalTo(1)))
        assertThat(reIssuanceRequests[0].state.data.issuer.owningKey, equalTo(issuerParty.owningKey))
        assertThat(reIssuanceRequests[0].state.data.requester.owningKey, equalTo(aliceParty.owningKey))
        assertThat(reIssuanceRequests[0].state.data.issuanceCommand is StateNeedingAllParticipantsToSignContract.Commands.Create, equalTo(true))
        assertThat(reIssuanceRequests[0].state.data.issuanceSigners, equalTo(issuanceCommandSigners as List<AbstractParty>))
        assertThat(reIssuanceRequests[0].state.data.stateRefsToReIssue, equalTo(listOf(stateNeedingAllParticipantsToSign.ref)))

        val statesNeedingAllParticipantsToSignAvailableToIssuer = getStateAndRefs<StateNeedingAllParticipantsToSign>(
            issuerNode)
        assertThat(statesNeedingAllParticipantsToSignAvailableToIssuer, hasSize(equalTo(1)))
        assertThat(statesNeedingAllParticipantsToSignAvailableToIssuer[0], equalTo(stateNeedingAllParticipantsToSign))
    }

    @Test
    fun `Tokens re-issuance request is created`() {
        initialiseParties()
        issueTokens(aliceParty, 50)

        val tokens = getTokens(aliceNode)
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            tokens,
            IssueTokenCommand(issuedTokenType, tokens.indices.toList()),
            issuerParty
        )

        val reIssuanceRequests = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states
        assertThat(reIssuanceRequests, hasSize(equalTo(1)))
        assertThat(reIssuanceRequests[0].state.data.issuer.owningKey, equalTo(issuerParty.owningKey))
        assertThat(reIssuanceRequests[0].state.data.requester.owningKey, equalTo(aliceParty.owningKey))
        assertThat(reIssuanceRequests[0].state.data.issuanceCommand is IssueTokenCommand, equalTo(true)) // TODO: better check
        assertThat(reIssuanceRequests[0].state.data.issuanceSigners, equalTo(listOf(issuerParty as AbstractParty)))
        assertThat(reIssuanceRequests[0].state.data.stateRefsToReIssue, equalTo(tokens.map { it.ref }))

        val tokensAvailableToIssuer = getStateAndRefs<FungibleToken>(issuerNode)
        assertThat(tokensAvailableToIssuer, hasSize(equalTo(1)))
        assertThat(tokensAvailableToIssuer, equalTo(tokens))
    }

    @Test
    fun `Tokens re-issuance request is created with many states`() {
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

        val reIssuanceRequests = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states
        assertThat(reIssuanceRequests, hasSize(equalTo(1)))
        assertThat(reIssuanceRequests[0].state.data.issuer.owningKey, equalTo(issuerParty.owningKey))
        assertThat(reIssuanceRequests[0].state.data.requester.owningKey, equalTo(aliceParty.owningKey))
        assertThat(reIssuanceRequests[0].state.data.issuanceCommand is IssueTokenCommand, equalTo(true)) // TODO: better check
        assertThat(reIssuanceRequests[0].state.data.issuanceSigners, equalTo(listOf(issuerParty as AbstractParty)))
        assertThat(reIssuanceRequests[0].state.data.stateRefsToReIssue, equalTo(tokens.map { it.ref }))

        val tokensAvailableToIssuer = getStateAndRefs<FungibleToken>(issuerNode)
        assertThat(tokensAvailableToIssuer, hasSize(equalTo(2)))
        assertThat(tokensAvailableToIssuer, equalTo(tokens))
    }

    @Test
    fun `SimpleState re-issuance request is created - accounts on the same host`() {
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

        val reIssuanceRequests = employeeNode.services.vaultService.queryBy<ReIssuanceRequest>().states
        assertThat(reIssuanceRequests, hasSize(equalTo(1)))
        assertThat(reIssuanceRequests[0].state.data.issuer.owningKey, equalTo(employeeIssuerParty.owningKey))
        assertThat(reIssuanceRequests[0].state.data.requester.owningKey, equalTo(employeeAliceParty.owningKey))
        assertThat(reIssuanceRequests[0].state.data.issuanceCommand is SimpleStateContract.Commands.Create, equalTo(true))
        assertThat(reIssuanceRequests[0].state.data.issuanceSigners, equalTo(listOf(employeeIssuerParty)))
        assertThat(reIssuanceRequests[0].state.data.stateRefsToReIssue, equalTo(listOf(simpleState.ref)))

        val simpleStatesAvailableToIssuer = getStateAndRefs<SimpleState>(employeeNode)
        assertThat(simpleStatesAvailableToIssuer, hasSize(equalTo(1)))
        assertThat(simpleStatesAvailableToIssuer[0], equalTo(simpleState))
    }

    @Test
    fun `SimpleState re-issuance request is created - accounts on different hosts`() {
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

        val reIssuanceRequests = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states
        assertThat(reIssuanceRequests, hasSize(equalTo(1)))
        assertThat(reIssuanceRequests[0].state.data.issuer.owningKey, equalTo(employeeIssuerParty.owningKey))
        assertThat(reIssuanceRequests[0].state.data.requester.owningKey, equalTo(employeeAliceParty.owningKey))
        assertThat(reIssuanceRequests[0].state.data.issuanceCommand is SimpleStateContract.Commands.Create, equalTo(true))
        assertThat(reIssuanceRequests[0].state.data.issuanceSigners, equalTo(listOf(employeeIssuerParty)))
        assertThat(reIssuanceRequests[0].state.data.stateRefsToReIssue, equalTo(listOf(simpleState.ref)))

        val simpleStatesAvailableToIssuer = getStateAndRefs<SimpleState>(issuerNode)
        assertThat(simpleStatesAvailableToIssuer, hasSize(equalTo(1)))
        assertThat(simpleStatesAvailableToIssuer[0], equalTo(simpleState))
    }

    @Test(expected = TransactionVerificationException::class)
    fun `Request re-issuance of 0 states can't be created`() {
        initialiseParties()
        createReIssuanceRequestAndShareRequiredTransactions<SimpleState>(
            aliceNode,
            listOf(),
            SimpleStateContract.Commands.Create(),
            issuerParty
        )
    }
}
