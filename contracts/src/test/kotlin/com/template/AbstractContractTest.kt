package com.template

import com.template.contracts.example.SimpleStateContract
import com.template.states.ReIssuanceLock
import com.template.states.ReIssuanceRequest
import com.template.states.example.SimpleState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.serialization.serialize
import net.corda.core.transactions.SignedTransaction
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.DUMMY_NOTARY_NAME
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.MockNetworkNotarySpec
import net.corda.testing.node.internal.InternalMockNetwork
import net.corda.testing.node.internal.InternalMockNodeParameters
import net.corda.testing.node.internal.TestStartedNode
import net.corda.testing.node.internal.findCordapp
import org.junit.After
import org.junit.Before
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

abstract class AbstractContractTest {

    lateinit var mockNet: InternalMockNetwork

    lateinit var notaryNode: TestStartedNode
    lateinit var issuerNode: TestStartedNode
    lateinit var aliceNode: TestStartedNode

    lateinit var notaryParty: Party
    lateinit var issuerParty: Party
    lateinit var aliceParty: Party

    val reIssuanceLockLabel = "re-issuance lock"
    val reIssuedStateLabel = "re-issued state encumbered by re-issuance lock"

    @Before
    fun initialize() {
        mockNet = InternalMockNetwork(
            cordappsForAllNodes = listOf(
                findCordapp("net.corda.testing.contracts"),
                findCordapp("com.template.contracts")
            ),
            notarySpecs = listOf(MockNetworkNotarySpec(DUMMY_NOTARY_NAME, false)),
            initialNetworkParameters = testNetworkParameters(
                minimumPlatformVersion = 4
            )
        )

        notaryNode = mockNet.notaryNodes.first()
        notaryParty = notaryNode.info.singleIdentity()

        val issuerLegalName = CordaX500Name(organisation = "ISSUER", locality = "London", country = "GB")
        issuerNode = mockNet.createNode(InternalMockNodeParameters(legalName = issuerLegalName))
        issuerParty = issuerNode.info.singleIdentity()

        val aliceLegalName = CordaX500Name(organisation = "ALICE", locality = "London", country = "GB")
        aliceNode = mockNet.createNode(InternalMockNodeParameters(legalName = aliceLegalName))
        aliceParty = aliceNode.info.singleIdentity()

    }

    @After
    fun tearDown() {
        mockNet.stopNodes()
    }

    fun createDummyState(): SimpleState {
        return SimpleState(aliceParty)
    }

    fun createDummyRef(): StateRef {
        return StateRef(SecureHash.randomSHA256(), 0)
    }

    fun createDummyReIssuanceRequest(
        stateRefList: List<StateRef>
    ): ReIssuanceRequest {
        return ReIssuanceRequest(
            issuerParty,
            aliceParty,
            stateRefList,
            SimpleStateContract.Commands.Create(),
            listOf(issuerParty)
        )
    }

    fun createDummyReIssuanceLock(
        stateRef: StateRef = createDummyRef()
    ): ReIssuanceLock<SimpleState> {
        val dummyTransactionState = TransactionState(data = createDummyState(), notary = notaryParty)
        val dummyStateAndRef = StateAndRef(dummyTransactionState, stateRef)
        return ReIssuanceLock(issuerParty, aliceParty, listOf(dummyStateAndRef))
    }

    // TODO: repeated code (copied over from GenerateTransactionByteArray flow)
    fun generateSignedTransactionByteArray(signedTransaction: SignedTransaction): ByteArray {
        val serializedSignedTransactionBytes = signedTransaction.serialize().bytes

        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            val entry = ZipEntry("SignedTransaction")
            zos.putNextEntry(entry)
            zos.write(serializedSignedTransactionBytes)
            zos.closeEntry()
        }
        baos.close()

        return baos.toByteArray()
    }
}
