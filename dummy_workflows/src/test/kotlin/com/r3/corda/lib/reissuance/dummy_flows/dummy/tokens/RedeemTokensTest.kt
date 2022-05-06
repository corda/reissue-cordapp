package com.r3.corda.lib.reissuance.dummy_flows.dummy.tokens

import com.r3.corda.lib.reissuance.dummy_flows.AbstractFlowTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
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