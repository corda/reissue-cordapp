package com.r3.corda.lib.reissuance.dummy_contracts

import com.r3.corda.lib.reissuance.states.ReissuanceLock
import com.r3.corda.lib.reissuance.states.ReissuanceRequest
import com.r3.corda.lib.reissuance.dummy_states.SimpleDummyState
import com.r3.corda.lib.reissuance.utils.convertSignedTransactionToByteArray
import com.r3.corda.lib.tokens.contracts.commands.IssueTokenCommand
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import net.corda.core.contracts.*
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
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

abstract class AbstractContractTest {

    lateinit var mockNet: InternalMockNetwork

    lateinit var notaryNode: TestStartedNode
    lateinit var issuerNode: TestStartedNode
    lateinit var aliceNode: TestStartedNode

    lateinit var notaryParty: Party
    lateinit var issuerParty: Party
    lateinit var aliceParty: Party

    val reissuanceLockLabel = "re-issuance lock"
    val reissuedStateLabel = "re-issued state encumbered by re-issuance lock"
    fun reissuedStateLabel(id: Int) = "re-issued state $id encumbered by re-issuance lock"

    lateinit var issuedTokenType: IssuedTokenType

    @Before
    fun initialize() {
        mockNet = InternalMockNetwork(
            cordappsForAllNodes = listOf(
                findCordapp("net.corda.testing.contracts"),
                findCordapp("com.r3.corda.lib.tokens.contracts"),
                findCordapp("com.r3.corda.lib.reissuance.contracts"),
                findCordapp("com.r3.corda.lib.reissuance.dummy_contracts")
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

    fun createSimpleDummyStateReissuanceRequest(
        stateRefList: List<StateRef>
    ): ReissuanceRequest {
        return ReissuanceRequest(
            issuerParty,
            aliceParty,
            stateRefList,
            SimpleDummyStateContract.Commands.Create(),
            listOf(issuerParty)
        )
    }

    fun createTokensReissuanceRequest(
        stateRefList: List<StateRef>
    ): ReissuanceRequest {
        return ReissuanceRequest(
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

    fun <T> createDummyReissuanceLock(
        stateAndRefList: List<StateAndRef<T>>,
        requiredSigners: List<AbstractParty>,
        status: ReissuanceLock.ReissuanceLockStatus = ReissuanceLock.ReissuanceLockStatus.ACTIVE
    ): ReissuanceLock<T> where T: ContractState {
        return ReissuanceLock(issuerParty, aliceParty, stateAndRefList, requiredSigners, status)
    }

    fun generateSignedTransactionByteArrayInputStream(
        signedTransaction: SignedTransaction
    ): ByteArrayInputStream {
        return convertSignedTransactionToByteArray(signedTransaction).inputStream()
    }
}
