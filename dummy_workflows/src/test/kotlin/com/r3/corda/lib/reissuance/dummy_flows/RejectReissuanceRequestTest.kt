package com.r3.corda.lib.reissuance.dummy_flows

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import com.r3.corda.lib.reissuance.dummy_contracts.DummyStateRequiringAcceptanceContract
import com.r3.corda.lib.reissuance.dummy_contracts.DummyStateRequiringAllParticipantsSignaturesContract
import com.r3.corda.lib.reissuance.dummy_contracts.SimpleDummyStateContract
import com.r3.corda.lib.reissuance.dummy_states.DummyStateRequiringAcceptance
import com.r3.corda.lib.reissuance.dummy_states.DummyStateRequiringAllParticipantsSignatures
import com.r3.corda.lib.reissuance.dummy_states.SimpleDummyState
import com.r3.corda.lib.reissuance.states.ReissuanceLock
import com.r3.corda.lib.reissuance.states.ReissuanceRequest
import com.r3.corda.lib.tokens.contracts.commands.RedeemTokenCommand
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import net.corda.core.contracts.ContractState
import net.corda.testing.node.internal.TestStartedNode
import org.junit.Test
import java.util.*

class RejectReissuanceRequestTest: AbstractFlowTest() {

    private inline fun <reified T> verifyReissuanceRequestRejection(
        node: TestStartedNode = aliceNode,
        accountUUID: UUID? = null
    ) where T: ContractState {
        val encumberedStates = getStateAndRefs<T>(node, encumbered = true, accountUUID = accountUUID)
        val unencumberedStates = getStateAndRefs<T>(node, encumbered = false, accountUUID = accountUUID)
        val lockStates = getStateAndRefs<ReissuanceLock>(node, accountUUID = accountUUID)
        val reissuanceRequests = getStateAndRefs<ReissuanceRequest>(node, accountUUID = accountUUID)

        assertThat(encumberedStates, empty())
        assertThat(unencumberedStates, hasSize(`is`(1)))
        assertThat(lockStates, empty())
        assertThat(reissuanceRequests, empty())
    }

    @Test
    fun `SimpleDummyState re-issuance request is rejected`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)

        val simpleDummyState = getStateAndRefs<SimpleDummyState>(aliceNode)[0]
        createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(simpleDummyState),
            SimpleDummyStateContract.Commands.Delete(),
            issuerParty
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]
        rejectReissuanceRequested<SimpleDummyState>(issuerNode, reissuanceRequest)
        verifyReissuanceRequestRejection<SimpleDummyState>()
    }

    @Test
    fun `SimpleDummyState re-issuance request is rejected on notary2`() {
        initialiseParties()
        createSimpleDummyStateOnNotary(aliceParty, notary2Party)

        val simpleDummyState = getStateAndRefs<SimpleDummyState>(aliceNode)[0]
        createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(simpleDummyState),
            SimpleDummyStateContract.Commands.Delete(),
            issuerParty
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]
        rejectReissuanceRequested<SimpleDummyState>(issuerNode, reissuanceRequest)
        verifyReissuanceRequestRejection<SimpleDummyState>()
    }

    @Test
    fun `DummyStateRequiringAcceptance re-issuance request is rejected`() {
        initialiseParties()
        createDummyStateRequiringAcceptance(aliceParty)

        val dummyStateRequiringAcceptance = getStateAndRefs<DummyStateRequiringAcceptance>(aliceNode)[0]
        createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(dummyStateRequiringAcceptance),
            DummyStateRequiringAcceptanceContract.Commands.Delete(),
            issuerParty,
            listOf(acceptorParty)
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]
        rejectReissuanceRequested<DummyStateRequiringAcceptance>(issuerNode, reissuanceRequest)
        verifyReissuanceRequestRejection<DummyStateRequiringAcceptance>()
    }

    @Test
    fun `DummyStateRequiringAllParticipantsSignatures re-issuance request is rejected`() {
        initialiseParties()
        createDummyStateRequiringAllParticipantsSignatures(aliceParty)

        val dummyStateRequiringAllParticipantsSignatures = getStateAndRefs<DummyStateRequiringAllParticipantsSignatures>(aliceNode)[0]
        createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(dummyStateRequiringAllParticipantsSignatures),
            DummyStateRequiringAllParticipantsSignaturesContract.Commands.Delete(),
            issuerParty,
            listOf(aliceParty, acceptorParty)
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]
        rejectReissuanceRequested<DummyStateRequiringAllParticipantsSignatures>(issuerNode, reissuanceRequest)
        verifyReissuanceRequestRejection<DummyStateRequiringAllParticipantsSignatures>()
    }

    @Test
    fun `Token re-issuance request is rejected`() {
        initialiseParties()
        issueTokens(aliceParty, 50)

        val tokens = getTokens(aliceNode)

        createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            tokens,
            RedeemTokenCommand(issuedTokenType, tokens.indices.toList()),
            issuerParty
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]
        rejectReissuanceRequested<FungibleToken>(issuerNode, reissuanceRequest)
        verifyReissuanceRequestRejection<FungibleToken>()
    }

    @Test
    fun `SimpleDummyState re-issuance request is rejected - accounts on the same host`() {
        initialisePartiesForAccountsOnTheSameHost()
        createSimpleDummyStateForAccount(employeeNode, employeeAliceParty)

        val simpleDummyState = getStateAndRefs<SimpleDummyState>(employeeNode,
            accountUUID = employeeAliceAccount.identifier.id)[0]
        createReissuanceRequestAndShareRequiredTransactions(
            employeeNode,
            listOf(simpleDummyState),
            SimpleDummyStateContract.Commands.Delete(),
            employeeIssuerParty,
            requester = employeeAliceParty
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(employeeNode)[0]
        rejectReissuanceRequested<SimpleDummyState>(employeeNode, reissuanceRequest)
        verifyReissuanceRequestRejection<SimpleDummyState>(accountUUID = employeeAliceAccount.identifier.id,
            node = employeeNode)
    }

    @Test
    fun `SimpleDummyState re-issuance request is rejected - accounts on different hosts`() {
        initialisePartiesForAccountsOnDifferentHosts()
        createSimpleDummyStateForAccount(issuerNode, employeeAliceParty)

        val simpleDummyState = getStateAndRefs<SimpleDummyState>(aliceNode)[0]
        createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(simpleDummyState),
            SimpleDummyStateContract.Commands.Delete(),
            employeeIssuerParty,
            requester = employeeAliceParty
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(aliceNode)[0]
        rejectReissuanceRequested<SimpleDummyState>(issuerNode, reissuanceRequest)
        verifyReissuanceRequestRejection<SimpleDummyState>(accountUUID = employeeAliceAccount.identifier.id)
    }

}
