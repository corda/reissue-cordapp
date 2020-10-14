package com.r3.corda.lib.reissuance.dummy_flows

import net.corda.core.crypto.SecureHash
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.Test

class GetTransactionBackChainTest: AbstractFlowTest() {

    @Test
    fun `Only issuance transaction is in back-chain of newly issued state`() {
        initialiseParties()
        val transactionId = createSimpleDummyState(aliceParty)
        val transactionBackChain = getTransactionBackChain(aliceNode, transactionId)
        assertThat(transactionBackChain, hasSize(`is`(1)))
        assertThat(transactionBackChain, hasItems(transactionId))
    }

    @Test
    fun `Back-chain for linearly updated state is correct`() {
        initialiseParties()
        val transactionIds = mutableListOf<SecureHash>()
        transactionIds.add(createSimpleDummyState(aliceParty))
        transactionIds.add(updateSimpleDummyState(aliceNode, bobParty))
        transactionIds.add(updateSimpleDummyState(bobNode, charlieParty))
        transactionIds.add(updateSimpleDummyState(charlieNode, debbieParty))
        transactionIds.add(updateSimpleDummyState(debbieNode, charlieParty))
        transactionIds.add(updateSimpleDummyState(charlieNode, bobParty))
        transactionIds.add(updateSimpleDummyState(bobNode, aliceParty))

        val transactionBackChain = getTransactionBackChain(aliceNode, transactionIds.last())
        assertThat(transactionBackChain, hasSize(`is`(transactionIds.size)))
        assertThat(transactionBackChain, hasItems(*transactionIds.toTypedArray()))
    }

    @Test
    fun `Back-chain for not linearly updated state is correct`() {
        initialiseParties()
        val transactionIds = mutableListOf<SecureHash>()
        transactionIds.add(issueTokens(aliceParty, 50))
        transactionIds.add(transferTokens(aliceNode, bobParty, 20))
        transactionIds.add(transferTokens(aliceNode, charlieParty, 20))
        transactionIds.add(transferTokens(bobNode, charlieParty, 10))
        transactionIds.add(transferTokens(charlieNode, aliceParty, 25))
        transactionIds.add(transferTokens(aliceNode, bobParty, 15))
        transactionIds.add(transferTokens(aliceNode, charlieParty, 15))
        transactionIds.add(transferTokens(charlieNode, aliceParty, 10))

        val transactionBackChain = getTransactionBackChain(aliceNode, transactionIds.last())
        assertThat(transactionBackChain, hasSize(`is`(transactionIds.size)))
        assertThat(transactionBackChain, hasItems(*transactionIds.toTypedArray()))
    }

}
