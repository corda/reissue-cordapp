package com.r3.corda.lib.reissuance.example.tokens

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import com.r3.corda.lib.reissuance.AbstractFlowTest
import org.junit.Test

class TransferTokensTest: AbstractFlowTest() {

    @Test
    fun `Transfer a given quantity of tokens`() {
        initialiseParties()
        issueTokens(aliceParty, 50)
        transferTokens(aliceNode, bobParty, 10)
        assertThat(getTokenQuantity(aliceNode), `is`(40))
        assertThat(getTokenQuantity(bobNode), `is`(10))
    }

    @Test
    fun `Transfer a given quantity of token from states`() {
        initialiseParties()
        issueTokens(aliceParty, 10)
        issueTokens(aliceParty, 10)
        transferTokens(aliceNode, bobParty, 15)
        assertThat(getTokenQuantity(aliceNode), `is`(5))
        assertThat(getTokenQuantity(bobNode), `is`(15))
    }

    @Test
    fun `Transfer tokens many times`() {
        initialiseParties()
        issueTokens(aliceParty, 50)

        transferTokens(aliceNode, bobParty, 50)
        transferTokens(bobNode, charlieParty, 50)
        transferTokens(charlieNode, debbieParty, 50)
        transferTokens(debbieNode, bobParty, 50)
        transferTokens(bobNode, aliceParty, 50)

        assertThat(getTokenQuantity(aliceNode), `is`(50))
        assertThat(getTokenQuantity(bobNode), `is`(0))
        assertThat(getTokenQuantity(charlieNode), `is`(0))
        assertThat(getTokenQuantity(debbieNode), `is`(0))
    }
}