package com.r3.corda.lib.reissuance

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import com.r3.corda.lib.reissuance.dummy_contracts.DummyStateRequiringAcceptanceContract
import com.r3.corda.lib.reissuance.dummy_contracts.DummyStateRequiringAllParticipantsSignaturesContract
import com.r3.corda.lib.reissuance.dummy_contracts.SimpleDummyStateContract
import com.r3.corda.lib.reissuance.states.ReIssuanceLock
import com.r3.corda.lib.reissuance.states.ReIssuanceRequest
import com.r3.corda.lib.reissuance.dummy_states.DummyStateRequiringAcceptance
import com.r3.corda.lib.reissuance.dummy_states.DummyStateRequiringAllParticipantsSignatures
import com.r3.corda.lib.reissuance.dummy_states.SimpleDummyState
import com.r3.corda.lib.tokens.contracts.commands.IssueTokenCommand
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import net.corda.core.node.services.queryBy
import org.junit.Test

class RejectReIssuanceRequestTest: AbstractFlowTest() {

    @Test
    fun `SimpleDummyState re-issuance request is rejected`() {
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
        rejectReIssuanceRequested<SimpleDummyState>(issuerNode, reIssuanceRequest)

        val encumberedStates = getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = true)
        val unencumberedStates = getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = false)
        val lockStates = getStateAndRefs<ReIssuanceLock<SimpleDummyState>>(aliceNode)
        val reIssuanceRequests = getStateAndRefs<ReIssuanceRequest>(aliceNode)

        assertThat(encumberedStates, empty())
        assertThat(unencumberedStates, hasSize(`is`(1)))
        assertThat(lockStates, empty())
        assertThat(reIssuanceRequests, empty())
    }

    @Test
    fun `DummyStateRequiringAcceptance re-issuance request is rejected`() {
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
        rejectReIssuanceRequested<DummyStateRequiringAcceptance>(issuerNode, reIssuanceRequest)

        val encumberedStates = getStateAndRefs<DummyStateRequiringAcceptance>(aliceNode, encumbered = true)
        val unencumberedStates = getStateAndRefs<DummyStateRequiringAcceptance>(aliceNode, encumbered = false)
        val lockStates = getStateAndRefs<ReIssuanceLock<DummyStateRequiringAcceptance>>(aliceNode)
        val reIssuanceRequests = getStateAndRefs<ReIssuanceRequest>(aliceNode)

        assertThat(encumberedStates, empty())
        assertThat(unencumberedStates, hasSize(`is`(1)))
        assertThat(lockStates, empty())
        assertThat(reIssuanceRequests, empty())
    }

    @Test
    fun `DummyStateRequiringAllParticipantsSignatures re-issuance request is rejected`() {
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
        rejectReIssuanceRequested<DummyStateRequiringAllParticipantsSignatures>(issuerNode, reIssuanceRequest)

        val encumberedStates = getStateAndRefs<DummyStateRequiringAllParticipantsSignatures>(aliceNode, encumbered = true)
        val unencumberedStates = getStateAndRefs<DummyStateRequiringAllParticipantsSignatures>(aliceNode, encumbered = false)
        val lockStates = getStateAndRefs<ReIssuanceLock<DummyStateRequiringAllParticipantsSignatures>>(aliceNode)
        val reIssuanceRequests = getStateAndRefs<ReIssuanceRequest>(aliceNode)

        assertThat(encumberedStates, empty())
        assertThat(unencumberedStates, hasSize(`is`(1)))
        assertThat(lockStates, empty())
        assertThat(reIssuanceRequests, empty())
    }

    @Test
    fun `Token re-issuance request is rejected`() {
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
        rejectReIssuanceRequested<FungibleToken>(issuerNode, reIssuanceRequest)

        val encumberedStates = getStateAndRefs<FungibleToken>(aliceNode, encumbered = true)
        val unencumberedStates = getStateAndRefs<FungibleToken>(aliceNode, encumbered = false)
        val lockStates = getStateAndRefs<ReIssuanceLock<FungibleToken>>(aliceNode)
        val reIssuanceRequests = getStateAndRefs<ReIssuanceRequest>(aliceNode)

        assertThat(encumberedStates, empty())
        assertThat(unencumberedStates, hasSize(`is`(1)))
        assertThat(lockStates, empty())
        assertThat(reIssuanceRequests, empty())
    }

    @Test
    fun `SimpleDummyState re-issuance request is rejected - accounts on the same host`() {
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
        rejectReIssuanceRequested<SimpleDummyState>(employeeNode, reIssuanceRequest)

        val encumberedStates = getStateAndRefs<SimpleDummyState>(employeeNode,
            accountUUID = employeeAliceAccount.identifier.id, encumbered = true)
        val unencumberedStates = getStateAndRefs<SimpleDummyState>(employeeNode,
            accountUUID = employeeAliceAccount.identifier.id, encumbered = false)
        val lockStates = getStateAndRefs<ReIssuanceLock<SimpleDummyState>>(employeeNode,
            accountUUID = employeeAliceAccount.identifier.id)
        val reIssuanceRequests = getStateAndRefs<ReIssuanceRequest>(employeeNode,
            accountUUID = employeeAliceAccount.identifier.id)

        assertThat(encumberedStates, empty())
        assertThat(unencumberedStates, hasSize(`is`(1)))
        assertThat(lockStates, empty())
        assertThat(reIssuanceRequests, empty())
    }

    @Test
    fun `SimpleDummyState re-issuance request is rejected - accounts on different hosts`() {
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
        rejectReIssuanceRequested<SimpleDummyState>(issuerNode, reIssuanceRequest)

        val encumberedStates = getStateAndRefs<SimpleDummyState>(aliceNode,
            accountUUID = employeeAliceAccount.identifier.id, encumbered = true)
        val unencumberedStates = getStateAndRefs<SimpleDummyState>(aliceNode,
            accountUUID = employeeAliceAccount.identifier.id, encumbered = false)
        val lockStates = getStateAndRefs<ReIssuanceLock<SimpleDummyState>>(aliceNode,
            accountUUID = employeeAliceAccount.identifier.id)
        val reIssuanceRequests = getStateAndRefs<ReIssuanceRequest>(aliceNode,
            accountUUID = employeeAliceAccount.identifier.id)

        assertThat(encumberedStates, empty())
        assertThat(unencumberedStates, hasSize(`is`(1)))
        assertThat(lockStates, empty())
        assertThat(reIssuanceRequests, empty())
    }

}
