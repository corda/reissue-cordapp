package com.r3.corda.lib.reissuance.dummy_flows

import com.r3.corda.lib.reissuance.dummy_contracts.DummyStateRequiringAcceptanceContract
import com.r3.corda.lib.reissuance.dummy_contracts.DummyStateRequiringAllParticipantsSignaturesContract
import com.r3.corda.lib.reissuance.dummy_contracts.SimpleDummyStateContract
import com.r3.corda.lib.reissuance.states.ReIssuanceLock
import com.r3.corda.lib.reissuance.states.ReIssuanceRequest
import com.r3.corda.lib.reissuance.dummy_states.DummyStateRequiringAcceptance
import com.r3.corda.lib.reissuance.dummy_states.DummyStateRequiringAllParticipantsSignatures
import com.r3.corda.lib.reissuance.dummy_states.SimpleDummyState
import com.r3.corda.lib.tokens.contracts.commands.IssueTokenCommand
import com.r3.corda.lib.tokens.contracts.commands.RedeemTokenCommand
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import net.corda.core.contracts.TransactionVerificationException
import net.corda.testing.node.internal.TestStartedNode
import org.junit.Test
import java.util.*

class DeleteReIssuedStatesAndLockTest: AbstractFlowTest() {

    private inline fun <reified T> verifyDeletedReIssuedStatesAndLock(
        statesToReIssue: List<StateAndRef<T>>,
        accountUUID: UUID? = null,
        node: TestStartedNode = aliceNode,
        extraUnencumberedStates: Int = 0
    ) where T: ContractState {
        val encumberedStates = getStateAndRefs<T>(node, encumbered = true, accountUUID = accountUUID)
        val unencumberedStates = getStateAndRefs<T>(node, encumbered = false, accountUUID = accountUUID)
        assertThat(encumberedStates, empty())
        assertThat(unencumberedStates, hasSize(`is`(statesToReIssue.size + extraUnencumberedStates)))
        assertThat(unencumberedStates, hasItems(*statesToReIssue.toTypedArray()))
    }

    @Test
    fun `Re-issued SimpleDummyState and corresponding ReIssuanceLock are deleted`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)

        val statesToReIssue = getStateAndRefs<SimpleDummyState>(aliceNode) // there is just 1
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            statesToReIssue,
            SimpleDummyStateContract.Commands.Create(),
            issuerParty
        )

        val reIssuanceRequest = getStateAndRefs<ReIssuanceRequest>(issuerNode)[0]
        reIssueRequestedStates<SimpleDummyState>(issuerNode, reIssuanceRequest,
            issuerIsRequiredExitCommandSigner = false)

        val reIssuedSimpleDummyStates = getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = true)
        val lockState = getStateAndRefs<ReIssuanceLock<SimpleDummyState>>(aliceNode)[0]
        deleteReIssuedStatesAndLock(
            aliceNode,
            lockState,
            reIssuedSimpleDummyStates,
            SimpleDummyStateContract.Commands.Delete()
        )

        verifyDeletedReIssuedStatesAndLock(statesToReIssue)
    }

    @Test
    fun `Re-issued DummyStateRequiringAcceptance is unencumbered after the original state are deleted`() {
        initialiseParties()
        createDummyStateRequiringAcceptance(aliceParty)

        val statesToReIssue = getStateAndRefs<DummyStateRequiringAcceptance>(aliceNode)
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            statesToReIssue,
            DummyStateRequiringAcceptanceContract.Commands.Create(),
            issuerParty,
            listOf(issuerParty, acceptorParty)
        )

        val reIssuanceRequest = getStateAndRefs<ReIssuanceRequest>(issuerNode)[0]
        reIssueRequestedStates<DummyStateRequiringAcceptance>(issuerNode, reIssuanceRequest,
            issuerIsRequiredExitCommandSigner = true)

        val reIssuedDummyStatesRequiringAcceptance = getStateAndRefs<DummyStateRequiringAcceptance>(aliceNode, encumbered = true)
        val lockState = getStateAndRefs<ReIssuanceLock<DummyStateRequiringAcceptance>>(aliceNode)[0]
        deleteReIssuedStatesAndLock(
            aliceNode,
            lockState,
            reIssuedDummyStatesRequiringAcceptance,
            DummyStateRequiringAcceptanceContract.Commands.Delete(),
            listOf(issuerParty, aliceParty, acceptorParty)
        )

        verifyDeletedReIssuedStatesAndLock(statesToReIssue)
    }

    @Test
    fun `Re-issued DummyStateRequiringAllParticipantsSignatures and corresponding ReIssuanceLock are deleted`() {
        initialiseParties()
        createDummyStateRequiringAllParticipantsSignatures(aliceParty)

        val statesToReIssue = getStateAndRefs<DummyStateRequiringAllParticipantsSignatures>(aliceNode)
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            statesToReIssue,
            DummyStateRequiringAllParticipantsSignaturesContract.Commands.Create(),
            issuerParty,
            listOf(aliceParty, issuerParty, acceptorParty)
        )

        val reIssuanceRequest = getStateAndRefs<ReIssuanceRequest>(issuerNode)[0]
        reIssueRequestedStates<DummyStateRequiringAllParticipantsSignatures>(issuerNode, reIssuanceRequest,
            issuerIsRequiredExitCommandSigner = true)

        val reIssuedDummyStatesRequiringAcceptance = getStateAndRefs<DummyStateRequiringAllParticipantsSignatures>(
            aliceNode, encumbered = true)
        val lockState = getStateAndRefs<ReIssuanceLock<DummyStateRequiringAllParticipantsSignatures>>(aliceNode)[0]
        deleteReIssuedStatesAndLock(
            aliceNode,
            lockState,
            reIssuedDummyStatesRequiringAcceptance,
            DummyStateRequiringAllParticipantsSignaturesContract.Commands.Delete(),
            listOf(issuerParty, aliceParty, acceptorParty)
        )

        verifyDeletedReIssuedStatesAndLock(statesToReIssue)
    }

    @Test
    fun `Re-issued tokens and corresponding ReIssuanceLock are deleted`() {
        initialiseParties()
        issueTokens(aliceParty, 50)

        val statesToReIssue = getTokens(aliceNode)
        val tokenIndices = statesToReIssue.indices.toList()
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            statesToReIssue,
            IssueTokenCommand(issuedTokenType, tokenIndices),
            issuerParty
        )

        val reIssuanceRequest = getStateAndRefs<ReIssuanceRequest>(issuerNode)[0]
        reIssueRequestedStates<FungibleToken>(issuerNode, reIssuanceRequest,
            issuerIsRequiredExitCommandSigner = true)

        val reIssuedTokens = getStateAndRefs<FungibleToken>(aliceNode, encumbered = true)
        val lockState = getStateAndRefs<ReIssuanceLock<FungibleToken>>(aliceNode)[0]
        deleteReIssuedStatesAndLock(
            aliceNode,
            lockState,
            reIssuedTokens,
            RedeemTokenCommand(issuedTokenType, tokenIndices, listOf())
        )

        verifyDeletedReIssuedStatesAndLock(statesToReIssue)
    }

    @Test
    fun `Re-issued tokens and corresponding ReIssuanceLock are deleted when only part of tokens had been re-issued`() {
        initialiseParties()
        issueTokens(aliceParty, 50)
        transferTokens(aliceNode, bobParty, 30)
        transferTokens(bobNode, aliceParty, 30)

        val tokens = getTokens(aliceNode)
        val statesToReIssue = listOf(tokens[1]) // 30 tokens
        val tokenIndices = listOf(0)

        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            statesToReIssue,
            IssueTokenCommand(issuedTokenType, tokenIndices),
            issuerParty
        )

        val reIssuanceRequest = getStateAndRefs<ReIssuanceRequest>(issuerNode)[0]
        reIssueRequestedStates<FungibleToken>(issuerNode, reIssuanceRequest,
            issuerIsRequiredExitCommandSigner = true)

        val reIssuedTokens = getStateAndRefs<FungibleToken>(aliceNode, encumbered = true)
        val lockState = getStateAndRefs<ReIssuanceLock<FungibleToken>>(aliceNode)[0]
        deleteReIssuedStatesAndLock(
            aliceNode,
            lockState,
            reIssuedTokens,
            RedeemTokenCommand(issuedTokenType, tokenIndices, listOf())
        )

        verifyDeletedReIssuedStatesAndLock(statesToReIssue, extraUnencumberedStates = 1)
    }

    @Test
    fun `Re-issued tokens and corresponding ReIssuanceLock are deleted when many token inputs were re-issued`() {
        initialiseParties()
        issueTokens(aliceParty, 50)
        transferTokens(aliceNode, bobParty, 30)
        transferTokens(bobNode, aliceParty, 30)

        val statesToReIssue = getTokens(aliceNode)
        val tokenIndices = statesToReIssue.indices.toList()
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            statesToReIssue,
            IssueTokenCommand(issuedTokenType, tokenIndices),
            issuerParty
        )

        val reIssuanceRequest = getStateAndRefs<ReIssuanceRequest>(issuerNode)[0]
        reIssueRequestedStates<FungibleToken>(issuerNode, reIssuanceRequest,
            issuerIsRequiredExitCommandSigner = true)

        val reIssuedTokens = getStateAndRefs<FungibleToken>(aliceNode, encumbered = true)
        val lockState = getStateAndRefs<ReIssuanceLock<FungibleToken>>(aliceNode)[0]
        deleteReIssuedStatesAndLock(
            aliceNode,
            lockState,
            reIssuedTokens,
            RedeemTokenCommand(issuedTokenType, tokenIndices, listOf())
        )

        verifyDeletedReIssuedStatesAndLock(statesToReIssue)
    }

    @Test
    fun `Re-issued SimpleDummyState and corresponding ReIssuanceLock are deleted - accounts on the same host`() {
        initialisePartiesForAccountsOnTheSameHost()
        createSimpleDummyStateForAccount(employeeNode, employeeAliceParty)

        val statesToReIssue = getStateAndRefs<SimpleDummyState>(employeeNode,
            accountUUID = employeeAliceAccount.identifier.id)
        createReIssuanceRequestAndShareRequiredTransactions(
            employeeNode,
            statesToReIssue,
            SimpleDummyStateContract.Commands.Create(),
            employeeIssuerParty,
            requester = employeeAliceParty
        )

        val reIssuanceRequest = getStateAndRefs<ReIssuanceRequest>(employeeNode)[0]
        reIssueRequestedStates<SimpleDummyState>(employeeNode, reIssuanceRequest,
            issuerIsRequiredExitCommandSigner = true)

        val reIssuedSimpleDummyStates = getStateAndRefs<SimpleDummyState>(employeeNode, encumbered = true,
            accountUUID = employeeAliceAccount.identifier.id)
        val lockState = getStateAndRefs<ReIssuanceLock<SimpleDummyState>>(employeeNode,
            accountUUID = employeeAliceAccount.identifier.id)[0]
        deleteReIssuedStatesAndLock(
            employeeNode,
            lockState,
            reIssuedSimpleDummyStates,
            SimpleDummyStateContract.Commands.Delete()
        )

        verifyDeletedReIssuedStatesAndLock(statesToReIssue, accountUUID = employeeAliceAccount.identifier.id,
            node = employeeNode)
    }

    @Test
    fun `Re-issued SimpleDummyState and corresponding ReIssuanceLock are deleted - accounts on different hosts`() {
        initialisePartiesForAccountsOnDifferentHosts()
        createSimpleDummyStateForAccount(issuerNode, employeeAliceParty)

        val statesToReIssue = getStateAndRefs<SimpleDummyState>(aliceNode,
            accountUUID = employeeAliceAccount.identifier.id)
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            statesToReIssue,
            SimpleDummyStateContract.Commands.Create(),
            employeeIssuerParty,
            requester = employeeAliceParty
        )

        val reIssuanceRequest = getStateAndRefs<ReIssuanceRequest>(aliceNode)[0]
        reIssueRequestedStates<SimpleDummyState>(issuerNode, reIssuanceRequest,
            issuerIsRequiredExitCommandSigner = true)

        val reIssuedSimpleDummyStates = getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = true,
            accountUUID = employeeAliceAccount.identifier.id)
        val lockState = getStateAndRefs<ReIssuanceLock<SimpleDummyState>>(aliceNode,
            accountUUID = employeeAliceAccount.identifier.id)[0]
        deleteReIssuedStatesAndLock(
            aliceNode,
            lockState,
            reIssuedSimpleDummyStates,
            SimpleDummyStateContract.Commands.Delete()
        )

        verifyDeletedReIssuedStatesAndLock(statesToReIssue, accountUUID = employeeAliceAccount.identifier.id)
    }

    @Test(expected = TransactionVerificationException::class)
    fun `Re-issued SimpleDummyState and corresponding ReIssuanceLock can't be deleted after they are unlocked`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)

        val simpleDummyStatesToReIssue = getStateAndRefs<SimpleDummyState>(aliceNode) // there is just 1
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            simpleDummyStatesToReIssue,
            SimpleDummyStateContract.Commands.Create(),
            issuerParty
        )

        val reIssuanceRequest = getStateAndRefs<ReIssuanceRequest>(issuerNode)[0]
        reIssueRequestedStates<SimpleDummyState>(issuerNode, reIssuanceRequest,
            issuerIsRequiredExitCommandSigner = true)

        val exitTransactionId = deleteSimpleDummyStateForAccount(aliceNode)
        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode, exitTransactionId)

        unlockReIssuedState<SimpleDummyState>(
            aliceNode,
            listOf(attachmentSecureHash),
            SimpleDummyStateContract.Commands.Update(),
            getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = true),
            getStateAndRefs<ReIssuanceLock<SimpleDummyState>>(aliceNode, encumbered = true)[0]
        )

        val reIssuedSimpleDummyStates = getStateAndRefs<SimpleDummyState>(aliceNode)
        val lockState = getStateAndRefs<ReIssuanceLock<SimpleDummyState>>(aliceNode)[0]
        deleteReIssuedStatesAndLock(
            aliceNode,
            lockState,
            reIssuedSimpleDummyStates,
            SimpleDummyStateContract.Commands.Delete()
        )
    }

    @Test(expected = TransactionVerificationException::class)
    fun `Make sure delete transaction can't produce any outputs`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)

        val simpleDummyStatesToReIssue = getStateAndRefs<SimpleDummyState>(aliceNode) // there is just 1
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            simpleDummyStatesToReIssue,
            SimpleDummyStateContract.Commands.Create(),
            issuerParty
        )

        val reIssuanceRequest = getStateAndRefs<ReIssuanceRequest>(issuerNode)[0]
        reIssueRequestedStates<SimpleDummyState>(issuerNode, reIssuanceRequest,
            issuerIsRequiredExitCommandSigner = false)

        val reIssuedSimpleDummyStates = getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = true)
        val lockState = getStateAndRefs<ReIssuanceLock<SimpleDummyState>>(aliceNode)[0]
        updatedDeleteReIssuedStatesAndLock(
            aliceNode,
            lockState,
            reIssuedSimpleDummyStates,
            SimpleDummyStateContract.Commands.Delete()
        )
    }

}
