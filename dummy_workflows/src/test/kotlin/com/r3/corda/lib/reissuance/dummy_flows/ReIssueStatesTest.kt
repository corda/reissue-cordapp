package com.r3.corda.lib.reissuance.dummy_flows

import com.r3.corda.lib.reissuance.dummy_contracts.DummyStateRequiringAcceptanceContract
import com.r3.corda.lib.reissuance.dummy_contracts.DummyStateRequiringAllParticipantsSignaturesContract
import com.r3.corda.lib.reissuance.dummy_contracts.SimpleDummyStateContract
import com.r3.corda.lib.reissuance.states.ReIssuanceLock
import com.r3.corda.lib.reissuance.states.ReIssuanceRequest
import com.r3.corda.lib.reissuance.dummy_states.DummyStateRequiringAcceptance
import com.r3.corda.lib.reissuance.dummy_states.DummyStateRequiringAllParticipantsSignatures
import com.r3.corda.lib.reissuance.dummy_states.SimpleDummyState
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import com.r3.corda.lib.tokens.contracts.commands.IssueTokenCommand
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.identity.AbstractParty
import net.corda.testing.node.internal.TestStartedNode
import org.junit.Test
import java.util.*

class ReIssueStatesTest: AbstractFlowTest() {

    private inline fun <reified T> verifyStatesAfterReIssuance(
        node: TestStartedNode = aliceNode,
        accountUUID: UUID? = null,
        issuerParty: AbstractParty = this.issuerParty,
        aliceParty: AbstractParty = this.aliceParty,
        statesNum: Int = 1
    ) where T: ContractState {
        val encumberedStates = getStateAndRefs<T>(node, encumbered = true, accountUUID = accountUUID)
        val unencumberedStates = getStateAndRefs<T>(node, encumbered = false, accountUUID = accountUUID)
        val lockStates = getStateAndRefs<ReIssuanceLock<T>>(node, encumbered = true,
            accountUUID = accountUUID)

        assertThat(encumberedStates, hasSize(`is`(statesNum)))
        assertThat(unencumberedStates, hasSize(`is`(statesNum)))
        assertThat(lockStates, hasSize(`is`(1)))
        assertThat(encumberedStates.map { it.state.data }, `is`(unencumberedStates.map { it.state.data }))
        assertThat(lockStates[0].state.data.issuer, `is`(issuerParty))
        assertThat(lockStates[0].state.data.requester, `is`(aliceParty))
        assertThat(lockStates[0].state.data.originalStates, `is`(unencumberedStates))
    }

    @Test
    fun `SimpleDummyState is re-issued`() {
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
        reIssueRequestedStates<SimpleDummyState>(issuerNode, reIssuanceRequest,
            issuerIsRequiredExitCommandSigner = false)
        verifyStatesAfterReIssuance<SimpleDummyState>()
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

        val reIssuanceRequest = getStateAndRefs<ReIssuanceRequest>(issuerNode)[0]
        reIssueRequestedStates<DummyStateRequiringAcceptance>(issuerNode, reIssuanceRequest,
            issuerIsRequiredExitCommandSigner = false)
        verifyStatesAfterReIssuance<DummyStateRequiringAcceptance>()
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

        val reIssuanceRequest = getStateAndRefs<ReIssuanceRequest>(issuerNode)[0]
        reIssueRequestedStates<DummyStateRequiringAllParticipantsSignatures>(issuerNode, reIssuanceRequest,
            issuerIsRequiredExitCommandSigner = true)
        verifyStatesAfterReIssuance<DummyStateRequiringAllParticipantsSignatures>()
    }

    @Test
    fun `Token is re-issued`() {
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
        reIssueRequestedStates<FungibleToken>(issuerNode, reIssuanceRequest,
            issuerIsRequiredExitCommandSigner = true)
        verifyStatesAfterReIssuance<FungibleToken>()
    }

    @Test
    fun `Tokens are re-issued`() {
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

        val reIssuanceRequest = getStateAndRefs<ReIssuanceRequest>(issuerNode)[0]
        reIssueRequestedStates<FungibleToken>(issuerNode, reIssuanceRequest,
            issuerIsRequiredExitCommandSigner = true)
        verifyStatesAfterReIssuance<FungibleToken>(statesNum = 2)
    }

    @Test
    fun `SimpleDummyState re-issued - accounts on the same host`() {
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
        reIssueRequestedStates<SimpleDummyState>(employeeNode, reIssuanceRequest,
            issuerIsRequiredExitCommandSigner = true)
        verifyStatesAfterReIssuance<SimpleDummyState>(accountUUID = employeeAliceAccount.identifier.id,
            node = employeeNode, issuerParty = employeeIssuerParty, aliceParty = employeeAliceParty)
    }

    @Test
    fun `SimpleDummyState re-issued - accounts on different hosts`() {
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
        reIssueRequestedStates<SimpleDummyState>(issuerNode, reIssuanceRequest,
            issuerIsRequiredExitCommandSigner = false)
        verifyStatesAfterReIssuance<SimpleDummyState>(accountUUID = employeeAliceAccount.identifier.id,
            issuerParty = employeeIssuerParty, aliceParty = employeeAliceParty)
    }

    @Test(expected = IllegalArgumentException::class) // issues find state by reference
    fun `State cannot be re-issued if issuer cannot access it`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)

        val simpleDummyStateRef = getStateAndRefs<SimpleDummyState>(aliceNode)[0].ref
        createReIssuanceRequest<SimpleDummyState>( // don't share required transactions
            aliceNode,
            listOf(simpleDummyStateRef),
            SimpleDummyStateContract.Commands.Create(),
            issuerParty
        )

        val reIssuanceRequest = getStateAndRefs<ReIssuanceRequest>(issuerNode)[0]
        reIssueRequestedStates<SimpleDummyState>(issuerNode, reIssuanceRequest,
            issuerIsRequiredExitCommandSigner = false)
    }

    @Test // issuer doesn't have an information about state being consumed
    fun `Consumed state is re-issued when issuer is not a participant`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)

        val simpleDummyState = getStateAndRefs<SimpleDummyState>(aliceNode)[0]
        updateSimpleDummyState(aliceNode, bobParty)

        createReIssuanceRequest<SimpleDummyState>(
            aliceNode,
            listOf(simpleDummyState.ref),
            SimpleDummyStateContract.Commands.Create(),
            issuerParty
        )
        shareTransaction(aliceNode, issuerParty, simpleDummyState.ref.txhash)

        val reIssuanceRequest = getStateAndRefs<ReIssuanceRequest>(issuerNode)[0]
        reIssueRequestedStates<SimpleDummyState>(issuerNode, reIssuanceRequest,
            issuerIsRequiredExitCommandSigner = false)
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

        val reIssuanceRequest = getStateAndRefs<ReIssuanceRequest>(issuerNode)[0]
        reIssueRequestedStates<DummyStateRequiringAcceptance>(issuerNode, reIssuanceRequest,
            issuerIsRequiredExitCommandSigner = false)
    }

    @Test
    fun `Another party can re-issue SimpleDummyState because it doesn't store issuer information`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)

        val simpleDummyState = getStateAndRefs<SimpleDummyState>(aliceNode)[0]
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(simpleDummyState),
            SimpleDummyStateContract.Commands.Create(),
            bobParty
        )

        val reIssuanceRequest = getStateAndRefs<ReIssuanceRequest>(bobNode)[0]
        reIssueRequestedStates<SimpleDummyState>(bobNode, reIssuanceRequest,
            issuerIsRequiredExitCommandSigner = false)
        verifyStatesAfterReIssuance<SimpleDummyState>(issuerParty = bobParty)
    }

    @Test(expected = TransactionVerificationException::class)
    fun `Another party can't re-issue DummyStateRequiringAcceptance as issuer is a participant`() {
        initialiseParties()
        createDummyStateRequiringAcceptance(aliceParty)

        val dummyStateRequiringAcceptance = getStateAndRefs<DummyStateRequiringAcceptance>(aliceNode)[0]
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(dummyStateRequiringAcceptance),
            DummyStateRequiringAcceptanceContract.Commands.Create(),
            bobParty,
            listOf(bobParty, acceptorParty)
        )

        val reIssuanceRequest = getStateAndRefs<ReIssuanceRequest>(bobNode)[0]
        reIssueRequestedStates<DummyStateRequiringAcceptance>(bobNode, reIssuanceRequest,
            issuerIsRequiredExitCommandSigner = false)
    }

    @Test(expected = TransactionVerificationException::class)
    fun `Another party can't re-issue DummyStateRequiringAllParticipantsSignatures as issuer is a participant`() {
        initialiseParties()
        createDummyStateRequiringAllParticipantsSignatures(aliceParty)

        val dummyStateRequiringAllParticipantsSignatures = getStateAndRefs<DummyStateRequiringAllParticipantsSignatures>(aliceNode)[0]
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(dummyStateRequiringAllParticipantsSignatures),
            DummyStateRequiringAllParticipantsSignaturesContract.Commands.Create(),
            bobParty,
            listOf(aliceParty, bobParty, acceptorParty)
        )

        val reIssuanceRequest = getStateAndRefs<ReIssuanceRequest>(bobNode)[0]
        reIssueRequestedStates<DummyStateRequiringAllParticipantsSignatures>(bobNode, reIssuanceRequest,
            issuerIsRequiredExitCommandSigner = true)
    }

    @Test(expected = TransactionVerificationException::class)
    fun `Another party can't re-issue tokens as issuer information is stored in IssuedTokenType`() {
        initialiseParties()
        issueTokens(aliceParty, 50)

        val tokens = getTokens(aliceNode)

        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            tokens,
            IssueTokenCommand(issuedTokenType, tokens.indices.toList()),
            bobParty
        )

        val reIssuanceRequest = getStateAndRefs<ReIssuanceRequest>(bobNode)[0]
        reIssueRequestedStates<FungibleToken>(bobNode, reIssuanceRequest,
            issuerIsRequiredExitCommandSigner = true)

    }

    @Test(expected = IllegalArgumentException::class)
    fun `State can't be re-issued twice`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)

        val simpleDummyState = getStateAndRefs<SimpleDummyState>(aliceNode)[0]
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(simpleDummyState),
            SimpleDummyStateContract.Commands.Create(),
            issuerParty
        )
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(simpleDummyState),
            SimpleDummyStateContract.Commands.Create(),
            issuerParty
        )

        val reIssuanceRequest1 = getStateAndRefs<ReIssuanceRequest>(issuerNode)[0]
        val reIssuanceRequest2 = getStateAndRefs<ReIssuanceRequest>(issuerNode)[1]

        reIssueRequestedStates<SimpleDummyState>(issuerNode, reIssuanceRequest1,
            issuerIsRequiredExitCommandSigner = false)
        reIssueRequestedStates<SimpleDummyState>(issuerNode, reIssuanceRequest2,
            issuerIsRequiredExitCommandSigner = false)
    }
}
