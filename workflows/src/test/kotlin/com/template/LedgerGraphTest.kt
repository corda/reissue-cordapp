package com.template

import com.r3.dr.ledgergraph.services.LedgerGraphService
import net.corda.core.identity.CordaX500Name
import net.corda.testing.node.*
import net.corda.testing.node.internal.findCordapp
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import java.util.concurrent.TimeUnit

/**
 * A test utility class providing a [MockNetwork] with a preconfigured set of rules as well as two MockNodes
 * for testing purposes.
 */
class LedgerGraphTest {

    val network = MockNetwork(MockNetworkParameters(
        notarySpecs = listOf(MockNetworkNotarySpec(CordaX500Name("Notary", "London", "GB"))),
        cordappsForAllNodes = listOf(
            findCordapp("com.r3.corda.lib.tokens.contracts"),
            findCordapp("com.r3.corda.lib.tokens.workflows"),
            findCordapp("com.r3.corda.lib.tokens.money"),
            findCordapp("com.r3.corda.lib.tokens.selection"),
            findCordapp("com.r3.corda.lib.accounts.contracts"),
            findCordapp("com.r3.corda.lib.accounts.workflows"),
            findCordapp("com.r3.corda.lib.ci.workflows"),
            findCordapp("com.template.flows"),
            findCordapp("com.template.contracts"),
            findCordapp("com.r3.dr.ledgergraph")
        )
    ))

    val nodeA = network.createNode(MockNodeParameters())
    val nodeB = network.createNode(MockNodeParameters())

    val partyA = nodeA.info.legalIdentities.first()
    val partyB = nodeB.info.legalIdentities.first()

    /**
     * Run the network before each test for cleanup and reset.
     */
    @Before
    fun setup() {
        nodeA.services.cordaService(LedgerGraphService::class.java).waitForInitialization()
        nodeB.services.cordaService(LedgerGraphService::class.java).waitForInitialization()
    }

    /**
     * Run the network after each test for cleanup and reset.
     */
    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun test() {
        println()
    }

}
