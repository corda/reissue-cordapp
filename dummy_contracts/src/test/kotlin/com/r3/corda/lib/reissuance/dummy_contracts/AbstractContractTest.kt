package com.r3.corda.lib.reissuance.dummy_contracts

import com.r3.corda.lib.reissuance.contracts.ReissuanceLockContract
import com.r3.corda.lib.reissuance.contracts.ReissuanceRequestContract
import com.r3.corda.lib.reissuance.dummy_states.SimpleDummyState
import com.r3.corda.lib.reissuance.states.ReissuanceLock
import com.r3.corda.lib.reissuance.states.ReissuanceRequest
import com.r3.corda.lib.reissuance.utils.convertSignedTransactionToByteArray
import com.r3.corda.lib.reissuance.utils.convertWireTransactionToByteArray
import com.r3.corda.lib.tokens.contracts.commands.IssueTokenCommand
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import net.corda.core.contracts.*
import net.corda.core.crypto.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.DUMMY_NOTARY_NAME
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.MockNetworkNotarySpec
import net.corda.testing.node.internal.InternalMockNetwork
import net.corda.testing.node.internal.InternalMockNodeParameters
import net.corda.testing.node.internal.TestStartedNode
import net.corda.testing.node.internal.findCordapp
import net.corda.testing.node.ledger
import org.junit.After
import org.junit.Before
import java.io.ByteArrayInputStream
import java.security.PublicKey
import java.time.Instant

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
            stateRefList.map { createSimpleDummyStateAndRef(it) },
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
            stateRefList.map { createTokenStateAndRef(it) },
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

    fun createDummyReissuanceLock2(
        signableData: SignableData,
        requiredSigners: List<AbstractParty>,
        status: ReissuanceLock.ReissuanceLockStatus = ReissuanceLock.ReissuanceLockStatus.ACTIVE,
        timeWindow: TimeWindow
    ): ReissuanceLock {
        return ReissuanceLock(issuerParty, aliceParty, signableData, status, timeWindow, requiredSigners)
    }

    fun generateWireTransactionByteArrayInputStream(
        wireTransaction: WireTransaction
    ): ByteArrayInputStream {
        return convertWireTransactionToByteArray(wireTransaction).inputStream()
    }

    fun createSignableData(txId: SecureHash = SecureHash.randomSHA256(), key: PublicKey): SignableData {
        return SignableData(txId, SignatureMetadata(notaryNode.services.myInfo.platformVersion, Crypto.findSignatureScheme
            (key).schemeNumberID))
    }

    fun generateSignedTransactionByteArrayInputStream(
        wireTransaction: WireTransaction,
        signersNodes: List<TestStartedNode>
    ): ByteArrayInputStream {
        val sigs = signersNodes.map {
            generateTransactionSignature(it, wireTransaction.id)
        }
        return convertSignedTransactionToByteArray(SignedTransaction(wireTransaction, sigs)).inputStream()
    }

    private fun generateTransactionSignature(node: TestStartedNode, txId: SecureHash): TransactionSignature {
        val signatureMetadata = SignatureMetadata(node.services.myInfo.platformVersion,
            Crypto.findSignatureScheme(node.services.myInfo.legalIdentities.first().owningKey).schemeNumberID)
        val signableData = SignableData(txId, signatureMetadata)
        return node.services.keyManagementService.sign(signableData, node.info.singleIdentity().owningKey)
    }

    fun prepareReissuanceLockState(
        inputContractId: String,
        inputs: List<StateAndRef<ContractState>>,
        reissuanceLockStatus: ReissuanceLock.ReissuanceLockStatus = ReissuanceLock.ReissuanceLockStatus.ACTIVE,
        nodesToSign: List<TestStartedNode> = listOf(aliceNode),
        isSigned: Boolean = false
    ): Pair<ReissuanceLock,
        SecureHash> {

        var tx: WireTransaction? = null
        aliceNode.services.ledger(notary = notaryParty) {
            tx = unverifiedTransaction {
                inputs.forEach {
                    input(inputContractId, it.state.data)
                }
            }
        }

        val dummyReissuanceRequest = if (inputContractId == SimpleDummyStateContract.contractId)
            createSimpleDummyStateReissuanceRequest(tx!!.inputs) else createTokensReissuanceRequest(tx!!.inputs)
        val uploadedSignedTransactionSecureHash = issuerNode.services.attachments.importAttachment(
            if (isSigned) {
                generateSignedTransactionByteArrayInputStream(tx!!, nodesToSign)
            } else {
                generateWireTransactionByteArrayInputStream(tx!!)
            }, aliceParty.toString(), null)

        val reissuanceLock = createDummyReissuanceLock2(
            signableData = createSignableData(tx!!.id, issuerParty.owningKey),
            requiredSigners = listOf(issuerParty, aliceParty),
            timeWindow = TimeWindow.untilOnly(Instant.now().plusSeconds(5)),
            status = reissuanceLockStatus
        )

        issuerNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReissuanceRequestContract.contractId, dummyReissuanceRequest)
            }

            unverifiedTransaction {
                input(ReissuanceRequestContract.contractId, dummyReissuanceRequest)
                inputs.forEachIndexed { index, stateAndRef ->
                    output(inputContractId, reissuedStateLabel(index), contractState = stateAndRef.state.data,
                        encumbrance = index + 1)
                }
                output(
                    ReissuanceLockContract.contractId, reissuanceLockLabel,
                    contractState = reissuanceLock,
                    encumbrance = 0)
                attachment(uploadedSignedTransactionSecureHash)
                command(listOf(issuerParty.owningKey), ReissuanceRequestContract.Commands.Accept())
                command(listOf(issuerParty.owningKey, aliceParty.owningKey), ReissuanceLockContract.Commands.Create())
                if (inputContractId == SimpleDummyStateContract.contractId)
                    command(listOf(issuerParty.owningKey), SimpleDummyStateContract.Commands.Create())
                else command(listOf(issuerParty.owningKey), IssueTokenCommand(issuedTokenType, inputs.indices.toList()))
                timeWindow(TimeWindow.untilOnly(Instant.now().plusSeconds(5)))
            }
        }
        return Pair(reissuanceLock, uploadedSignedTransactionSecureHash)
    }
}
