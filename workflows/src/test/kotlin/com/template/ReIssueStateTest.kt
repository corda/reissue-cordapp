package com.template

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.template.states.ReIssuanceRequest
import com.template.states.example.SimpleState
import com.template.states.example.StateNeedingAcceptance
import net.corda.core.node.services.queryBy
import org.junit.Test

class ReIssueStateTest: AbstractFlowTest() {

    @Test
    fun `SimpleState is re-issued`() {
        createSimpleState(aliceParty)
        updateSimpleState(aliceNode, bobParty)
        updateSimpleState(bobNode, charlieParty)
        updateSimpleState(charlieNode, debbieParty)
        updateSimpleState(debbieNode, charlieParty)
        updateSimpleState(charlieNode, bobParty)
        updateSimpleState(bobNode, aliceParty)

        val transactions = getTransactions(aliceNode)

        assertThat(transactions.size, equalTo(7))

        val simpleStateStateAndRef = getStateAndRefs<SimpleState>(aliceNode)[0]

        createSimpleStateReIssuanceRequest(aliceNode, simpleStateStateAndRef)

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest<SimpleState>>().states[0]

        reIssueRequestedStates(reIssuanceRequest)

    }

    @Test
    fun `StateNeedingAcceptance is re-issued`() {
        createStateNeedingAcceptance(aliceParty)
        updateStateNeedingAcceptance(aliceNode, bobParty)
        updateStateNeedingAcceptance(bobNode, charlieParty)
        updateStateNeedingAcceptance(charlieNode, debbieParty)
        updateStateNeedingAcceptance(debbieNode, charlieParty)
        updateStateNeedingAcceptance(charlieNode, bobParty)
        updateStateNeedingAcceptance(bobNode, aliceParty)

        val transactions = getTransactions(aliceNode)

        assertThat(transactions.size, equalTo(7))

        val stateNeedingAcceptanceStateAndRef = getStateAndRefs<StateNeedingAcceptance>(aliceNode)[0]

        createStateNeedingAcceptanceReIssuanceRequest(aliceNode, stateNeedingAcceptanceStateAndRef)

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest<StateNeedingAcceptance>>().states[0]

        reIssueRequestedStates(reIssuanceRequest)
    }

    @Test
    fun `Tokens are re-issued`() {
        issueTokens(aliceParty, 50)

        transferTokens(aliceNode, bobParty, 50)
        transferTokens(bobNode, charlieParty, 50)
        transferTokens(charlieNode, debbieParty, 50)
        transferTokens(debbieNode, bobParty, 50)
        transferTokens(bobNode, aliceParty, 50)

        val transactions = getTransactions(aliceNode)

        assertThat(transactions.size, equalTo(6))

        val tokens = getTokens(aliceNode)

        createTokenReIssuanceRequest(aliceNode, tokens)

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest<FungibleToken>>().states[0]

        reIssueRequestedStates(reIssuanceRequest)
    }

}