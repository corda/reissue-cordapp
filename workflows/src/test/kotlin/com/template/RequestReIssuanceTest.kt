package com.template

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import com.r3.corda.lib.tokens.contracts.commands.IssueTokenCommand
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.template.contracts.example.SimpleDummyStateContract
import com.template.contracts.example.DummyStateRequiringAcceptanceContract
import com.template.contracts.example.DummyStateRequiringAllParticipantsSignaturesContract
import com.template.states.ReIssuanceRequest
import com.template.states.example.SimpleDummyState
import com.template.states.example.DummyStateRequiringAcceptance
import com.template.states.example.DummyStateRequiringAllParticipantsSignatures
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.identity.AbstractParty
import net.corda.core.node.services.queryBy
import org.junit.Test

class RequestReIssuanceTest: AbstractFlowTest() {

    @Test
    fun `SimpleState re-issuance request is created`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)

        val simpleState = getStateAndRefs<SimpleDummyState>(aliceNode)[0]
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(simpleState),
            SimpleDummyStateContract.Commands.Create(),
            issuerParty
        )

        val reIssuanceRequests = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states
        assertThat(reIssuanceRequests, hasSize(equalTo(1)))
        assertThat(reIssuanceRequests[0].state.data.issuer.owningKey, equalTo(issuerParty.owningKey))
        assertThat(reIssuanceRequests[0].state.data.requester.owningKey, equalTo(aliceParty.owningKey))
        assertThat(reIssuanceRequests[0].state.data.issuanceCommand is SimpleDummyStateContract.Commands.Create, equalTo(true))
        assertThat(reIssuanceRequests[0].state.data.issuanceSigners, equalTo(listOf(issuerParty as AbstractParty)))
        assertThat(reIssuanceRequests[0].state.data.stateRefsToReIssue, equalTo(listOf(simpleState.ref)))

        val simpleStatesAvailableToIssuer = getStateAndRefs<SimpleDummyState>(issuerNode)
        assertThat(simpleStatesAvailableToIssuer, hasSize(equalTo(1)))
        assertThat(simpleStatesAvailableToIssuer[0], equalTo(simpleState))
    }

    @Test
    fun `StateNeedingAcceptance re-issuance request is created`() {
        initialiseParties()
        createDummyStateRequiringAcceptance(aliceParty)

        val stateNeedingAcceptance = getStateAndRefs<DummyStateRequiringAcceptance>(aliceNode)[0]
        val issuanceCommandSigners = listOf(issuerParty, acceptorParty)
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(stateNeedingAcceptance),
            DummyStateRequiringAcceptanceContract.Commands.Create(),
            issuerParty,
            issuanceCommandSigners
        )

        val reIssuanceRequests = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states
        assertThat(reIssuanceRequests, hasSize(equalTo(1)))
        assertThat(reIssuanceRequests[0].state.data.issuer.owningKey, equalTo(issuerParty.owningKey))
        assertThat(reIssuanceRequests[0].state.data.requester.owningKey, equalTo(aliceParty.owningKey))
        assertThat(reIssuanceRequests[0].state.data.issuanceCommand is DummyStateRequiringAcceptanceContract.Commands.Create, equalTo(true))
        assertThat(reIssuanceRequests[0].state.data.issuanceSigners, equalTo(issuanceCommandSigners as List<AbstractParty>))
        assertThat(reIssuanceRequests[0].state.data.stateRefsToReIssue, equalTo(listOf(stateNeedingAcceptance.ref)))

        val statesNeedingAcceptanceAvailableToIssuer = getStateAndRefs<DummyStateRequiringAcceptance>(issuerNode)
        assertThat(statesNeedingAcceptanceAvailableToIssuer, hasSize(equalTo(1)))
        assertThat(statesNeedingAcceptanceAvailableToIssuer[0], equalTo(stateNeedingAcceptance))
    }

    @Test
    fun `StateNeedingAllParticipantsToSign re-issuance request is created`() {
        initialiseParties()
        createDummyStateRequiringAllParticipantsSignatures(aliceParty)

        val stateNeedingAllParticipantsToSign = getStateAndRefs<DummyStateRequiringAllParticipantsSignatures>(aliceNode)[0]
        val issuanceCommandSigners = listOf(aliceParty, issuerParty, acceptorParty)
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(stateNeedingAllParticipantsToSign),
            DummyStateRequiringAllParticipantsSignaturesContract.Commands.Create(),
            issuerParty,
            issuanceCommandSigners
        )

        val reIssuanceRequests = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states
        assertThat(reIssuanceRequests, hasSize(equalTo(1)))
        assertThat(reIssuanceRequests[0].state.data.issuer.owningKey, equalTo(issuerParty.owningKey))
        assertThat(reIssuanceRequests[0].state.data.requester.owningKey, equalTo(aliceParty.owningKey))
        assertThat(reIssuanceRequests[0].state.data.issuanceCommand is DummyStateRequiringAllParticipantsSignaturesContract.Commands.Create, equalTo(true))
        assertThat(reIssuanceRequests[0].state.data.issuanceSigners, equalTo(issuanceCommandSigners as List<AbstractParty>))
        assertThat(reIssuanceRequests[0].state.data.stateRefsToReIssue, equalTo(listOf(stateNeedingAllParticipantsToSign.ref)))

        val statesNeedingAllParticipantsToSignAvailableToIssuer = getStateAndRefs<DummyStateRequiringAllParticipantsSignatures>(
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

        val reIssuanceRequests = employeeNode.services.vaultService.queryBy<ReIssuanceRequest>().states
        assertThat(reIssuanceRequests, hasSize(equalTo(1)))
        assertThat(reIssuanceRequests[0].state.data.issuer.owningKey, equalTo(employeeIssuerParty.owningKey))
        assertThat(reIssuanceRequests[0].state.data.requester.owningKey, equalTo(employeeAliceParty.owningKey))
        assertThat(reIssuanceRequests[0].state.data.issuanceCommand is SimpleDummyStateContract.Commands.Create, equalTo(true))
        assertThat(reIssuanceRequests[0].state.data.issuanceSigners, equalTo(listOf(employeeIssuerParty)))
        assertThat(reIssuanceRequests[0].state.data.stateRefsToReIssue, equalTo(listOf(simpleState.ref)))

        val simpleStatesAvailableToIssuer = getStateAndRefs<SimpleDummyState>(employeeNode) // available to node, not account
        assertThat(simpleStatesAvailableToIssuer, hasSize(equalTo(1)))
        assertThat(simpleStatesAvailableToIssuer[0], equalTo(simpleState))
    }

    @Test
    fun `SimpleState re-issuance request is created - accounts on different hosts`() {
        initialisePartiesForAccountsOnDifferentHosts()
        createSimpleDummyStateForAccount(issuerNode, employeeAliceParty)

        val simpleState = getStateAndRefs<SimpleDummyState>(aliceNode, accountUUID = employeeAliceAccount.identifier.id)[0]
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(simpleState),
            SimpleDummyStateContract.Commands.Create(),
            employeeIssuerParty,
            requester = employeeAliceParty
        )

        val reIssuanceRequests = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states
        assertThat(reIssuanceRequests, hasSize(equalTo(1)))
        assertThat(reIssuanceRequests[0].state.data.issuer.owningKey, equalTo(employeeIssuerParty.owningKey))
        assertThat(reIssuanceRequests[0].state.data.requester.owningKey, equalTo(employeeAliceParty.owningKey))
        assertThat(reIssuanceRequests[0].state.data.issuanceCommand is SimpleDummyStateContract.Commands.Create, equalTo(true))
        assertThat(reIssuanceRequests[0].state.data.issuanceSigners, equalTo(listOf(employeeIssuerParty)))
        assertThat(reIssuanceRequests[0].state.data.stateRefsToReIssue, equalTo(listOf(simpleState.ref)))

        val simpleStatesAvailableToIssuer = getStateAndRefs<SimpleDummyState>(issuerNode) // available to node, not account
        assertThat(simpleStatesAvailableToIssuer, hasSize(equalTo(1)))
        assertThat(simpleStatesAvailableToIssuer[0], equalTo(simpleState))
    }

    @Test(expected = TransactionVerificationException::class)
    fun `Request re-issuance of 0 states can't be created`() {
        initialiseParties()
        createReIssuanceRequestAndShareRequiredTransactions<SimpleDummyState>(
            aliceNode,
            listOf(),
            SimpleDummyStateContract.Commands.Create(),
            issuerParty
        )
    }
}
