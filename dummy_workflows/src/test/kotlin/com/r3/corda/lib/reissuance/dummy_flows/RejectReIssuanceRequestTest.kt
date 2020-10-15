package com.r3.corda.lib.reissuance.dummy_flows

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
import net.corda.core.contracts.ContractState
import net.corda.testing.node.internal.TestStartedNode
import org.junit.Test
import java.util.*

class RejectReIssuanceRequestTest: AbstractFlowTest() {

    private inline fun <reified T> verifyReIssuanceRequestRejection(
        node: TestStartedNode = aliceNode,
        accountUUID: UUID? = null
    ) where T: ContractState {
        val encumberedStates = getStateAndRefs<T>(node, encumbered = true, accountUUID = accountUUID)
        val unencumberedStates = getStateAndRefs<T>(node, encumbered = false, accountUUID = accountUUID)
        val lockStates = getStateAndRefs<ReIssuanceLock<T>>(node, accountUUID = accountUUID)
        val reIssuanceRequests = getStateAndRefs<ReIssuanceRequest>(node, accountUUID = accountUUID)

        assertThat(encumberedStates, empty())
        assertThat(unencumberedStates, hasSize(`is`(1)))
        assertThat(lockStates, empty())
        assertThat(reIssuanceRequests, empty())
    }

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

        val reIssuanceRequest = getStateAndRefs<ReIssuanceRequest>(issuerNode)[0]
        rejectReIssuanceRequested<SimpleDummyState>(issuerNode, reIssuanceRequest)
        verifyReIssuanceRequestRejection<SimpleDummyState>()
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

        val reIssuanceRequest = getStateAndRefs<ReIssuanceRequest>(issuerNode)[0]
        rejectReIssuanceRequested<DummyStateRequiringAcceptance>(issuerNode, reIssuanceRequest)
        verifyReIssuanceRequestRejection<DummyStateRequiringAcceptance>()
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

        val reIssuanceRequest = getStateAndRefs<ReIssuanceRequest>(issuerNode)[0]
        rejectReIssuanceRequested<DummyStateRequiringAllParticipantsSignatures>(issuerNode, reIssuanceRequest)
        verifyReIssuanceRequestRejection<DummyStateRequiringAllParticipantsSignatures>()
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

        val reIssuanceRequest = getStateAndRefs<ReIssuanceRequest>(issuerNode)[0]
        rejectReIssuanceRequested<FungibleToken>(issuerNode, reIssuanceRequest)
        verifyReIssuanceRequestRejection<FungibleToken>()
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

        val reIssuanceRequest = getStateAndRefs<ReIssuanceRequest>(employeeNode)[0]
        rejectReIssuanceRequested<SimpleDummyState>(employeeNode, reIssuanceRequest)
        verifyReIssuanceRequestRejection<SimpleDummyState>(accountUUID = employeeAliceAccount.identifier.id,
            node = employeeNode)
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

        val reIssuanceRequest = getStateAndRefs<ReIssuanceRequest>(aliceNode)[0]
        rejectReIssuanceRequested<SimpleDummyState>(issuerNode, reIssuanceRequest)
        verifyReIssuanceRequestRejection<SimpleDummyState>(accountUUID = employeeAliceAccount.identifier.id)
    }

}
