package com.template.example.tokens

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import com.template.AbstractFlowTest
import org.junit.Test

class RedeemTokensTest: AbstractFlowTest() {

    @Test
    fun `All tokens are redeemed`() {
        initialiseParties()
        issueTokens(aliceParty, 50)
        val tokens = getTokens(aliceNode)
        redeemTokens(aliceNode, tokens)
        assertThat(getTokenQuantity(aliceNode), `is`(0))
    }

    @Test
    fun `Some of tokens are redeemed`() {
        initialiseParties()
        issueTokens(aliceParty, 10)
        issueTokens(aliceParty, 10)
        val tokens = getTokens(aliceNode)
        redeemTokens(aliceNode, listOf(tokens[0]))
        assertThat(getTokenQuantity(aliceNode), `is`(10))
    }
}