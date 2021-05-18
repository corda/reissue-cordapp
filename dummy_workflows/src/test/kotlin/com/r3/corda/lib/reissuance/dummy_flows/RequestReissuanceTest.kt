package com.r3.corda.lib.reissuance.dummy_flows

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import com.r3.corda.lib.reissuance.dummy_contracts.DummyStateRequiringAcceptanceContract
import com.r3.corda.lib.reissuance.dummy_contracts.DummyStateRequiringAllParticipantsSignaturesContract
import com.r3.corda.lib.reissuance.dummy_contracts.SimpleDummyStateContract
import com.r3.corda.lib.reissuance.states.ReissuanceRequest
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

class RequestReissuanceTest: AbstractFlowTest() {

    private fun <T> verifyReissuanceRequests(
        reissuanceRequests: List<StateAndRef<ReissuanceRequest>>,
        expectedCommandData: CommandData,
        statesToBeReissued: List<StateAndRef<T>>,
        extraIssuanceCommandSigners: List<AbstractParty> = listOf(),
        issuerParty: AbstractParty = this.issuerParty,
        aliceParty: AbstractParty = this.aliceParty
    ) where T: ContractState {
        assertThat(reissuanceRequests, hasSize(`is`(1)))
        assertThat(reissuanceRequests[0].state.data.issuer.owningKey, `is`(issuerParty.owningKey))
        assertThat(reissuanceRequests[0].state.data.requester.owningKey, `is`(aliceParty.owningKey))
        assertThat(reissuanceRequests[0].state.data.assetIssuanceCommand, `is`(expectedCommandData))
        assertThat(reissuanceRequests[0].state.data.assetIssuanceSigners, `is`(
            listOf(issuerParty) + extraIssuanceCommandSigners))
        assertThat(reissuanceRequests[0].state.data.stateRefsToReissue, `is`(statesToBeReissued.map { it.ref }))
    }

    @Test
    fun `SimpleDummyState re-issuance request is created`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)

        val issuanceCommandData = SimpleDummyStateContract.Commands.Create()
        val statesToBeReissued = getStateAndRefs<SimpleDummyState>(aliceNode) // there is just 1 state
        createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            statesToBeReissued,
            issuanceCommandData,
            issuerParty
        )

        val reissuanceRequests = getStateAndRefs<ReissuanceRequest>(issuerNode)
        verifyReissuanceRequests(reissuanceRequests, issuanceCommandData, statesToBeReissued)

        val simpleDummyStatesAvailableToIssuer = getStateAndRefs<SimpleDummyState>(issuerNode)
        assertThat(simpleDummyStatesAvailableToIssuer, `is`(statesToBeReissued))
    }

    @Test
    fun `SimpleDummyState re-issuance request is created on notary 2`() {
        initialiseParties()
        createSimpleDummyStateOnNotary(aliceParty, notary2Party)

        val issuanceCommandData = SimpleDummyStateContract.Commands.Create()
        val statesToBeReissued = getStateAndRefs<SimpleDummyState>(aliceNode) // there is just 1 state
        createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            statesToBeReissued,
            issuanceCommandData,
            issuerParty
        )

        val reissuanceRequests = getStateAndRefs<ReissuanceRequest>(issuerNode)
        verifyReissuanceRequests(reissuanceRequests, issuanceCommandData, statesToBeReissued)

        val simpleDummyStatesAvailableToIssuer = getStateAndRefs<SimpleDummyState>(issuerNode)
        assertThat(simpleDummyStatesAvailableToIssuer, `is`(statesToBeReissued))
    }

    @Test
    fun `DummyStateRequiringAcceptance re-issuance request is created`() {
        initialiseParties()
        createDummyStateRequiringAcceptance(aliceParty)

        val statesToBeReissued = getStateAndRefs<DummyStateRequiringAcceptance>(aliceNode) // there is just 1 state
        val issuanceCommandData = DummyStateRequiringAcceptanceContract.Commands.Create()
        val extraIssuanceCommandSigners = listOf(acceptorParty)
        createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            statesToBeReissued,
            issuanceCommandData,
            issuerParty,
            extraIssuanceCommandSigners
        )

        val reissuanceRequests = getStateAndRefs<ReissuanceRequest>(issuerNode)
        verifyReissuanceRequests(reissuanceRequests, issuanceCommandData, statesToBeReissued, extraIssuanceCommandSigners)

        val dummyStatesRequiringAcceptanceAvailableToIssuer = getStateAndRefs<DummyStateRequiringAcceptance>(issuerNode)
        assertThat(dummyStatesRequiringAcceptanceAvailableToIssuer, `is`(statesToBeReissued))
    }

    @Test
    fun `DummyStateRequiringAllParticipantsSignatures re-issuance request is created`() {
        initialiseParties()
        createDummyStateRequiringAllParticipantsSignatures(aliceParty)

        val statesToBeReissued = getStateAndRefs<DummyStateRequiringAllParticipantsSignatures>(aliceNode)
        val issuanceCommandData = DummyStateRequiringAllParticipantsSignaturesContract.Commands.Create()
        val extraIssuanceCommandSigners = listOf(aliceParty, acceptorParty)
        createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            statesToBeReissued,
            issuanceCommandData,
            issuerParty,
            extraIssuanceCommandSigners
        )

        val reissuanceRequests = getStateAndRefs<ReissuanceRequest>(issuerNode)
        verifyReissuanceRequests(reissuanceRequests, issuanceCommandData, statesToBeReissued, extraIssuanceCommandSigners)

        val dummyStatesRequiringAllParticipantsSignaturesAvailableToIssuer = getStateAndRefs<DummyStateRequiringAllParticipantsSignatures>(
            issuerNode)
        assertThat(dummyStatesRequiringAllParticipantsSignaturesAvailableToIssuer, `is`(statesToBeReissued))
    }

    @Test
    fun `Tokens re-issuance request is created`() {
        initialiseParties()
        issueTokens(aliceParty, 50)

        val statesToBeReissued = getTokens(aliceNode)
        val issuanceCommandData = IssueTokenCommand(issuedTokenType, statesToBeReissued.indices.toList())
        createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            statesToBeReissued,
            issuanceCommandData,
            issuerParty
        )

        val reissuanceRequests = getStateAndRefs<ReissuanceRequest>(issuerNode)
        verifyReissuanceRequests(reissuanceRequests, issuanceCommandData, statesToBeReissued)

        val tokensAvailableToIssuer = getStateAndRefs<FungibleToken>(issuerNode)
        assertThat(tokensAvailableToIssuer, `is`(statesToBeReissued))
    }

    @Test
    fun `Tokens re-issuance request is created with many states`() {
        initialiseParties()
        issueTokens(aliceParty, 25)
        issueTokens(aliceParty, 25)

        val statesToBeReissued = getTokens(aliceNode)
        val issuanceCommandData = IssueTokenCommand(issuedTokenType, statesToBeReissued.indices.toList())
        createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            statesToBeReissued,
            issuanceCommandData,
            issuerParty
        )

        val reissuanceRequests = getStateAndRefs<ReissuanceRequest>(issuerNode)
        verifyReissuanceRequests(reissuanceRequests, issuanceCommandData, statesToBeReissued)

        val tokensAvailableToIssuer = getStateAndRefs<FungibleToken>(issuerNode)
        assertThat(tokensAvailableToIssuer, `is`(statesToBeReissued))
    }

    @Test
    fun `SimpleDummyState re-issuance request is created - accounts on the same host`() {
        initialisePartiesForAccountsOnTheSameHost()
        createSimpleDummyStateForAccount(employeeNode, employeeAliceParty)

        val statesToBeReissued = getStateAndRefs<SimpleDummyState>(employeeNode,
            accountUUID = employeeAliceAccount.identifier.id)
        val issuanceCommandData = SimpleDummyStateContract.Commands.Create()
        createReissuanceRequestAndShareRequiredTransactions(
            employeeNode,
            statesToBeReissued,
            SimpleDummyStateContract.Commands.Create(),
            employeeIssuerParty,
            requester = employeeAliceParty
        )

        val reissuanceRequests = getStateAndRefs<ReissuanceRequest>(employeeNode)
        verifyReissuanceRequests(reissuanceRequests, issuanceCommandData, statesToBeReissued,
            issuerParty = employeeIssuerParty, aliceParty = employeeAliceParty)

        val simpleDummyStatesAvailableToIssuer = getStateAndRefs<SimpleDummyState>(employeeNode) // available to node, not account
        assertThat(simpleDummyStatesAvailableToIssuer, `is`(statesToBeReissued))
    }

    @Test
    fun `SimpleDummyState re-issuance request is created - accounts on different hosts`() {
        initialisePartiesForAccountsOnDifferentHosts()
        createSimpleDummyStateForAccount(issuerNode, employeeAliceParty)

        val statesToBeReissued = getStateAndRefs<SimpleDummyState>(aliceNode,
            accountUUID = employeeAliceAccount.identifier.id)
        val issuanceCommandData = SimpleDummyStateContract.Commands.Create()
        createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            statesToBeReissued,
            issuanceCommandData,
            employeeIssuerParty,
            requester = employeeAliceParty
        )

        val reissuanceRequests = getStateAndRefs<ReissuanceRequest>(issuerNode)
        verifyReissuanceRequests(reissuanceRequests, issuanceCommandData, statesToBeReissued,
            issuerParty = employeeIssuerParty, aliceParty = employeeAliceParty)

        val simpleDummyStatesAvailableToIssuer = getStateAndRefs<SimpleDummyState>(issuerNode) // available to node, not account
        assertThat(simpleDummyStatesAvailableToIssuer, `is`(statesToBeReissued))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Request re-issuance of 0 states can't be created`() {
        initialiseParties()
        createReissuanceRequestAndShareRequiredTransactions<SimpleDummyState>(
            aliceNode,
            listOf(),
            SimpleDummyStateContract.Commands.Create(),
            issuerParty
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Issuer shouldn't be passed in as one of extraAssetIssuanceSigners`() { // issuer is a required signer
        initialiseParties()
        createSimpleDummyState(aliceParty)

        val issuanceCommandData = SimpleDummyStateContract.Commands.Create()
        val statesToBeReissued = getStateAndRefs<SimpleDummyState>(aliceNode) // there is just 1 state
        createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            statesToBeReissued,
            issuanceCommandData,
            issuerParty,
            extraCommandSigners = listOf(issuerParty)
        )
    }
}
