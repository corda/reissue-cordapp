package com.template

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.template.states.ReIssuanceRequest
import com.template.states.example.SimpleState
import com.template.states.example.StateNeedingAcceptance
import net.corda.core.node.services.queryBy
import org.junit.Test

class UnlockReIssuedStateTest: AbstractFlowTest() {

    @Test
    fun `Re-issued SimpleState is unencumbered after the original state is deleted`() {
        createSimpleState(aliceParty)
        updateSimpleState(aliceNode, bobParty)
        updateSimpleState(bobNode, charlieParty)
        updateSimpleState(charlieNode, aliceParty)
        updateSimpleState(aliceNode, bobParty)
        updateSimpleState(bobNode, charlieParty)
        updateSimpleState(charlieNode, aliceParty)

        val transactionsBeforeReIssuance = getTransactions(aliceNode)

        assertThat(transactionsBeforeReIssuance.size, equalTo(7))

        val simpleStateStateAndRef = getStateAndRefs<SimpleState>(aliceNode)[0]

        createSimpleStateReIssuanceRequest(aliceNode, simpleStateStateAndRef)

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest<SimpleState>>().states[0]

        reIssueRequestedStates(reIssuanceRequest)

        deleteSimpleState(aliceNode)

        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode)

        unlockReIssuedSimpleState(aliceNode, attachmentSecureHash)

        updateSimpleState(aliceNode, debbieParty)

        val transactionsAfterReIssuance = getTransactions(debbieNode)
        assertThat(transactionsAfterReIssuance.size, equalTo(4))

    }

    @Test
    fun `Re-issued StateNeedingAcceptance is unencumbered after the original state is deleted`() {
        createStateNeedingAcceptance(aliceParty)
        updateStateNeedingAcceptance(aliceNode, bobParty)
        updateStateNeedingAcceptance(bobNode, charlieParty)
        updateStateNeedingAcceptance(charlieNode, aliceParty)
        updateStateNeedingAcceptance(aliceNode, bobParty)
        updateStateNeedingAcceptance(bobNode, charlieParty)
        updateStateNeedingAcceptance(charlieNode, aliceParty)

        val transactions = getTransactions(aliceNode)

        assertThat(transactions.size, equalTo(7))

        val stateNeedingAcceptanceStateAndRef = getStateAndRefs<StateNeedingAcceptance>(aliceNode)[0]

        createStateNeedingAcceptanceReIssuanceRequest(aliceNode, stateNeedingAcceptanceStateAndRef)

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest<StateNeedingAcceptance>>().states[0]

        reIssueRequestedStates(reIssuanceRequest)

        deleteStateNeedingAcceptance(aliceNode)

        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode)

        unlockReIssuedStateNeedingAcceptance(aliceNode, attachmentSecureHash)

        updateStateNeedingAcceptance(aliceNode, debbieParty)

        val transactionsAfterReIssuance = getTransactions(debbieNode)
        assertThat(transactionsAfterReIssuance.size, equalTo(4))
    }

    @Test
    fun `Re-issued token is unencumbered after the original state is deleted`() {
        issueTokens(aliceParty, 50)

        transferTokens(aliceNode, bobParty, 50)
        transferTokens(bobNode, charlieParty, 50)
        transferTokens(charlieNode, aliceParty, 50)
        transferTokens(aliceNode, bobParty, 50)
        transferTokens(bobNode, charlieParty, 50)
        transferTokens(charlieNode, aliceParty, 50)

        val transactions = getTransactions(aliceNode)

        assertThat(transactions.size, equalTo(7))

        val tokens = getTokens(aliceNode)

        createTokenReIssuanceRequest(aliceNode, tokens)

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest<FungibleToken>>().states[0]

        reIssueRequestedStates(reIssuanceRequest)

        redeemTokens(aliceNode, tokens)

        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode)

        unlockReIssuedTokens(aliceNode, attachmentSecureHash)

        transferTokens(aliceNode, debbieParty, 50)

        val transactionsAfterReIssuance = getTransactions(debbieNode)
        assertThat(transactionsAfterReIssuance.size, equalTo(4))
    }


    @Test
    fun `Re-issue just part of tokens`() {
        issueTokens(aliceParty, 50)

        transferTokens(aliceNode, bobParty, 40)
        transferTokens(bobNode, charlieParty, 30)
        transferTokens(charlieNode, aliceParty, 30)
        transferTokens(aliceNode, bobParty, 30)
        transferTokens(bobNode, charlieParty, 30)
        transferTokens(charlieNode, aliceParty, 30)

        val transactions = getTransactions(aliceNode)

        assertThat(transactions.size, equalTo(7))

        val tokens = listOf(getTokens(aliceNode)[1]) // 30 tokens

        createTokenReIssuanceRequest(aliceNode, tokens)

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest<FungibleToken>>().states[0]

        reIssueRequestedStates(reIssuanceRequest)

        redeemTokens(aliceNode, tokens)

        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode)

        unlockReIssuedTokens(aliceNode, attachmentSecureHash)

        transferTokens(aliceNode, debbieParty, 35)

        val transactionsAfterReIssuance = getTransactions(debbieNode)
        assertThat(transactionsAfterReIssuance.size, equalTo(9))
    }


    @Test
    fun `Re-issued tokens are unencumbered after the original state is deleted`() {
        issueTokens(aliceParty, 50)

        transferTokens(aliceNode, bobParty, 40)
        transferTokens(bobNode, charlieParty, 30)
        transferTokens(charlieNode, aliceParty, 30)
        transferTokens(aliceNode, bobParty, 30)
        transferTokens(bobNode, charlieParty, 30)
        transferTokens(charlieNode, aliceParty, 30)

        val transactions = getTransactions(aliceNode)

        assertThat(transactions.size, equalTo(7))

        val tokens = getTokens(aliceNode)

        createTokenReIssuanceRequest(aliceNode, tokens)

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest<FungibleToken>>().states[0]

        reIssueRequestedStates(reIssuanceRequest)

        redeemTokens(aliceNode, tokens)

        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode)

        unlockReIssuedTokens(aliceNode, attachmentSecureHash)

        transferTokens(aliceNode, debbieParty, 35)

        val transactionsAfterReIssuance = getTransactions(debbieNode)
        assertThat(transactionsAfterReIssuance.size, equalTo(4))
    }
}
