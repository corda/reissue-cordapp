package com.template.contracts

import com.r3.corda.lib.tokens.contracts.commands.IssueTokenCommand
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.template.contracts.example.SimpleDummyStateContract
import com.template.states.ReIssuanceLock
import com.template.states.ReIssuanceRequest
import com.template.states.example.SimpleDummyState
import com.template.utils.convertSignedTransactionToByteArray
import net.corda.core.contracts.*
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
import java.io.ByteArrayInputStream
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
    fun reIssuedStateLabel(id: Int) = "re-issued state $id encumbered by re-issuance lock"

    lateinit var issuedTokenType: IssuedTokenType

    @Before
    fun initialize() {
        mockNet = InternalMockNetwork(
            cordappsForAllNodes = listOf(
                findCordapp("net.corda.testing.contracts"),
                findCordapp("com.r3.corda.lib.tokens.contracts"),
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

        issuedTokenType = IssuedTokenType(issuerParty, TokenType("token", 0))
    }

    @After
    fun tearDown() {
        mockNet.stopNodes()
    }

    fun createSimpleDummyState(): SimpleDummyState {
        return SimpleDummyState(aliceParty)
    }

    fun createToken(): FungibleToken {
        val issuedTokenType = IssuedTokenType(issuerParty, TokenType("token", 0))
        val amount = Amount(10, issuedTokenType)
        return FungibleToken(amount, aliceParty)
    }

    fun createDummyRef(): StateRef {
        return StateRef(SecureHash.randomSHA256(), 0)
    }

    fun createDummySimpleStateReIssuanceRequest(
        stateRefList: List<StateRef>
    ): ReIssuanceRequest {
        return ReIssuanceRequest(
            issuerParty,
            aliceParty,
            stateRefList,
            SimpleDummyStateContract.Commands.Create(),
            listOf(issuerParty)
        )
    }

    fun createTokensReIssuanceRequest(
        stateRefList: List<StateRef>
    ): ReIssuanceRequest {
        return ReIssuanceRequest(
            issuerParty,
            aliceParty,
            stateRefList,
            IssueTokenCommand(issuedTokenType, stateRefList.indices.toList()),
            listOf(issuerParty)
        )
    }

    fun createSimpleDummyStateAndRef(
        stateRef: StateRef = createDummyRef()
    ): StateAndRef<SimpleDummyState> {
        val dummyTransactionState = TransactionState(data = createSimpleDummyState(), notary = notaryParty)
        return StateAndRef(dummyTransactionState, stateRef)
    }

    fun createTokenStateAndRef(
        stateRef: StateRef = createDummyRef()
    ): StateAndRef<FungibleToken> {
        val dummyTransactionState = TransactionState(data = createToken(), notary = notaryParty)
        return StateAndRef(dummyTransactionState, stateRef)
    }

    fun <T> createDummyReIssuanceLock(
        stateAndRefList: List<StateAndRef<T>>,
        status: ReIssuanceLock.ReIssuanceLockStatus = ReIssuanceLock.ReIssuanceLockStatus.ACTIVE
    ): ReIssuanceLock<T> where T: ContractState {
        return ReIssuanceLock(issuerParty, aliceParty, stateAndRefList, status)
    }

    fun generateSignedTransactionByteArrayInputStream(
        signedTransaction: SignedTransaction
    ): ByteArrayInputStream {
        return convertSignedTransactionToByteArray(signedTransaction).inputStream()
    }
}
