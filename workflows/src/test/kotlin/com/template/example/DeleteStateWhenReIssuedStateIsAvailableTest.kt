package com.template.example

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.template.AbstractFlowTest
import com.template.states.ReIssuanceRequest
import com.template.states.example.SimpleState
import net.corda.core.node.services.queryBy
import org.junit.Test

class DeleteStateWhenReIssuedStateIsAvailableTest: AbstractFlowTest() {

    @Test
    fun `SimpleState is deleted once re-issued state is available`() {
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

        deleteSimpleState(aliceNode)

    }

    @Test
    fun `Token is deleted once re-issued state is available`() {
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

        redeemTokens(aliceNode, tokens)
    }
}