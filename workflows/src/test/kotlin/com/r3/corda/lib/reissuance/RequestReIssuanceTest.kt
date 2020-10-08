package com.r3.corda.lib.reissuance

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import com.r3.corda.lib.reissuance.dummy_contracts.DummyStateRequiringAcceptanceContract
import com.r3.corda.lib.reissuance.dummy_contracts.DummyStateRequiringAllParticipantsSignaturesContract
import com.r3.corda.lib.reissuance.dummy_contracts.SimpleDummyStateContract
import com.r3.corda.lib.reissuance.states.ReIssuanceRequest
import com.r3.corda.lib.reissuance.dummy_states.DummyStateRequiringAcceptance
import com.r3.corda.lib.reissuance.dummy_states.DummyStateRequiringAllParticipantsSignatures
import com.r3.corda.lib.reissuance.dummy_states.SimpleDummyState
import com.r3.corda.lib.tokens.contracts.commands.IssueTokenCommand
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.identity.AbstractParty
import net.corda.core.node.services.queryBy
import org.junit.Test

class RequestReIssuanceTest: AbstractFlowTest() {

    @Test
    fun `SimpleDummyState re-issuance request is created`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)

        val simpleDummyState = getStateAndRefs<SimpleDummyState>(aliceNode)[0]
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(simpleDummyState),
            SimpleDummyStateContract.Commands.Create(),
            issuerParty
        )

        val reIssuanceRequests = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states
        assertThat(reIssuanceRequests, hasSize(`is`(1)))
        assertThat(reIssuanceRequests[0].state.data.issuer.owningKey, `is`(issuerParty.owningKey))
        assertThat(reIssuanceRequests[0].state.data.requester.owningKey, `is`(aliceParty.owningKey))
        assertThat(reIssuanceRequests[0].state.data.assetIssuanceCommand is SimpleDummyStateContract.Commands.Create, `is`(true))
        assertThat(reIssuanceRequests[0].state.data.assetIssuanceSigners, `is`(listOf(issuerParty as AbstractParty)))
        assertThat(reIssuanceRequests[0].state.data.stateRefsToReIssue, `is`(listOf(simpleDummyState.ref)))

        val simpleDummyStatesAvailableToIssuer = getStateAndRefs<SimpleDummyState>(issuerNode)
        assertThat(simpleDummyStatesAvailableToIssuer, hasSize(`is`(1)))
        assertThat(simpleDummyStatesAvailableToIssuer[0], `is`(simpleDummyState))
    }

    @Test
    fun `DummyStateRequiringAcceptance re-issuance request is created`() {
        initialiseParties()
        createDummyStateRequiringAcceptance(aliceParty)

        val dummyStateRequiringAcceptance = getStateAndRefs<DummyStateRequiringAcceptance>(aliceNode)[0]
        val extraIssuanceCommandSigners = listOf(acceptorParty)
        val allIssuanceCommandSigners = listOf(issuerParty) + extraIssuanceCommandSigners
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(dummyStateRequiringAcceptance),
            DummyStateRequiringAcceptanceContract.Commands.Create(),
            issuerParty,
            extraIssuanceCommandSigners
        )

        val reIssuanceRequests = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states
        assertThat(reIssuanceRequests, hasSize(`is`(1)))
        assertThat(reIssuanceRequests[0].state.data.issuer.owningKey, `is`(issuerParty.owningKey))
        assertThat(reIssuanceRequests[0].state.data.requester.owningKey, `is`(aliceParty.owningKey))
        assertThat(reIssuanceRequests[0].state.data.assetIssuanceCommand is DummyStateRequiringAcceptanceContract.Commands.Create, `is`(true))
        assertThat(reIssuanceRequests[0].state.data.assetIssuanceSigners, `is`(allIssuanceCommandSigners as List<AbstractParty>))
        assertThat(reIssuanceRequests[0].state.data.stateRefsToReIssue, `is`(listOf(dummyStateRequiringAcceptance.ref)))

        val dummyStatesRequiringAcceptanceAvailableToIssuer = getStateAndRefs<DummyStateRequiringAcceptance>(issuerNode)
        assertThat(dummyStatesRequiringAcceptanceAvailableToIssuer, hasSize(`is`(1)))
        assertThat(dummyStatesRequiringAcceptanceAvailableToIssuer[0], `is`(dummyStateRequiringAcceptance))
    }

    @Test
    fun `DummyStateRequiringAllParticipantsSignatures re-issuance request is created`() {
        initialiseParties()
        createDummyStateRequiringAllParticipantsSignatures(aliceParty)

        val dummyStateRequiringAllParticipantsSignatures = getStateAndRefs<DummyStateRequiringAllParticipantsSignatures>(aliceNode)[0]
        val extraIssuanceCommandSigners = listOf(aliceParty, acceptorParty)
        val allIssuanceCommandSigners = listOf(issuerParty) + extraIssuanceCommandSigners
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(dummyStateRequiringAllParticipantsSignatures),
            DummyStateRequiringAllParticipantsSignaturesContract.Commands.Create(),
            issuerParty,
            extraIssuanceCommandSigners
        )

        val reIssuanceRequests = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states
        assertThat(reIssuanceRequests, hasSize(`is`(1)))
        assertThat(reIssuanceRequests[0].state.data.issuer.owningKey, `is`(issuerParty.owningKey))
        assertThat(reIssuanceRequests[0].state.data.requester.owningKey, `is`(aliceParty.owningKey))
        assertThat(reIssuanceRequests[0].state.data.assetIssuanceCommand is DummyStateRequiringAllParticipantsSignaturesContract.Commands.Create, `is`(true))
        assertThat(reIssuanceRequests[0].state.data.assetIssuanceSigners, `is`(allIssuanceCommandSigners as List<AbstractParty>))
        assertThat(reIssuanceRequests[0].state.data.stateRefsToReIssue, `is`(listOf(dummyStateRequiringAllParticipantsSignatures.ref)))

        val dummyStatesRequiringAllParticipantsSignaturesAvailableToIssuer = getStateAndRefs<DummyStateRequiringAllParticipantsSignatures>(
            issuerNode)
        assertThat(dummyStatesRequiringAllParticipantsSignaturesAvailableToIssuer, hasSize(`is`(1)))
        assertThat(dummyStatesRequiringAllParticipantsSignaturesAvailableToIssuer[0], `is`(dummyStateRequiringAllParticipantsSignatures))
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
        assertThat(reIssuanceRequests, hasSize(`is`(1)))
        assertThat(reIssuanceRequests[0].state.data.issuer.owningKey, `is`(issuerParty.owningKey))
        assertThat(reIssuanceRequests[0].state.data.requester.owningKey, `is`(aliceParty.owningKey))
        assertThat(reIssuanceRequests[0].state.data.assetIssuanceCommand is IssueTokenCommand, `is`(true))
        val issuanceCommand = reIssuanceRequests[0].state.data.assetIssuanceCommand as IssueTokenCommand
        assertThat(issuanceCommand.token, `is`(issuedTokenType))
        assertThat(issuanceCommand.outputs, `is`(listOf(0)))
        assertThat(reIssuanceRequests[0].state.data.assetIssuanceSigners, `is`(listOf(issuerParty as AbstractParty)))
        assertThat(reIssuanceRequests[0].state.data.stateRefsToReIssue, `is`(tokens.map { it.ref }))

        val tokensAvailableToIssuer = getStateAndRefs<FungibleToken>(issuerNode)
        assertThat(tokensAvailableToIssuer, hasSize(`is`(1)))
        assertThat(tokensAvailableToIssuer, `is`(tokens))
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
        assertThat(reIssuanceRequests, hasSize(`is`(1)))
        assertThat(reIssuanceRequests[0].state.data.issuer.owningKey, `is`(issuerParty.owningKey))
        assertThat(reIssuanceRequests[0].state.data.requester.owningKey, `is`(aliceParty.owningKey))
        assertThat(reIssuanceRequests[0].state.data.assetIssuanceCommand is IssueTokenCommand, `is`(true))
        val issuanceCommand = reIssuanceRequests[0].state.data.assetIssuanceCommand as IssueTokenCommand
        assertThat(issuanceCommand.token, `is`(issuedTokenType))
        assertThat(issuanceCommand.outputs, `is`(listOf(0, 1)))
        assertThat(reIssuanceRequests[0].state.data.assetIssuanceSigners, `is`(listOf(issuerParty as AbstractParty)))
        assertThat(reIssuanceRequests[0].state.data.stateRefsToReIssue, `is`(tokens.map { it.ref }))

        val tokensAvailableToIssuer = getStateAndRefs<FungibleToken>(issuerNode)
        assertThat(tokensAvailableToIssuer, hasSize(`is`(2)))
        assertThat(tokensAvailableToIssuer, `is`(tokens))
    }

    @Test
    fun `SimpleDummyState re-issuance request is created - accounts on the same host`() {
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

        val reIssuanceRequests = employeeNode.services.vaultService.queryBy<ReIssuanceRequest>().states
        assertThat(reIssuanceRequests, hasSize(`is`(1)))
        assertThat(reIssuanceRequests[0].state.data.issuer.owningKey, `is`(employeeIssuerParty.owningKey))
        assertThat(reIssuanceRequests[0].state.data.requester.owningKey, `is`(employeeAliceParty.owningKey))
        assertThat(reIssuanceRequests[0].state.data.assetIssuanceCommand is SimpleDummyStateContract.Commands.Create, `is`(true))
        assertThat(reIssuanceRequests[0].state.data.assetIssuanceSigners, `is`(listOf(employeeIssuerParty)))
        assertThat(reIssuanceRequests[0].state.data.stateRefsToReIssue, `is`(listOf(simpleDummyState.ref)))

        val simpleDummyStatesAvailableToIssuer = getStateAndRefs<SimpleDummyState>(employeeNode) // available to node, not account
        assertThat(simpleDummyStatesAvailableToIssuer, hasSize(`is`(1)))
        assertThat(simpleDummyStatesAvailableToIssuer[0], `is`(simpleDummyState))
    }

    @Test
    fun `SimpleDummyState re-issuance request is created - accounts on different hosts`() {
        initialisePartiesForAccountsOnDifferentHosts()
        createSimpleDummyStateForAccount(issuerNode, employeeAliceParty)

        val simpleDummyState = getStateAndRefs<SimpleDummyState>(aliceNode, accountUUID = employeeAliceAccount.identifier.id)[0]
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(simpleDummyState),
            SimpleDummyStateContract.Commands.Create(),
            employeeIssuerParty,
            requester = employeeAliceParty
        )

        val reIssuanceRequests = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states
        assertThat(reIssuanceRequests, hasSize(`is`(1)))
        assertThat(reIssuanceRequests[0].state.data.issuer.owningKey, `is`(employeeIssuerParty.owningKey))
        assertThat(reIssuanceRequests[0].state.data.requester.owningKey, `is`(employeeAliceParty.owningKey))
        assertThat(reIssuanceRequests[0].state.data.assetIssuanceCommand is SimpleDummyStateContract.Commands.Create, `is`(true))
        assertThat(reIssuanceRequests[0].state.data.assetIssuanceSigners, `is`(listOf(employeeIssuerParty)))
        assertThat(reIssuanceRequests[0].state.data.stateRefsToReIssue, `is`(listOf(simpleDummyState.ref)))

        val simpleDummyStatesAvailableToIssuer = getStateAndRefs<SimpleDummyState>(issuerNode) // available to node, not account
        assertThat(simpleDummyStatesAvailableToIssuer, hasSize(`is`(1)))
        assertThat(simpleDummyStatesAvailableToIssuer[0], `is`(simpleDummyState))
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
