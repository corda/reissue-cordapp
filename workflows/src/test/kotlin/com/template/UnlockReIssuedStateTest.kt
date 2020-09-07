package com.template

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.r3.corda.lib.tokens.contracts.commands.IssueTokenCommand
import com.r3.corda.lib.tokens.contracts.commands.MoveTokenCommand
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.template.contracts.example.SimpleStateContract
import com.template.contracts.example.StateNeedingAcceptanceContract
import com.template.contracts.example.StateNeedingAllParticipantsToSignContract
import com.template.states.ReIssuanceRequest
import com.template.states.example.SimpleState
import com.template.states.example.StateNeedingAcceptance
import com.template.states.example.StateNeedingAllParticipantsToSign
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.node.services.queryBy
import org.junit.Test

class UnlockReIssuedStateTest: AbstractFlowTest() {

    @Test
    fun `Re-issued SimpleState is unencumbered after the original state is deleted`() {
        // generate back-chain
        initialiseParties()
        createSimpleState(aliceParty)
        updateSimpleState(aliceNode, bobParty)
        updateSimpleState(bobNode, charlieParty)
        updateSimpleState(charlieNode, aliceParty)
        updateSimpleState(aliceNode, bobParty)
        updateSimpleState(bobNode, charlieParty)
        updateSimpleState(charlieNode, aliceParty)

        // get transaction history, not a back-chain
        val transactionsBeforeReIssuance = getTransactions(aliceNode)
        assertThat(transactionsBeforeReIssuance.size, equalTo(7))

        // re-issue state
        val simpleStateStateAndRef = getStateAndRefs<SimpleState>(aliceNode)[0]

        createReIssuanceRequest(
            aliceNode,
            listOf(simpleStateStateAndRef),
            SimpleStateContract.Commands.Create(),
            issuerParty
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest<SimpleState>>().states[0]

        reIssueRequestedStates(reIssuanceRequest)

        deleteSimpleState(aliceNode)

        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode)

        unlockReIssuedState<SimpleState>(
            aliceNode, attachmentSecureHash,
            SimpleStateContract.Commands.Update()
        )

        // change state holder
        updateSimpleState(aliceNode, debbieParty)

        // check transaction history again
        val transactionsAfterReIssuance = getTransactions(debbieNode)
        assertThat(transactionsAfterReIssuance.size, equalTo(4))

    }

    @Test
    fun `Re-issued StateNeedingAcceptance is unencumbered after the original state is deleted`() {
        initialiseParties()
        createStateNeedingAcceptance(aliceParty)
        updateStateNeedingAcceptance(aliceNode, bobParty)
        updateStateNeedingAcceptance(bobNode, charlieParty)
        updateStateNeedingAcceptance(charlieNode, aliceParty)
        updateStateNeedingAcceptance(aliceNode, bobParty)
        updateStateNeedingAcceptance(bobNode, charlieParty)
        updateStateNeedingAcceptance(charlieNode, aliceParty)

        val transactionsBeforeReIssuance = getTransactions(aliceNode)
        assertThat(transactionsBeforeReIssuance.size, equalTo(7))

        val stateNeedingAcceptanceStateAndRef = getStateAndRefs<StateNeedingAcceptance>(aliceNode)[0]

        createReIssuanceRequest(
            aliceNode,
            listOf(stateNeedingAcceptanceStateAndRef),
            StateNeedingAcceptanceContract.Commands.Create(),
            issuerParty,
            listOf(issuerParty, acceptorParty)
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest<StateNeedingAcceptance>>().states[0]

        reIssueRequestedStates(reIssuanceRequest)

        deleteStateNeedingAcceptance(aliceNode)

        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode)

        unlockReIssuedState<StateNeedingAcceptance>(
            aliceNode, attachmentSecureHash,
            StateNeedingAcceptanceContract.Commands.Update(),
            listOf(aliceParty, issuerParty, acceptorParty)
        )

        updateStateNeedingAcceptance(aliceNode, debbieParty)

        val transactionsAfterReIssuance = getTransactions(debbieNode)
        assertThat(transactionsAfterReIssuance.size, equalTo(4))
    }

    @Test
    fun `StateNeedingAllParticipantsToSign is re-issued`() {
        initialiseParties()
        createStateNeedingAllParticipantsToSign(aliceParty)
        updateStateNeedingAllParticipantsToSign(aliceNode, bobParty)
        updateStateNeedingAllParticipantsToSign(bobNode, charlieParty)
        updateStateNeedingAllParticipantsToSign(charlieNode, aliceParty)
        updateStateNeedingAllParticipantsToSign(aliceNode, bobParty)
        updateStateNeedingAllParticipantsToSign(bobNode, charlieParty)
        updateStateNeedingAllParticipantsToSign(charlieNode, aliceParty)

        val transactionsBeforeReIssuance = getTransactions(aliceNode)
        assertThat(transactionsBeforeReIssuance.size, equalTo(7))

        val stateNeedingAllParticipantsToSignStateAndRef = getStateAndRefs<StateNeedingAllParticipantsToSign>(aliceNode)[0]

        createReIssuanceRequest(
            aliceNode,
            listOf(stateNeedingAllParticipantsToSignStateAndRef),
            StateNeedingAllParticipantsToSignContract.Commands.Create(),
            issuerParty,
            listOf(aliceParty, issuerParty, acceptorParty)
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest<StateNeedingAllParticipantsToSign>>().states[0]

        reIssueRequestedStates(reIssuanceRequest)

        deleteStateNeedingAllParticipantsToSign(aliceNode)

        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode)

        unlockReIssuedState<StateNeedingAllParticipantsToSign>(
            aliceNode,
            attachmentSecureHash,
            StateNeedingAllParticipantsToSignContract.Commands.Update(),
            listOf(aliceParty, issuerParty, acceptorParty)
        )

        updateStateNeedingAllParticipantsToSign(aliceNode, debbieParty)

        val transactionsAfterReIssuance = getTransactions(debbieNode)
        assertThat(transactionsAfterReIssuance.size, equalTo(4))
    }

    @Test
    fun `Re-issued token is unencumbered after the original state is deleted`() {
        initialiseParties()
        issueTokens(aliceParty, 50)

        transferTokens(aliceNode, bobParty, 50)
        transferTokens(bobNode, charlieParty, 50)
        transferTokens(charlieNode, aliceParty, 50)
        transferTokens(aliceNode, bobParty, 50)
        transferTokens(bobNode, charlieParty, 50)
        transferTokens(charlieNode, aliceParty, 50)

        val transactionsBeforeReIssuance = getTransactions(aliceNode)
        assertThat(transactionsBeforeReIssuance.size, equalTo(7))

        val tokens = getTokens(aliceNode)
        val tokenIndices = tokens.indices.toList()

        createReIssuanceRequest(
            aliceNode,
            tokens,
            IssueTokenCommand(issuedTokenType, tokenIndices),
            issuerParty
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest<FungibleToken>>().states[0]

        reIssueRequestedStates(reIssuanceRequest)

        redeemTokens(aliceNode, tokens)

        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode)

        val indicesList = tokens.indices.toList()
        unlockReIssuedState<FungibleToken>(
            aliceNode,
            attachmentSecureHash,
            MoveTokenCommand(issuedTokenType, indicesList, indicesList)
        )

        transferTokens(aliceNode, debbieParty, 50)

        val transactionsAfterReIssuance = getTransactions(debbieNode)
        assertThat(transactionsAfterReIssuance.size, equalTo(4))
    }


    @Test
    fun `Re-issue just part of tokens`() {
        initialiseParties()
        issueTokens(aliceParty, 50)

        transferTokens(aliceNode, bobParty, 40)
        transferTokens(bobNode, charlieParty, 30)
        transferTokens(charlieNode, aliceParty, 30)
        transferTokens(aliceNode, bobParty, 30)
        transferTokens(bobNode, charlieParty, 30)
        transferTokens(charlieNode, aliceParty, 30)

        val transactionsBeforeReIssuance = getTransactions(aliceNode)
        assertThat(transactionsBeforeReIssuance.size, equalTo(7))

        val tokens = listOf(getTokens(aliceNode)[1]) // 30 tokens
        val indicesList = listOf(0)

        createReIssuanceRequest(
            aliceNode,
            tokens,
            IssueTokenCommand(issuedTokenType, indicesList),
            issuerParty
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest<FungibleToken>>().states[0]

        reIssueRequestedStates(reIssuanceRequest)

        redeemTokens(aliceNode, tokens)

        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode)

        unlockReIssuedState<FungibleToken>(
            aliceNode,
            attachmentSecureHash,
            MoveTokenCommand(issuedTokenType, indicesList, indicesList)
        )

        transferTokens(aliceNode, debbieParty, 35)

        val transactionsAfterReIssuance = getTransactions(debbieNode)
        assertThat(transactionsAfterReIssuance.size, equalTo(9))
    }


    @Test
    fun `Re-issued tokens are unencumbered after the original state is deleted`() {
        initialiseParties()
        issueTokens(aliceParty, 50)

        transferTokens(aliceNode, bobParty, 40)
        transferTokens(bobNode, charlieParty, 30)
        transferTokens(charlieNode, aliceParty, 30)
        transferTokens(aliceNode, bobParty, 30)
        transferTokens(bobNode, charlieParty, 30)
        transferTokens(charlieNode, aliceParty, 30)

        val transactionsBeforeReIssuance = getTransactions(aliceNode)
        assertThat(transactionsBeforeReIssuance.size, equalTo(7))

        val tokens = getTokens(aliceNode)
        val tokenIndices = tokens.indices.toList()

        createReIssuanceRequest(
            aliceNode,
            tokens,
            IssueTokenCommand(issuedTokenType, tokenIndices),
            issuerParty
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest<FungibleToken>>().states[0]

        reIssueRequestedStates(reIssuanceRequest)

        redeemTokens(aliceNode, tokens)

        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode)

        unlockReIssuedState<FungibleToken>(
            aliceNode,
            attachmentSecureHash,
            MoveTokenCommand(issuedTokenType, tokenIndices, tokenIndices)
        )

        transferTokens(aliceNode, debbieParty, 35)

        val transactionsAfterReIssuance = getTransactions(debbieNode)
        assertThat(transactionsAfterReIssuance.size, equalTo(4))
    }

    @Test(expected = TransactionVerificationException::class)
    fun `Only requester can unlock re-issued state`() {
        initialiseParties()
        createStateNeedingAcceptance(aliceParty)

        val stateNeedingAcceptanceStateAndRef = getStateAndRefs<StateNeedingAcceptance>(aliceNode)[0]

        createReIssuanceRequest(
            aliceNode,
            listOf(stateNeedingAcceptanceStateAndRef),
            StateNeedingAcceptanceContract.Commands.Create(),
            issuerParty,
            listOf(issuerParty, acceptorParty)
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest<StateNeedingAcceptance>>().states[0]

        reIssueRequestedStates(reIssuanceRequest)

        deleteStateNeedingAcceptance(aliceNode)

        // issuer creates attachment and tries to unlock state
        val attachmentSecureHash = uploadDeletedStateAttachment(issuerNode)

        unlockReIssuedState<StateNeedingAcceptance>(
            issuerNode,
            attachmentSecureHash,
            StateNeedingAcceptanceContract.Commands.Update(),
            listOf(aliceParty, issuerParty, acceptorParty)
        )
    }

    @Test
    fun `SimpleState re-issued for an account`() {
        initialisePartiesForAccountsOnTheSameHost()
        createSimpleStateForAccount(employeeNode, employeeAliceParty)
        updateSimpleStateForAccount(employeeBobParty)
        updateSimpleStateForAccount(employeeCharlieParty)
        updateSimpleStateForAccount(employeeAliceParty)
        updateSimpleStateForAccount(employeeBobParty)
        updateSimpleStateForAccount(employeeCharlieParty)
        updateSimpleStateForAccount(employeeAliceParty)

        val transactionsBeforeReIssuance = getTransactions(employeeNode)
        assertThat(transactionsBeforeReIssuance.size, equalTo(12)) // including 5 create account transactions

        val simpleStateStateAndRef = getStateAndRefs<SimpleState>(employeeNode)[0]
        createReIssuanceRequest(
            employeeNode,
            listOf(simpleStateStateAndRef),
            SimpleStateContract.Commands.Create(),
            employeeIssuerParty
        )

        val reIssuanceRequest = employeeNode.services.vaultService.queryBy<ReIssuanceRequest<SimpleState>>().states[0]

        reIssueRequestedStates(reIssuanceRequest, employeeNode)

        deleteSimpleStateForAccount()

        val attachmentSecureHash = uploadDeletedStateAttachment(employeeNode)

        unlockReIssuedState<SimpleState>(
            employeeNode,
            attachmentSecureHash,
            SimpleStateContract.Commands.Update()
        )

        updateSimpleStateForAccount(employeeDebbieParty)

        val transactionsAfterReIssuance = getTransactions(employeeNode) // TODO: figure out how to get back-chain for a given account
//        assertThat(transactionsAfterReIssuance.size, equalTo(4))
    }
}
