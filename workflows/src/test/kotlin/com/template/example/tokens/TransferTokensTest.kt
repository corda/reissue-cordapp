package com.template.example.tokens

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.template.AbstractFlowTest
import org.junit.Test

class TransferTokensTest: AbstractFlowTest() {

    @Test
    fun `Transfer a given quantity of tokens`() {
        issueTokens(aliceParty, 50)
        transferTokens(aliceNode, bobParty, 10)
        assertThat(getTokenQuantity(aliceNode), equalTo(40))
        assertThat(getTokenQuantity(bobNode), equalTo(10))
    }

    @Test
    fun `Transfer a given quantity of token from states`() {
        issueTokens(aliceParty, 10)
        issueTokens(aliceParty, 10)
        transferTokens(aliceNode, bobParty, 15)
        assertThat(getTokenQuantity(aliceNode), equalTo(5))
        assertThat(getTokenQuantity(bobNode), equalTo(15))
    }

    @Test
    fun `Transfer tokens many times`() {
        issueTokens(aliceParty, 50)

        transferTokens(aliceNode, bobParty, 50)
        transferTokens(bobNode, charlieParty, 50)
        transferTokens(charlieNode, debbieParty, 50)
        transferTokens(debbieNode, bobParty, 50)
        transferTokens(bobNode, aliceParty, 50)

        assertThat(getTokenQuantity(aliceNode), equalTo(50))
        assertThat(getTokenQuantity(bobNode), equalTo(0))
        assertThat(getTokenQuantity(charlieNode), equalTo(0))
        assertThat(getTokenQuantity(debbieNode), equalTo(0))
    }
}