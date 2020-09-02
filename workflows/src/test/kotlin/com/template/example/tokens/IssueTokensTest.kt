package com.template.example.tokens

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.template.AbstractFlowTest
import org.junit.Test

class IssueTokensTest: AbstractFlowTest() {

    @Test
    fun `Issued number of issued token type is in holder's vault`() {
        issueTokens(aliceParty, 50)
        assertThat(getTokenQuantity(aliceNode), equalTo(50))
    }
}