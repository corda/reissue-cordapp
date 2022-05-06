package com.r3.corda.lib.reissuance.dummy_flows

import com.r3.corda.lib.reissuance.dummy_contracts.DummyStateRequiringAcceptanceContract
import com.r3.corda.lib.reissuance.dummy_contracts.DummyStateRequiringAllParticipantsSignaturesContract
import com.r3.corda.lib.reissuance.dummy_contracts.SimpleDummyStateContract
import com.r3.corda.lib.reissuance.dummy_states.DummyStateRequiringAcceptance
import com.r3.corda.lib.reissuance.dummy_states.DummyStateRequiringAllParticipantsSignatures
import com.r3.corda.lib.reissuance.dummy_states.SimpleDummyState
import com.r3.corda.lib.reissuance.states.ReissuanceLock
import com.r3.corda.lib.reissuance.states.ReissuanceRequest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import com.r3.corda.lib.tokens.contracts.commands.IssueTokenCommand
import com.r3.corda.lib.tokens.contracts.commands.RedeemTokenCommand
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import net.corda.testing.node.internal.TestStartedNode
import org.junit.Test
import java.util.*
import kotlin.test.assertTrue

class ReissueStatesTest: AbstractFlowTest() {

    private inline fun <reified T> verifyStatesAfterReissuance(
        node: TestStartedNode = aliceNode,
        accountUUID: UUID? = null,
        issuerParty: AbstractParty = this.issuerParty,
        aliceParty: AbstractParty = this.aliceParty,
        statesNum: Int = 1,
        deleteTxAttachmentId: SecureHash
    ) where T: ContractState {
        val encumberedStates = getStateAndRefs<T>(node, encumbered = true, accountUUID = accountUUID)
        val unencumberedStates = getStateAndRefs<T>(node, encumbered = false, accountUUID = accountUUID)
        val lockStates = getStateAndRefs<ReissuanceLock>(node, encumbered = true,
            accountUUID = accountUUID)
        val deleteTx = getAttachedWireTransaction(node, deleteTxAttachmentId)

        assertThat(encumberedStates, hasSize(`is`(statesNum)))
        assertThat(unencumberedStates, hasSize(`is`(statesNum)))
        assertThat(lockStates, hasSize(`is`(1)))
        assertThat(encumberedStates.map { it.state.data }, `is`(unencumberedStates.map { it.state.data }))
        assertThat(lockStates[0].state.data.issuer, `is`(issuerParty))
        assertThat(lockStates[0].state.data.requester, `is`(aliceParty))
        assertTrue(deleteTx.inputs.map { node.services.toStateAndRef<ContractState>(it) }.containsAll(unencumberedStates))
    }

    @Test
    fun `SimpleDummyState is re-issued`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)

        val simpleDummyState = getStateAndRefs<SimpleDummyState>(aliceNode)[0]
        val requestReissuanceTransactionId = createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(simpleDummyState),
            SimpleDummyStateContract.Commands.Delete(),
            issuerParty
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]
        reissueRequestedStates<SimpleDummyState>(issuerNode, reissuanceRequest, SimpleDummyStateContract.Commands.Create(),
            listOf(), requestReissuanceTransactionId)
        verifyStatesAfterReissuance<SimpleDummyState>(deleteTxAttachmentId = requestReissuanceTransactionId)
    }

    @Test
    fun `SimpleDummyState is re-issued on notary 2`() {
        initialiseParties()
        createSimpleDummyStateOnNotary(aliceParty, notary2Party)

        val simpleDummyState = getStateAndRefs<SimpleDummyState>(aliceNode)[0]
        val requestReissuanceTransactionId = createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(simpleDummyState),
            SimpleDummyStateContract.Commands.Delete(),
            issuerParty
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]
        reissueRequestedStates<SimpleDummyState>(issuerNode, reissuanceRequest, SimpleDummyStateContract.Commands.Create(),
            listOf(), requestReissuanceTransactionId)
        verifyStatesAfterReissuance<SimpleDummyState>(deleteTxAttachmentId = requestReissuanceTransactionId)
    }

    @Test
    fun `DummyStateRequiringAcceptance is re-issued`() {
        initialiseParties()
        createDummyStateRequiringAcceptance(aliceParty)

        val dummyStateRequiringAcceptance = getStateAndRefs<DummyStateRequiringAcceptance>(aliceNode)[0]
        val requestReissuanceTransactionId = createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(dummyStateRequiringAcceptance),
            DummyStateRequiringAcceptanceContract.Commands.Delete(),
            issuerParty,
            listOf(acceptorParty)
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]
        reissueRequestedStates<DummyStateRequiringAcceptance>(issuerNode, reissuanceRequest,
            DummyStateRequiringAcceptanceContract.Commands.Create(), listOf(acceptorParty, issuerParty),
            requestReissuanceTransactionId)
        verifyStatesAfterReissuance<DummyStateRequiringAcceptance>(deleteTxAttachmentId = requestReissuanceTransactionId)
    }

    @Test
    fun `DummyStateRequiringAllParticipantsSignatures is re-issued`() {
        initialiseParties()
        createDummyStateRequiringAllParticipantsSignatures(aliceParty)

        val dummyStateRequiringAllParticipantsSignatures = getStateAndRefs<DummyStateRequiringAllParticipantsSignatures>(aliceNode)[0]
        val requestReissuanceTransactionId = createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(dummyStateRequiringAllParticipantsSignatures),
            DummyStateRequiringAllParticipantsSignaturesContract.Commands.Delete(),
            issuerParty,
            listOf(aliceParty, acceptorParty)
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]
        reissueRequestedStates<DummyStateRequiringAllParticipantsSignatures>(issuerNode, reissuanceRequest,
            DummyStateRequiringAllParticipantsSignaturesContract.Commands.Create(),
            listOf(issuerParty, acceptorParty), requestReissuanceTransactionId)
        verifyStatesAfterReissuance<DummyStateRequiringAllParticipantsSignatures>(deleteTxAttachmentId = requestReissuanceTransactionId)
    }

    @Test
    fun `Token is re-issued`() {
        initialiseParties()
        issueTokens(aliceParty, 50)

        val tokens = getTokens(aliceNode)

        val requestReissuanceTransactionId = createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            tokens,
            RedeemTokenCommand(issuedTokenType, tokens.indices.toList()),
            issuerParty
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]
        reissueRequestedStates<FungibleToken>(issuerNode, reissuanceRequest, IssueTokenCommand(issuedTokenType, tokens.indices.toList()),
            listOf(issuerParty), requestReissuanceTransactionId)
        verifyStatesAfterReissuance<FungibleToken>(deleteTxAttachmentId = requestReissuanceTransactionId)
    }

    @Test
    fun `Tokens are re-issued`() {
        initialiseParties()
        issueTokens(aliceParty, 25)
        issueTokens(aliceParty, 25)

        val tokens = getTokens(aliceNode)

        val requestReissuanceTransactionId = createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            tokens,
            RedeemTokenCommand(issuedTokenType, tokens.indices.toList()),
            issuerParty
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]
        reissueRequestedStates<FungibleToken>(issuerNode, reissuanceRequest, IssueTokenCommand(issuedTokenType,
            tokens.indices.toList()), listOf(issuerParty), requestReissuanceTransactionId)
        verifyStatesAfterReissuance<FungibleToken>(statesNum = 2, deleteTxAttachmentId = requestReissuanceTransactionId)
    }

    @Test
    fun `SimpleDummyState re-issued - accounts on the same host`() {
        initialisePartiesForAccountsOnTheSameHost()
        createSimpleDummyStateForAccount(employeeNode, employeeAliceParty)

        val simpleDummyState = getStateAndRefs<SimpleDummyState>(employeeNode,
            accountUUID = employeeAliceAccount.identifier.id)[0]
        val requestReissuanceTransactionId = createReissuanceRequestAndShareRequiredTransactions(
            employeeNode,
            listOf(simpleDummyState),
            SimpleDummyStateContract.Commands.Delete(),
            employeeIssuerParty,
            requester = employeeAliceParty
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(employeeNode)[0]
        reissueRequestedStates<SimpleDummyState>(employeeNode, reissuanceRequest, SimpleDummyStateContract.Commands
            .Create(), listOf(), requestReissuanceTransactionId)
        verifyStatesAfterReissuance<SimpleDummyState>(accountUUID = employeeAliceAccount.identifier.id,
            node = employeeNode, issuerParty = employeeIssuerParty, aliceParty = employeeAliceParty,
            deleteTxAttachmentId = requestReissuanceTransactionId)
    }

    @Test
    fun `SimpleDummyState re-issued - accounts on different hosts`() {
        initialisePartiesForAccountsOnDifferentHosts()
        createSimpleDummyStateForAccount(issuerNode, employeeAliceParty)

        val simpleDummyState = getStateAndRefs<SimpleDummyState>(aliceNode)[0]
        val requestReissuanceTransactionId = createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(simpleDummyState),
            SimpleDummyStateContract.Commands.Delete(),
            employeeIssuerParty,
            requester = employeeAliceParty
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(aliceNode)[0]
        reissueRequestedStates<SimpleDummyState>(issuerNode, reissuanceRequest, SimpleDummyStateContract.Commands
            .Create(), listOf(), requestReissuanceTransactionId)
        verifyStatesAfterReissuance<SimpleDummyState>(accountUUID = employeeAliceAccount.identifier.id,
            issuerParty = employeeIssuerParty, aliceParty = employeeAliceParty, deleteTxAttachmentId = requestReissuanceTransactionId)
    }

    @Test(expected = IllegalArgumentException::class) // issues find state by reference
    fun `State cannot be re-issued if issuer cannot access it`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)

        val simpleDummyStateRef = getStateAndRefs<SimpleDummyState>(aliceNode)[0].ref
        val requestReissuanceTransactionId = createReissuanceRequest<SimpleDummyState>( // don't share required transactions
            aliceNode,
            listOf(simpleDummyStateRef),
            SimpleDummyStateContract.Commands.Delete(),
            issuerParty
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]
        reissueRequestedStates<SimpleDummyState>(issuerNode, reissuanceRequest, SimpleDummyStateContract.Commands.Create(), listOf(),
            requestReissuanceTransactionId)
    }

    @Test // issuer doesn't have an information about state being consumed
    fun `Consumed state is re-issued when issuer is not a participant`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)

        val simpleDummyState = getStateAndRefs<SimpleDummyState>(aliceNode)[0]
        updateSimpleDummyState(aliceNode, bobParty)

        val requestReissuanceTransactionId = createReissuanceRequest<SimpleDummyState>(
            aliceNode,
            listOf(simpleDummyState.ref),
            SimpleDummyStateContract.Commands.Delete(),
            issuerParty
        )
        shareTransaction(aliceNode, issuerParty, simpleDummyState.ref.txhash)

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]
        reissueRequestedStates<SimpleDummyState>(issuerNode, reissuanceRequest, SimpleDummyStateContract.Commands.Create(),
            listOf(), requestReissuanceTransactionId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Consumed state isn't re-issued when issuer is a participant`() {
        initialiseParties()
        createDummyStateRequiringAcceptance(aliceParty)

        val dummyStateRequiringAcceptance = getStateAndRefs<DummyStateRequiringAcceptance>(aliceNode)[0]
        val requestReissuanceTransactionId = createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(dummyStateRequiringAcceptance),
            DummyStateRequiringAcceptanceContract.Commands.Delete(),
            issuerParty,
            listOf(acceptorParty)
        )

        updateDummyStateRequiringAcceptance(aliceNode, bobParty)

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]
        reissueRequestedStates<DummyStateRequiringAcceptance>(issuerNode, reissuanceRequest,
            DummyStateRequiringAcceptanceContract.Commands.Create(), listOf(), requestReissuanceTransactionId)
    }

    @Test
    fun `Another party can re-issue SimpleDummyState because it doesn't store issuer information`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)

        val simpleDummyState = getStateAndRefs<SimpleDummyState>(aliceNode)[0]
        val requestReissuanceTransactionId = createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(simpleDummyState),
            SimpleDummyStateContract.Commands.Delete(),
            bobParty
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(bobNode)[0]
        reissueRequestedStates<SimpleDummyState>(bobNode, reissuanceRequest, SimpleDummyStateContract.Commands.Create(),
            listOf(), requestReissuanceTransactionId)
        verifyStatesAfterReissuance<SimpleDummyState>(issuerParty = bobParty, deleteTxAttachmentId = requestReissuanceTransactionId)
    }

    @Test(expected = TransactionVerificationException::class)
    fun `Another party can't re-issue DummyStateRequiringAcceptance as issuer is a participant`() {
        initialiseParties()
        createDummyStateRequiringAcceptance(aliceParty)

        val dummyStateRequiringAcceptance = getStateAndRefs<DummyStateRequiringAcceptance>(aliceNode)[0]
        val requestReissuanceTransactionId = createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(dummyStateRequiringAcceptance),
            DummyStateRequiringAcceptanceContract.Commands.Delete(),
            bobParty,
            listOf(acceptorParty)
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(bobNode)[0]
        reissueRequestedStates<DummyStateRequiringAcceptance>(bobNode, reissuanceRequest,
            DummyStateRequiringAcceptanceContract.Commands.Create(), listOf(acceptorParty),
            requestReissuanceTransactionId)
    }

    @Test(expected = TransactionVerificationException::class)
    fun `Another party can't re-issue DummyStateRequiringAllParticipantsSignatures as issuer is a participant`() {
        initialiseParties()
        createDummyStateRequiringAllParticipantsSignatures(aliceParty)

        val dummyStateRequiringAllParticipantsSignatures = getStateAndRefs<DummyStateRequiringAllParticipantsSignatures>(aliceNode)[0]
        val requestReissuanceTransactionId = createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(dummyStateRequiringAllParticipantsSignatures),
            DummyStateRequiringAllParticipantsSignaturesContract.Commands.Delete(),
            bobParty,
            listOf(aliceParty, acceptorParty)
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(bobNode)[0]
        reissueRequestedStates<DummyStateRequiringAllParticipantsSignatures>(bobNode, reissuanceRequest,
            DummyStateRequiringAllParticipantsSignaturesContract.Commands.Create(), listOf(),
            requestReissuanceTransactionId)
    }

    @Test(expected = TransactionVerificationException::class)
    fun `Another party can't re-issue tokens as issuer information is stored in IssuedTokenType`() {
        initialiseParties()
        issueTokens(aliceParty, 50)

        val tokens = getTokens(aliceNode)

        val requestReissuanceTransactionId = createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            tokens,
            RedeemTokenCommand(issuedTokenType, tokens.indices.toList()),
            bobParty
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(bobNode)[0]
        reissueRequestedStates<FungibleToken>(bobNode, reissuanceRequest, IssueTokenCommand(issuedTokenType, tokens.indices.toList()),
            listOf(), requestReissuanceTransactionId)

    }

    @Test(expected = IllegalArgumentException::class)
    fun `State can't be re-issued twice`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)

        val simpleDummyState = getStateAndRefs<SimpleDummyState>(aliceNode)[0]
        val requestReissuanceTransactionId1 = createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(simpleDummyState),
            SimpleDummyStateContract.Commands.Delete(),
            issuerParty
        )
        val requestReissuanceTransactionId2 = createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(simpleDummyState),
            SimpleDummyStateContract.Commands.Delete(),
            issuerParty
        )

        val reissuanceRequest1 = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]
        val reissuanceRequest2 = getStateAndRefs<ReissuanceRequest>(issuerNode)[1]

        reissueRequestedStates<SimpleDummyState>(issuerNode, reissuanceRequest1, SimpleDummyStateContract.Commands.Create(),
            listOf(), requestReissuanceTransactionId1)
        reissueRequestedStates<SimpleDummyState>(issuerNode, reissuanceRequest2, SimpleDummyStateContract.Commands.Create(),
            listOf(), requestReissuanceTransactionId2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Requester shouldn't be passed in as one of extraAssetExitCommandSigners`() { // requester is a required signer
        initialiseParties()
        createSimpleDummyState(aliceParty)

        val simpleDummyState = getStateAndRefs<SimpleDummyState>(aliceNode)[0]
        val requestReissuanceTransactionId = createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(simpleDummyState),
            SimpleDummyStateContract.Commands.Delete(),
            issuerParty
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]
        reissueRequestedStates<SimpleDummyState>(issuerNode, reissuanceRequest, SimpleDummyStateContract.Commands.Create(),
            listOf(aliceParty), requestReissuanceTransactionId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Notary shouldn't be passed in as one of extraAssetExitCommandSigners`() { // notary is a required signer
        initialiseParties()
        createSimpleDummyState(aliceParty)

        val simpleDummyState = getStateAndRefs<SimpleDummyState>(aliceNode)[0]
        val requestReissuanceTransactionId = createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(simpleDummyState),
            SimpleDummyStateContract.Commands.Delete(),
            issuerParty
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]
        reissueRequestedStates<SimpleDummyState>(issuerNode, reissuanceRequest, SimpleDummyStateContract.Commands.Create(),
            listOf(notaryParty), requestReissuanceTransactionId)
    }
}
