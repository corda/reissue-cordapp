package com.template

import com.r3.corda.lib.tokens.contracts.commands.IssueTokenCommand
import com.r3.corda.lib.tokens.contracts.commands.MoveTokenCommand
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.template.contracts.example.SimpleStateContract
import com.template.contracts.example.StateNeedingAcceptanceContract
import com.template.flows.*
import com.template.flows.example.simpleState.CreateSimpleState
import com.template.flows.example.simpleState.DeleteSimpleState
import com.template.flows.example.simpleState.UpdateSimpleState
import com.template.flows.example.stateNeedingAcceptance.CreateStateNeedingAcceptance
import com.template.flows.example.stateNeedingAcceptance.DeleteStateNeedingAcceptance
import com.template.flows.example.stateNeedingAcceptance.UpdateStateNeedingAcceptance
import com.template.flows.example.stateNeedingAllParticipantsToSign.CreateStateNeedingAllParticipantsToSign
import com.template.flows.example.stateNeedingAllParticipantsToSign.DeleteStateNeedingAllParticipantsToSign
import com.template.flows.example.stateNeedingAllParticipantsToSign.UpdateStateNeedingAllParticipantsToSign
import com.template.flows.example.tokens.IssueTokens
import com.template.flows.example.tokens.ListTokensFlow
import com.template.flows.example.tokens.RedeemTokens
import com.template.flows.example.tokens.TransferTokens
import com.template.states.ReIssuanceLock
import com.template.states.ReIssuanceRequest
import com.template.states.example.SimpleState
import com.template.states.example.StateNeedingAcceptance
import com.template.states.example.StateNeedingAllParticipantsToSign
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.utilities.getOrThrow
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.DUMMY_NOTARY_NAME
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.MockNetworkNotarySpec
import net.corda.testing.node.internal.*
import org.junit.After
import org.junit.Before


abstract class AbstractFlowTest {

    lateinit var mockNet: InternalMockNetwork

    lateinit var notaryNode: TestStartedNode
    lateinit var issuerNode: TestStartedNode
    lateinit var acceptorNode: TestStartedNode
    lateinit var aliceNode: TestStartedNode
    lateinit var bobNode: TestStartedNode
    lateinit var charlieNode: TestStartedNode
    lateinit var debbieNode: TestStartedNode

    lateinit var notaryParty: Party
    lateinit var issuerParty: Party
    lateinit var acceptorParty: Party
    lateinit var aliceParty: Party
    lateinit var bobParty: Party
    lateinit var charlieParty: Party
    lateinit var debbieParty: Party

    lateinit var issuerLegalName: CordaX500Name
    lateinit var acceptorLegalName: CordaX500Name
    lateinit var aliceLegalName: CordaX500Name
    lateinit var bobLegalName: CordaX500Name
    lateinit var charlieLegalName: CordaX500Name
    lateinit var debbieLegalName: CordaX500Name

    lateinit var allNotaries: List<TestStartedNode>

    lateinit var issuedTokenType: IssuedTokenType

    @Before
    fun setup() {
        mockNet = InternalMockNetwork(
            cordappsForAllNodes = listOf(
                findCordapp("com.template.flows"),
                findCordapp("com.template.contracts"),
                findCordapp("com.r3.corda.lib.accounts.workflows"),
                findCordapp("com.r3.corda.lib.accounts.contracts"),
                findCordapp("com.r3.corda.lib.tokens.contracts")
            ),
            notarySpecs = listOf(MockNetworkNotarySpec(DUMMY_NOTARY_NAME, false)),
            initialNetworkParameters = testNetworkParameters(
                minimumPlatformVersion = 4
            )
        )

        allNotaries = mockNet.notaryNodes
        notaryNode = mockNet.notaryNodes.first()
        notaryParty = notaryNode.info.singleIdentity()

        issuerLegalName = CordaX500Name(organisation = "ISSUER", locality = "London", country = "GB")
        issuerNode = mockNet.createNode(InternalMockNodeParameters(legalName = issuerLegalName))
        issuerParty = issuerNode.info.singleIdentity()

        acceptorLegalName = CordaX500Name(organisation = "ACCEPTOR", locality = "London", country = "GB")
        acceptorNode = mockNet.createNode(InternalMockNodeParameters(legalName = acceptorLegalName))
        acceptorParty = acceptorNode.info.singleIdentity()

        aliceLegalName = CordaX500Name(organisation = "ALICE", locality = "London", country = "GB")
        aliceNode = mockNet.createNode(InternalMockNodeParameters(legalName = aliceLegalName))
        aliceParty = aliceNode.info.singleIdentity()

        bobLegalName = CordaX500Name(organisation = "BOB", locality = "London", country = "GB")
        bobNode = mockNet.createNode(InternalMockNodeParameters(legalName = bobLegalName))
        bobParty = bobNode.info.singleIdentity()

        charlieLegalName = CordaX500Name(organisation = "CHARLIE", locality = "London", country = "GB")
        charlieNode = mockNet.createNode(InternalMockNodeParameters(legalName = charlieLegalName))
        charlieParty = charlieNode.info.singleIdentity()

        debbieLegalName = CordaX500Name(organisation = "DEBBIE", locality = "London", country = "GB")
        debbieNode = mockNet.createNode(InternalMockNodeParameters(legalName = debbieLegalName))
        debbieParty = debbieNode.info.singleIdentity()

        issuedTokenType = IssuedTokenType(issuerParty, TokenType("token", 0))
    }

    @After
    fun tearDown() {
        mockNet.stopNodes()
        System.setProperty("net.corda.node.dbtransactionsresolver.InMemoryResolutionLimit", "0")
    }

    // simple state

    fun createSimpleState(
        owner: Party
    ) {
        val flowFuture = issuerNode.services.startFlow(CreateSimpleState(owner)).resultFuture
        mockNet.runNetwork()
        flowFuture.getOrThrow()
    }

    fun updateSimpleState(
        node: TestStartedNode,
        owner: Party
    ) {
        val simpleStateStateAndRef = getStateAndRefs<SimpleState>(node)[0]
        val flowFuture = node.services.startFlow(UpdateSimpleState(simpleStateStateAndRef, owner)).resultFuture
        mockNet.runNetwork()
        flowFuture.getOrThrow()
    }

    fun deleteSimpleState(
        node: TestStartedNode
    ) {
        val simpleStateStateAndRef = getStateAndRefs<SimpleState>(node)[0]
        val flowFuture = node.services.startFlow(DeleteSimpleState(simpleStateStateAndRef, issuerParty)).resultFuture
        mockNet.runNetwork()
        flowFuture.getOrThrow()
    }

    // state needing acceptance

    fun createStateNeedingAcceptance(
        owner: Party
    ) {
        val flowFuture = issuerNode.services.startFlow(CreateStateNeedingAcceptance(owner, acceptorParty)).resultFuture
        mockNet.runNetwork()
        flowFuture.getOrThrow()
    }

    fun updateStateNeedingAcceptance(
        node: TestStartedNode,
        owner: Party
    ) {
        val stateNeedingAcceptanceStateAndRef = getStateAndRefs<StateNeedingAcceptance>(node)[0]
        val flowFuture = node.services.startFlow(
            UpdateStateNeedingAcceptance(stateNeedingAcceptanceStateAndRef, owner)).resultFuture
        mockNet.runNetwork()
        flowFuture.getOrThrow()
    }

    fun deleteStateNeedingAcceptance(
        node: TestStartedNode
    ) {
        val stateNeedingAcceptanceStateAndRef = getStateAndRefs<StateNeedingAcceptance>(node)[0]
        val flowFuture = node.services.startFlow(DeleteStateNeedingAcceptance(stateNeedingAcceptanceStateAndRef)).resultFuture
        mockNet.runNetwork()
        flowFuture.getOrThrow()
    }

    // state needing all participants to sign

    fun createStateNeedingAllParticipantsToSign(
        owner: Party
    ) {
        val flowFuture = issuerNode.services.startFlow(CreateStateNeedingAllParticipantsToSign(owner, acceptorParty)).resultFuture
        mockNet.runNetwork()
        flowFuture.getOrThrow()
    }

    fun updateStateNeedingAllParticipantsToSign(
        node: TestStartedNode,
        owner: Party
    ) {
        val stateNeedingAllParticipantsToSignStateAndRef = getStateAndRefs<StateNeedingAllParticipantsToSign>(node)[0]
        val flowFuture = node.services.startFlow(
            UpdateStateNeedingAllParticipantsToSign(stateNeedingAllParticipantsToSignStateAndRef, owner)).resultFuture
        mockNet.runNetwork()
        flowFuture.getOrThrow()
    }

    fun deleteStateNeedingAllParticipantsToSign(
        node: TestStartedNode
    ) {
        val stateNeedingAllParticipantsToSignStateAndRef = getStateAndRefs<StateNeedingAllParticipantsToSign>(node)[0]
        val flowFuture = node.services.startFlow(DeleteStateNeedingAllParticipantsToSign(stateNeedingAllParticipantsToSignStateAndRef)).resultFuture
        mockNet.runNetwork()
        flowFuture.getOrThrow()
    }

    // tokens

    fun getTokenQuantity(
        node: TestStartedNode
    ): Int {
        return getTokens(node).sumBy { it.state.data.amount.quantity.toInt() }
    }

    fun issueTokens(
        holder: Party,
        tokenAmount: Long
    ) {
        val flowFuture = issuerNode.services.startFlow(IssueTokens(holder, tokenAmount)).resultFuture
        mockNet.runNetwork()
        flowFuture.getOrThrow()
    }

    fun transferTokens(
        node: TestStartedNode,
        newHolder: Party,
        tokenAmount: Long
    ) {
        val flowFuture = node.services.startFlow(TransferTokens(issuerParty, newHolder, tokenAmount)).resultFuture
        mockNet.runNetwork()
        flowFuture.getOrThrow()
    }

    fun redeemTokens(
        node: TestStartedNode,
        tokens: List<StateAndRef<FungibleToken>>
    ) {
        val flowFuture = node.services.startFlow(RedeemTokens(tokens, issuerParty)).resultFuture
        mockNet.runNetwork()
        flowFuture.getOrThrow()
    }

    fun getTokens(
        node: TestStartedNode,
        encumbered: Boolean = false
    ): List<StateAndRef<FungibleToken>> {
        val tokensFuture = node.services.startFlow(ListTokensFlow()).resultFuture
        mockNet.runNetwork()
        val tokens = tokensFuture.getOrThrow()
        return filterStates(tokens, encumbered)
    }

    // common

    inline fun <reified T : ContractState> getStateAndRefs(
        node: TestStartedNode,
        encumbered: Boolean = false
    ): List<StateAndRef<T>> {
        val states = node.services.vaultService.queryBy<T>().states
        return filterStates(states, encumbered)
    }

    inline fun <reified T : ContractState> filterStates(
        states: List<StateAndRef<T>>,
        encumbered: Boolean
    ): List<StateAndRef<T>> {
        if(encumbered)
            return states.filter { it.state.encumbrance != null }
        return states.filter { it.state.encumbrance == null }
    }

    fun <T> createReIssuanceRequest(
        node: TestStartedNode,
        stateToReIssue: List<StateAndRef<T>>,
        command: CommandData,
        commandSigners: List<Party> = listOf(issuerParty)
    ) where T: ContractState {
        val flowLogic = CreateReIssuanceRequest(issuerParty, stateToReIssue, command, commandSigners)
        val flowFuture = node.services.startFlow(flowLogic).resultFuture
        mockNet.runNetwork()
        flowFuture.getOrThrow()
    }

    inline fun <reified T : ContractState> unlockReIssuedState(
        node: TestStartedNode,
        attachmentSecureHash: SecureHash,
        command: CommandData,
        commandSigners: List<Party> = listOf(node.info.singleIdentity())
    ) {
        val reIssuedStateAndRefs = getStateAndRefs<T>(node, true)
        val lockStateAndRef = getStateAndRefs<ReIssuanceLock<T>>(node, encumbered = true)[0]
        val flowFuture = node.services.startFlow(UnlockReIssuedState(reIssuedStateAndRefs, lockStateAndRef, attachmentSecureHash, command, commandSigners)).resultFuture
        mockNet.runNetwork()
        flowFuture.getOrThrow()
    }

    fun <T> reIssueRequestedStates(
        reIssuanceRequest: StateAndRef<ReIssuanceRequest<T>>
    ) where T: ContractState {
        val flowFuture = issuerNode.services.startFlow(ReIssueState(reIssuanceRequest)).resultFuture
        mockNet.runNetwork()
        flowFuture.getOrThrow()
    }

    fun uploadDeletedStateAttachment(
        node: TestStartedNode
    ): SecureHash {
        val party = node.info.singleIdentity()

        val deleteStateTransaction = getTransactions(node).last()

        val flowFuture = node.services.startFlow(GenerateTransactionByteArray(deleteStateTransaction.id)).resultFuture
        mockNet.runNetwork()
        val transactionInputStream = flowFuture.getOrThrow()

        return node.services.attachments.importAttachment(transactionInputStream.inputStream(), party.toString(), null)
    }

    fun getTransactions(
        node: TestStartedNode
    ): List<LedgerTransaction> {
        return node.services.validatedTransactions.track().snapshot.map {
            it.toLedgerTransaction(aliceNode.services)
        }
    }
}
