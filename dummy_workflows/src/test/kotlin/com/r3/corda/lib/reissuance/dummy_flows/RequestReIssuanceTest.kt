package com.r3.corda.lib.reissuance.dummy_flows

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
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.identity.AbstractParty
import org.junit.Test

class RequestReIssuanceTest: AbstractFlowTest() {

    private fun <T> verifyReIssuanceRequests(
        reIssuanceRequests: List<StateAndRef<ReIssuanceRequest>>,
        expectedCommandData: CommandData,
        statesToBeReIssued: List<StateAndRef<T>>,
        extraIssuanceCommandSigners: List<AbstractParty> = listOf(),
        issuerParty: AbstractParty = this.issuerParty,
        aliceParty: AbstractParty = this.aliceParty
    ) where T: ContractState {
        assertThat(reIssuanceRequests, hasSize(`is`(1)))
        assertThat(reIssuanceRequests[0].state.data.issuer.owningKey, `is`(issuerParty.owningKey))
        assertThat(reIssuanceRequests[0].state.data.requester.owningKey, `is`(aliceParty.owningKey))
        assertThat(reIssuanceRequests[0].state.data.assetIssuanceCommand, `is`(expectedCommandData))
        assertThat(reIssuanceRequests[0].state.data.assetIssuanceSigners, `is`(
            listOf(issuerParty) + extraIssuanceCommandSigners))
        assertThat(reIssuanceRequests[0].state.data.stateRefsToReIssue, `is`(statesToBeReIssued.map { it.ref }))
    }

    @Test
    fun `SimpleDummyState re-issuance request is created`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)

        val issuanceCommandData = SimpleDummyStateContract.Commands.Create()
        val statesToBeReIssued = getStateAndRefs<SimpleDummyState>(aliceNode) // there is just 1 state
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            statesToBeReIssued,
            issuanceCommandData,
            issuerParty
        )

        val reIssuanceRequests = getStateAndRefs<ReIssuanceRequest>(issuerNode)
        verifyReIssuanceRequests(reIssuanceRequests, issuanceCommandData, statesToBeReIssued)

        val simpleDummyStatesAvailableToIssuer = getStateAndRefs<SimpleDummyState>(issuerNode)
        assertThat(simpleDummyStatesAvailableToIssuer, hasSize(`is`(1)))
        assertThat(simpleDummyStatesAvailableToIssuer[0], `is`(statesToBeReIssued[0]))
    }

    @Test
    fun `DummyStateRequiringAcceptance re-issuance request is created`() {
        initialiseParties()
        createDummyStateRequiringAcceptance(aliceParty)

        val statesToBeReIssued = getStateAndRefs<DummyStateRequiringAcceptance>(aliceNode) // there is just 1 state
        val issuanceCommandData = DummyStateRequiringAcceptanceContract.Commands.Create()
        val extraIssuanceCommandSigners = listOf(acceptorParty)
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            statesToBeReIssued,
            issuanceCommandData,
            issuerParty,
            extraIssuanceCommandSigners
        )

        val reIssuanceRequests = getStateAndRefs<ReIssuanceRequest>(issuerNode)
        verifyReIssuanceRequests(reIssuanceRequests, issuanceCommandData, statesToBeReIssued, extraIssuanceCommandSigners)

        val dummyStatesRequiringAcceptanceAvailableToIssuer = getStateAndRefs<DummyStateRequiringAcceptance>(issuerNode)
        assertThat(dummyStatesRequiringAcceptanceAvailableToIssuer, hasSize(`is`(1)))
        assertThat(dummyStatesRequiringAcceptanceAvailableToIssuer[0], `is`(statesToBeReIssued[0]))
    }

    @Test
    fun `DummyStateRequiringAllParticipantsSignatures re-issuance request is created`() {
        initialiseParties()
        createDummyStateRequiringAllParticipantsSignatures(aliceParty)

        val statesToBeReIssued = getStateAndRefs<DummyStateRequiringAllParticipantsSignatures>(aliceNode)
        val issuanceCommandData = DummyStateRequiringAllParticipantsSignaturesContract.Commands.Create()
        val extraIssuanceCommandSigners = listOf(aliceParty, acceptorParty)
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            statesToBeReIssued,
            issuanceCommandData,
            issuerParty,
            extraIssuanceCommandSigners
        )

        val reIssuanceRequests = getStateAndRefs<ReIssuanceRequest>(issuerNode)
        verifyReIssuanceRequests(reIssuanceRequests, issuanceCommandData, statesToBeReIssued, extraIssuanceCommandSigners)

        val dummyStatesRequiringAllParticipantsSignaturesAvailableToIssuer = getStateAndRefs<DummyStateRequiringAllParticipantsSignatures>(
            issuerNode)
        assertThat(dummyStatesRequiringAllParticipantsSignaturesAvailableToIssuer, hasSize(`is`(1)))
        assertThat(dummyStatesRequiringAllParticipantsSignaturesAvailableToIssuer[0], `is`(statesToBeReIssued[0]))
    }

    @Test
    fun `Tokens re-issuance request is created`() {
        initialiseParties()
        issueTokens(aliceParty, 50)

        val statesToBeReIssued = getTokens(aliceNode)
        val issuanceCommandData = IssueTokenCommand(issuedTokenType, statesToBeReIssued.indices.toList())
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            statesToBeReIssued,
            issuanceCommandData,
            issuerParty
        )

        val reIssuanceRequests = getStateAndRefs<ReIssuanceRequest>(issuerNode)
        verifyReIssuanceRequests(reIssuanceRequests, issuanceCommandData, statesToBeReIssued)

        val tokensAvailableToIssuer = getStateAndRefs<FungibleToken>(issuerNode)
        assertThat(tokensAvailableToIssuer, hasSize(`is`(1)))
        assertThat(tokensAvailableToIssuer, `is`(statesToBeReIssued))
    }

    @Test
    fun `Tokens re-issuance request is created with many states`() {
        initialiseParties()
        issueTokens(aliceParty, 25)
        issueTokens(aliceParty, 25)

        val statesToBeReIssued = getTokens(aliceNode)
        val issuanceCommandData = IssueTokenCommand(issuedTokenType, statesToBeReIssued.indices.toList())
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            statesToBeReIssued,
            issuanceCommandData,
            issuerParty
        )

        val reIssuanceRequests = getStateAndRefs<ReIssuanceRequest>(issuerNode)
        verifyReIssuanceRequests(reIssuanceRequests, issuanceCommandData, statesToBeReIssued)

        val tokensAvailableToIssuer = getStateAndRefs<FungibleToken>(issuerNode)
        assertThat(tokensAvailableToIssuer, hasSize(`is`(2)))
        assertThat(tokensAvailableToIssuer, `is`(statesToBeReIssued))
    }

    @Test
    fun `SimpleDummyState re-issuance request is created - accounts on the same host`() {
        initialisePartiesForAccountsOnTheSameHost()
        createSimpleDummyStateForAccount(employeeNode, employeeAliceParty)

        val statesToBeReIssued = getStateAndRefs<SimpleDummyState>(employeeNode,
            accountUUID = employeeAliceAccount.identifier.id)
        val issuanceCommandData = SimpleDummyStateContract.Commands.Create()
        createReIssuanceRequestAndShareRequiredTransactions(
            employeeNode,
            statesToBeReIssued,
            SimpleDummyStateContract.Commands.Create(),
            employeeIssuerParty,
            requester = employeeAliceParty
        )

        val reIssuanceRequests = getStateAndRefs<ReIssuanceRequest>(employeeNode)
        verifyReIssuanceRequests(reIssuanceRequests, issuanceCommandData, statesToBeReIssued,
            issuerParty = employeeIssuerParty, aliceParty = employeeAliceParty)

        val simpleDummyStatesAvailableToIssuer = getStateAndRefs<SimpleDummyState>(employeeNode) // available to node, not account
        assertThat(simpleDummyStatesAvailableToIssuer, hasSize(`is`(1)))
        assertThat(simpleDummyStatesAvailableToIssuer, `is`(statesToBeReIssued))
    }

    @Test
    fun `SimpleDummyState re-issuance request is created - accounts on different hosts`() {
        initialisePartiesForAccountsOnDifferentHosts()
        createSimpleDummyStateForAccount(issuerNode, employeeAliceParty)

        val statesToBeReIssued = getStateAndRefs<SimpleDummyState>(aliceNode,
            accountUUID = employeeAliceAccount.identifier.id)
        val issuanceCommandData = SimpleDummyStateContract.Commands.Create()
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            statesToBeReIssued,
            issuanceCommandData,
            employeeIssuerParty,
            requester = employeeAliceParty
        )

        val reIssuanceRequests = getStateAndRefs<ReIssuanceRequest>(issuerNode)
        verifyReIssuanceRequests(reIssuanceRequests, issuanceCommandData, statesToBeReIssued,
            issuerParty = employeeIssuerParty, aliceParty = employeeAliceParty)

        val simpleDummyStatesAvailableToIssuer = getStateAndRefs<SimpleDummyState>(issuerNode) // available to node, not account
        assertThat(simpleDummyStatesAvailableToIssuer, hasSize(`is`(1)))
        assertThat(simpleDummyStatesAvailableToIssuer, `is`(statesToBeReIssued))
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
