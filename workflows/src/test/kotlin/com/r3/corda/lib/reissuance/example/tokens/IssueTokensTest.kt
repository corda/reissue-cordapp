package com.r3.corda.lib.reissuance.example.tokens

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import com.r3.corda.lib.reissuance.AbstractFlowTest
import org.junit.Test

class IssueTokensTest: AbstractFlowTest() {

    @Test
    fun `Issued number of issued token type is in holder's vault`() {
        initialiseParties()
        issueTokens(aliceParty, 50)
        assertThat(getTokenQuantity(aliceNode), `is`(50))
    }
}