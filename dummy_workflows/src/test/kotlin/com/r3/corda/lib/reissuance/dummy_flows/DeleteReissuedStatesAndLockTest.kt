package com.r3.corda.lib.reissuance.dummy_flows

import com.r3.corda.lib.reissuance.dummy_contracts.DummyStateRequiringAcceptanceContract
import com.r3.corda.lib.reissuance.dummy_contracts.DummyStateRequiringAllParticipantsSignaturesContract
import com.r3.corda.lib.reissuance.dummy_contracts.SimpleDummyStateContract
import com.r3.corda.lib.reissuance.dummy_flows.dummy.ModifiedDeleteReissuedStatesAndLock
import com.r3.corda.lib.reissuance.dummy_flows.dummy.ModifiedDeleteReissuedStatesAndLockResponder
import com.r3.corda.lib.reissuance.states.ReissuanceLock
import com.r3.corda.lib.reissuance.states.ReissuanceRequest
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

class DeleteReissuedStatesAndLockTest: AbstractFlowTest() {

    private inline fun <reified T> verifyDeletedReissuedStatesAndLock(
        statesToReissue: List<StateAndRef<T>>,
        accountUUID: UUID? = null,
        node: TestStartedNode = aliceNode,
        extraUnencumberedStates: Int = 0
    ) where T: ContractState {
        val encumberedStates = getStateAndRefs<T>(node, encumbered = true, accountUUID = accountUUID)
        val unencumberedStates = getStateAndRefs<T>(node, encumbered = false, accountUUID = accountUUID)
        assertThat(encumberedStates, empty())
        assertThat(unencumberedStates, hasSize(`is`(statesToReissue.size + extraUnencumberedStates)))
        assertThat(unencumberedStates, hasItems(*statesToReissue.toTypedArray()))
    }

    @Test
    fun `Re-issued SimpleDummyState and corresponding ReissuanceLock are deleted`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)

        val statesToReissue = getStateAndRefs<SimpleDummyState>(aliceNode) // there is just 1
        createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            statesToReissue,
            SimpleDummyStateContract.Commands.Create(),
            issuerParty
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]
        reissueRequestedStates<SimpleDummyState>(issuerNode, reissuanceRequest, listOf())

        val reissuedSimpleDummyStates = getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = true)
        val lockState = getStateAndRefs<ReissuanceLock<SimpleDummyState>>(aliceNode)[0]
        deleteReissuedStatesAndLock(
            aliceNode,
            lockState,
            reissuedSimpleDummyStates,
            SimpleDummyStateContract.Commands.Delete()
        )

        verifyDeletedReissuedStatesAndLock(statesToReissue)
    }

    @Test
    fun `Re-issued DummyStateRequiringAcceptance is unencumbered after the original state are deleted`() {
        initialiseParties()
        createDummyStateRequiringAcceptance(aliceParty)

        val statesToReissue = getStateAndRefs<DummyStateRequiringAcceptance>(aliceNode)
        createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            statesToReissue,
            DummyStateRequiringAcceptanceContract.Commands.Create(),
            issuerParty,
            listOf(acceptorParty)
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]
        reissueRequestedStates<DummyStateRequiringAcceptance>(issuerNode, reissuanceRequest,
            listOf(acceptorParty))

        val reissuedDummyStatesRequiringAcceptance = getStateAndRefs<DummyStateRequiringAcceptance>(aliceNode, encumbered = true)
        val lockState = getStateAndRefs<ReissuanceLock<DummyStateRequiringAcceptance>>(aliceNode)[0]
        deleteReissuedStatesAndLock(
            aliceNode,
            lockState,
            reissuedDummyStatesRequiringAcceptance,
            DummyStateRequiringAcceptanceContract.Commands.Delete(),
            listOf(issuerParty, aliceParty, acceptorParty)
        )

        verifyDeletedReissuedStatesAndLock(statesToReissue)
    }

    @Test
    fun `Re-issued DummyStateRequiringAllParticipantsSignatures and corresponding ReissuanceLock are deleted`() {
        initialiseParties()
        createDummyStateRequiringAllParticipantsSignatures(aliceParty)

        val statesToReissue = getStateAndRefs<DummyStateRequiringAllParticipantsSignatures>(aliceNode)
        createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            statesToReissue,
            DummyStateRequiringAllParticipantsSignaturesContract.Commands.Create(),
            issuerParty,
            listOf(acceptorParty, aliceParty)
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]
        reissueRequestedStates<DummyStateRequiringAllParticipantsSignatures>(issuerNode, reissuanceRequest,
            listOf(issuerParty, acceptorParty))

        val reissuedDummyStatesRequiringAcceptance = getStateAndRefs<DummyStateRequiringAllParticipantsSignatures>(
            aliceNode, encumbered = true)
        val lockState = getStateAndRefs<ReissuanceLock<DummyStateRequiringAllParticipantsSignatures>>(aliceNode)[0]
        deleteReissuedStatesAndLock(
            aliceNode,
            lockState,
            reissuedDummyStatesRequiringAcceptance,
            DummyStateRequiringAllParticipantsSignaturesContract.Commands.Delete(),
            listOf(issuerParty, aliceParty, acceptorParty)
        )

        verifyDeletedReissuedStatesAndLock(statesToReissue)
    }

    @Test
    fun `Re-issued tokens and corresponding ReissuanceLock are deleted`() {
        initialiseParties()
        issueTokens(aliceParty, 50)

        val statesToReissue = getTokens(aliceNode)
        val tokenIndices = statesToReissue.indices.toList()
        createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            statesToReissue,
            IssueTokenCommand(issuedTokenType, tokenIndices),
            issuerParty
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]
        reissueRequestedStates<FungibleToken>(issuerNode, reissuanceRequest,
            listOf(issuerParty))

        val reissuedTokens = getStateAndRefs<FungibleToken>(aliceNode, encumbered = true)
        val lockState = getStateAndRefs<ReissuanceLock<FungibleToken>>(aliceNode)[0]
        deleteReissuedStatesAndLock(
            aliceNode,
            lockState,
            reissuedTokens,
            RedeemTokenCommand(issuedTokenType, tokenIndices, listOf())
        )

        verifyDeletedReissuedStatesAndLock(statesToReissue)
    }

    @Test
    fun `Re-issued tokens and corresponding ReissuanceLock are deleted when only part of tokens had been re-issued`() {
        initialiseParties()
        issueTokens(aliceParty, 50)
        transferTokens(aliceNode, bobParty, 30)
        transferTokens(bobNode, aliceParty, 30)

        val tokens = getTokens(aliceNode)
        val statesToReissue = listOf(tokens[1]) // 30 tokens
        val tokenIndices = listOf(0)

        createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            statesToReissue,
            IssueTokenCommand(issuedTokenType, tokenIndices),
            issuerParty
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]
        reissueRequestedStates<FungibleToken>(issuerNode, reissuanceRequest,
            listOf(issuerParty))

        val reissuedTokens = getStateAndRefs<FungibleToken>(aliceNode, encumbered = true)
        val lockState = getStateAndRefs<ReissuanceLock<FungibleToken>>(aliceNode)[0]
        deleteReissuedStatesAndLock(
            aliceNode,
            lockState,
            reissuedTokens,
            RedeemTokenCommand(issuedTokenType, tokenIndices, listOf())
        )

        verifyDeletedReissuedStatesAndLock(statesToReissue, extraUnencumberedStates = 1)
    }

    @Test
    fun `Re-issued tokens and corresponding ReissuanceLock are deleted when many token inputs were re-issued`() {
        initialiseParties()
        issueTokens(aliceParty, 50)
        transferTokens(aliceNode, bobParty, 30)
        transferTokens(bobNode, aliceParty, 30)

        val statesToReissue = getTokens(aliceNode)
        val tokenIndices = statesToReissue.indices.toList()
        createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            statesToReissue,
            IssueTokenCommand(issuedTokenType, tokenIndices),
            issuerParty
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]
        reissueRequestedStates<FungibleToken>(issuerNode, reissuanceRequest,
            listOf(issuerParty))

        val reissuedTokens = getStateAndRefs<FungibleToken>(aliceNode, encumbered = true)
        val lockState = getStateAndRefs<ReissuanceLock<FungibleToken>>(aliceNode)[0]
        deleteReissuedStatesAndLock(
            aliceNode,
            lockState,
            reissuedTokens,
            RedeemTokenCommand(issuedTokenType, tokenIndices, listOf())
        )

        verifyDeletedReissuedStatesAndLock(statesToReissue)
    }

    @Test
    fun `Re-issued SimpleDummyState and corresponding ReissuanceLock are deleted - accounts on the same host`() {
        initialisePartiesForAccountsOnTheSameHost()
        createSimpleDummyStateForAccount(employeeNode, employeeAliceParty)

        val statesToReissue = getStateAndRefs<SimpleDummyState>(employeeNode,
            accountUUID = employeeAliceAccount.identifier.id)
        createReissuanceRequestAndShareRequiredTransactions(
            employeeNode,
            statesToReissue,
            SimpleDummyStateContract.Commands.Create(),
            employeeIssuerParty,
            requester = employeeAliceParty
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(employeeNode)[0]
        reissueRequestedStates<SimpleDummyState>(employeeNode, reissuanceRequest, listOf())

        val reissuedSimpleDummyStates = getStateAndRefs<SimpleDummyState>(employeeNode, encumbered = true,
            accountUUID = employeeAliceAccount.identifier.id)
        val lockState = getStateAndRefs<ReissuanceLock<SimpleDummyState>>(employeeNode,
            accountUUID = employeeAliceAccount.identifier.id)[0]
        deleteReissuedStatesAndLock(
            employeeNode,
            lockState,
            reissuedSimpleDummyStates,
            SimpleDummyStateContract.Commands.Delete()
        )

        verifyDeletedReissuedStatesAndLock(statesToReissue, accountUUID = employeeAliceAccount.identifier.id,
            node = employeeNode)
    }

    @Test
    fun `Re-issued SimpleDummyState and corresponding ReissuanceLock are deleted - accounts on different hosts`() {
        initialisePartiesForAccountsOnDifferentHosts()
        createSimpleDummyStateForAccount(issuerNode, employeeAliceParty)

        val statesToReissue = getStateAndRefs<SimpleDummyState>(aliceNode,
            accountUUID = employeeAliceAccount.identifier.id)
        createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            statesToReissue,
            SimpleDummyStateContract.Commands.Create(),
            employeeIssuerParty,
            requester = employeeAliceParty
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(aliceNode)[0]
        reissueRequestedStates<SimpleDummyState>(issuerNode, reissuanceRequest, listOf())

        val reissuedSimpleDummyStates = getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = true,
            accountUUID = employeeAliceAccount.identifier.id)
        val lockState = getStateAndRefs<ReissuanceLock<SimpleDummyState>>(aliceNode,
            accountUUID = employeeAliceAccount.identifier.id)[0]
        deleteReissuedStatesAndLock(
            aliceNode,
            lockState,
            reissuedSimpleDummyStates,
            SimpleDummyStateContract.Commands.Delete()
        )

        verifyDeletedReissuedStatesAndLock(statesToReissue, accountUUID = employeeAliceAccount.identifier.id)
    }

    @Test(expected = TransactionVerificationException::class)
    fun `Re-issued SimpleDummyState and corresponding ReissuanceLock can't be deleted after they are unlocked`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)

        val simpleDummyStatesToReissue = getStateAndRefs<SimpleDummyState>(aliceNode) // there is just 1
        createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            simpleDummyStatesToReissue,
            SimpleDummyStateContract.Commands.Create(),
            issuerParty
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]
        reissueRequestedStates<SimpleDummyState>(issuerNode, reissuanceRequest, listOf())

        val exitTransactionId = deleteSimpleDummyStateForAccount(aliceNode)
        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode, exitTransactionId)

        unlockReissuedState<SimpleDummyState>(
            aliceNode,
            listOf(attachmentSecureHash),
            SimpleDummyStateContract.Commands.Update(),
            getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = true),
            getStateAndRefs<ReissuanceLock<SimpleDummyState>>(aliceNode, encumbered = true)[0]
        )

        val reissuedSimpleDummyStates = getStateAndRefs<SimpleDummyState>(aliceNode)
        val lockState = getStateAndRefs<ReissuanceLock<SimpleDummyState>>(aliceNode)[0]
        deleteReissuedStatesAndLock(
            aliceNode,
            lockState,
            reissuedSimpleDummyStates,
            SimpleDummyStateContract.Commands.Delete()
        )
    }

    @Test(expected = TransactionVerificationException::class)
    fun `Make sure delete transaction can't produce any outputs`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)

        val simpleDummyStatesToReissue = getStateAndRefs<SimpleDummyState>(aliceNode) // there is just 1
        createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            simpleDummyStatesToReissue,
            SimpleDummyStateContract.Commands.Create(),
            issuerParty
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]
        reissueRequestedStates<SimpleDummyState>(issuerNode, reissuanceRequest, listOf())

        val reissuedSimpleDummyStates = getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = true)
        val lockState = getStateAndRefs<ReissuanceLock<SimpleDummyState>>(aliceNode)[0]
        deleteReissuedStatesAndLockUsingModifiedFlow(
            aliceNode,
            lockState,
            reissuedSimpleDummyStates,
            SimpleDummyStateContract.Commands.Delete()
        )
    }

}
