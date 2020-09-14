package com.template

import com.template.contracts.example.SimpleStateContract
import com.template.states.ReIssuanceLock
import com.template.states.ReIssuanceRequest
import com.template.states.example.SimpleState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import net.corda.core.serialization.serialize
import net.corda.core.transactions.SignedTransaction
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.DUMMY_NOTARY_NAME
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import org.junit.Before
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

abstract class AbstractContractTest {
    protected val dummyNotary = TestIdentity(DUMMY_NOTARY_NAME, 20)
    protected val ledgerServices = MockServices(
        firstIdentity = dummyNotary,
        networkParameters = testNetworkParameters(
            minimumPlatformVersion = 4
        ),
        cordappPackages = listOf(
            "net.corda.testing.contracts",
            "com.template.contracts"
        )
    )

    protected lateinit var notaryParty: Party
    protected lateinit var issuerParty: AbstractParty
    protected lateinit var aliceParty: AbstractParty
    protected lateinit var bobParty: AbstractParty

    @Before
    fun initialize() {
        notaryParty = dummyNotary.party
        issuerParty = AnonymousParty(Crypto.generateKeyPair().public)
        aliceParty = AnonymousParty(Crypto.generateKeyPair().public)
        bobParty = AnonymousParty(Crypto.generateKeyPair().public)
    }

    fun createDummyState(): SimpleState {
        return SimpleState(aliceParty)
    }

    fun createDummyRef(): StateRef {
        return StateRef(SecureHash.randomSHA256(), 0)
    }

    fun createDummyReIssuanceRequest(): ReIssuanceRequest {
        return ReIssuanceRequest(
            issuerParty,
            aliceParty,
            listOf(createDummyRef()),
            SimpleStateContract.Commands.Create(),
            listOf(issuerParty)
        )
    }

    fun createDummyReIssuanceLock(): ReIssuanceLock<SimpleState> {
        val dummyTransactionState = TransactionState(data = createDummyState(), notary = notaryParty)
        val dummyStateAndRef = StateAndRef(dummyTransactionState, createDummyRef())
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
