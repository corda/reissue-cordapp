package com.template.example.tokens

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import com.template.AbstractFlowTest
import org.junit.Test

class IssueTokensTest: AbstractFlowTest() {

    @Test
    fun `Issued number of issued token type is in holder's vault`() {
        initialiseParties()
        issueTokens(aliceParty, 50)
        assertThat(getTokenQuantity(aliceNode), `is`(50))
    }
}