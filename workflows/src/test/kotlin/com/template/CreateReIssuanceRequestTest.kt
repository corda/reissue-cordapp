package com.template

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.template.states.example.SimpleState
import com.template.states.example.StateNeedingAcceptance
import org.junit.Test

class CreateReIssuanceRequestTest: AbstractFlowTest() {

    @Test
    fun `SimpleState re-issuance request is created`() {
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
    }

    @Test
    fun `StateNeedingAcceptance re-issuance request is created`() {
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
    }

    @Test
    fun `Tokens re-issuance request is created`() {
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
    }

}