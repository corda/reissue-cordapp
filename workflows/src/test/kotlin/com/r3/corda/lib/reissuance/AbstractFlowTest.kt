package com.r3.corda.lib.reissuance

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.r3.corda.lib.accounts.workflows.internal.accountService
import com.r3.corda.lib.ci.workflows.SyncKeyMappingInitiator
import com.r3.corda.lib.reissuance.flows.*
import com.r3.corda.lib.reissuance.flows.example.dummyStateRequiringAcceptance.CreateDummyStateRequiringAcceptance
import com.r3.corda.lib.reissuance.flows.example.dummyStateRequiringAcceptance.DeleteDummyStateRequiringAcceptance
import com.r3.corda.lib.reissuance.flows.example.dummyStateRequiringAcceptance.UpdateDummyStateRequiringAcceptance
import com.r3.corda.lib.reissuance.flows.example.dummyStateRequiringAllParticipantsSignatures.CreateDummyStateRequiringAllParticipantsSignatures
import com.r3.corda.lib.reissuance.flows.example.dummyStateRequiringAllParticipantsSignatures.DeleteDummyStateRequiringAllParticipantsSignatures
import com.r3.corda.lib.reissuance.flows.example.dummyStateRequiringAllParticipantsSignatures.UpdateDummyStateRequiringAllParticipantsSignatures
import com.r3.corda.lib.reissuance.flows.example.dummyStateWithInvalidEqualsMethod.CreateDummyStateWithInvalidEqualsMethod
import com.r3.corda.lib.reissuance.flows.example.dummyStateWithInvalidEqualsMethod.DeleteDummyStateWithInvalidEqualsMethod
import com.r3.corda.lib.reissuance.flows.example.dummyStateWithInvalidEqualsMethod.UpdateDummyStateWithInvalidEqualsMethod
import com.r3.corda.lib.reissuance.flows.example.simpleDummyState.*
import com.r3.corda.lib.reissuance.flows.example.tokens.IssueTokens
import com.r3.corda.lib.reissuance.flows.example.tokens.ListTokensFlow
import com.r3.corda.lib.reissuance.flows.example.tokens.RedeemTokens
import com.r3.corda.lib.reissuance.flows.example.tokens.TransferTokens
import com.r3.corda.lib.reissuance.states.ReIssuanceLock
import com.r3.corda.lib.reissuance.states.ReIssuanceRequest
import com.r3.corda.lib.reissuance.states.example.DummyStateRequiringAcceptance
import com.r3.corda.lib.reissuance.states.example.DummyStateRequiringAllParticipantsSignatures
import com.r3.corda.lib.reissuance.states.example.DummyStateWithInvalidEqualsMethod
import com.r3.corda.lib.reissuance.states.example.SimpleDummyState
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.dr.ledgergraph.services.LedgerGraphService
import net.corda.core.concurrent.CordaFuture
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.DUMMY_NOTARY_NAME
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.MockNetworkNotarySpec
import net.corda.testing.node.internal.*
import org.junit.After
import org.junit.Before
import java.util.*


abstract class AbstractFlowTest {

    lateinit var mockNet: InternalMockNetwork

    lateinit var notaryNode: TestStartedNode
    lateinit var issuerNode: TestStartedNode
    lateinit var acceptorNode: TestStartedNode
    lateinit var aliceNode: TestStartedNode
    lateinit var bobNode: TestStartedNode
    lateinit var charlieNode: TestStartedNode
    lateinit var debbieNode: TestStartedNode
    lateinit var employeeNode: TestStartedNode

    lateinit var notaryParty: Party
    lateinit var issuerParty: Party
    lateinit var acceptorParty: Party
    lateinit var aliceParty: Party
    lateinit var bobParty: Party
    lateinit var charlieParty: Party
    lateinit var debbieParty: Party
    lateinit var employeeParty: Party

    lateinit var employeeIssuerParty: AbstractParty
    lateinit var employeeAliceParty: AbstractParty
    lateinit var employeeBobParty: AbstractParty
    lateinit var employeeCharlieParty: AbstractParty
    lateinit var employeeDebbieParty: AbstractParty

    lateinit var employeeIssuerAccount: AccountInfo
    lateinit var employeeAliceAccount: AccountInfo
    lateinit var employeeBobAccount: AccountInfo
    lateinit var employeeCharlieAccount: AccountInfo
    lateinit var employeeDebbieAccount: AccountInfo

    lateinit var issuerLegalName: CordaX500Name
    lateinit var acceptorLegalName: CordaX500Name
    lateinit var aliceLegalName: CordaX500Name
    lateinit var bobLegalName: CordaX500Name
    lateinit var charlieLegalName: CordaX500Name
    lateinit var debbieLegalName: CordaX500Name
    lateinit var employeeLegalName: CordaX500Name

    lateinit var allNotaries: List<TestStartedNode>

    lateinit var issuedTokenType: IssuedTokenType

    @Before
    fun setup() {
        mockNet = InternalMockNetwork(
            cordappsForAllNodes = listOf(
                findCordapp("com.r3.corda.lib.tokens.contracts"),
                findCordapp("com.r3.corda.lib.tokens.workflows"),
                findCordapp("com.r3.corda.lib.tokens.money"),
                findCordapp("com.r3.corda.lib.tokens.selection"),
                findCordapp("com.r3.corda.lib.accounts.contracts"),
                findCordapp("com.r3.corda.lib.accounts.workflows"),
                findCordapp("com.r3.corda.lib.ci.workflows"),
                findCordapp("com.r3.corda.lib.reissuance.contracts"),
                findCordapp("com.r3.corda.lib.reissuance.flows"),
                findCordapp("com.r3.dr.ledgergraph")
            ),
            notarySpecs = listOf(MockNetworkNotarySpec(DUMMY_NOTARY_NAME, false)),
            initialNetworkParameters = testNetworkParameters(
                minimumPlatformVersion = 4
            )
        )

        allNotaries = mockNet.notaryNodes
        notaryNode = mockNet.notaryNodes.first()
        notaryParty = notaryNode.info.singleIdentity()

    }

    @After
    fun tearDown() {
        mockNet.stopNodes()
        System.setProperty("net.corda.node.dbtransactionsresolver.InMemoryResolutionLimit", "0")
    }

    fun initialiseParties() {
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

        aliceNode.services.cordaService(LedgerGraphService::class.java).waitForInitialization()
    }

    fun initialisePartiesForAccountsOnTheSameHost() {
        employeeLegalName = CordaX500Name(organisation = "EMPLOYEE", locality = "London", country = "GB")
        employeeNode = mockNet.createNode(InternalMockNodeParameters(legalName = employeeLegalName))
        employeeParty = employeeNode.info.singleIdentity()

        employeeIssuerAccount = createAccount(employeeNode, "Issuer")
        employeeAliceAccount = createAccount(employeeNode, "Alice")
        employeeBobAccount = createAccount(employeeNode, "Bob")
        employeeCharlieAccount = createAccount(employeeNode, "Charlie")
        employeeDebbieAccount = createAccount(employeeNode, "Debbie")

        employeeIssuerParty = getPartyForAccount(employeeNode, employeeIssuerAccount)
        employeeAliceParty = getPartyForAccount(employeeNode, employeeAliceAccount)
        employeeBobParty = getPartyForAccount(employeeNode, employeeBobAccount)
        employeeCharlieParty = getPartyForAccount(employeeNode, employeeCharlieAccount)
        employeeDebbieParty = getPartyForAccount(employeeNode, employeeDebbieAccount)
    }

    fun initialisePartiesForAccountsOnDifferentHosts() {
        initialiseParties()

        employeeIssuerAccount = createAccount(issuerNode, "Issuer")
        employeeAliceAccount = createAccount(aliceNode, "Alice")
        employeeBobAccount = createAccount(bobNode, "Bob")
        employeeCharlieAccount = createAccount(charlieNode, "Charlie")
        employeeDebbieAccount = createAccount(debbieNode, "Debbie")

        employeeIssuerParty = getPartyForAccount(issuerNode, employeeIssuerAccount)
        employeeAliceParty = getPartyForAccount(aliceNode, employeeAliceAccount)
        employeeBobParty = getPartyForAccount(bobNode, employeeBobAccount)
        employeeCharlieParty = getPartyForAccount(charlieNode, employeeCharlieAccount)
        employeeDebbieParty = getPartyForAccount(debbieNode, employeeDebbieAccount)

        shareAccountInfo(issuerNode, employeeIssuerAccount, listOf(aliceParty, bobParty, charlieParty, debbieParty))
        shareAccountInfo(aliceNode, employeeAliceAccount, listOf(issuerParty, bobParty, charlieParty, debbieParty))
        shareAccountInfo(bobNode, employeeBobAccount, listOf(issuerParty, aliceParty, charlieParty, debbieParty))
        shareAccountInfo(charlieNode, employeeCharlieAccount, listOf(issuerParty, aliceParty, bobParty, debbieParty))
        shareAccountInfo(debbieNode, employeeDebbieAccount, listOf(issuerParty, aliceParty, bobParty, charlieParty))

        inform(issuerNode, employeeIssuerAccount, employeeIssuerParty, listOf(aliceNode, bobNode, charlieNode, debbieNode))
        inform(aliceNode, employeeAliceAccount, employeeAliceParty, listOf(issuerNode, bobNode, charlieNode, debbieNode))
        inform(bobNode, employeeBobAccount, employeeBobParty, listOf(issuerNode, aliceNode, charlieNode, debbieNode))
        inform(charlieNode, employeeCharlieAccount, employeeCharlieParty, listOf(issuerNode, aliceNode, bobNode, debbieNode))
        inform(debbieNode, employeeDebbieAccount, employeeDebbieParty, listOf(issuerNode, aliceNode, bobNode, charlieNode))

    }

    // accounts

    fun createAccount(
        node: TestStartedNode,
        accountName: String
    ): AccountInfo {
        val accountFuture = node.services.accountService.createAccount(name = accountName)
        mockNet.runNetwork()
        return accountFuture.getOrThrow().state.data
    }

    fun getPartyForAccount(
        node: TestStartedNode,
        account: AccountInfo
    ): AnonymousParty {
        return runFlow(
            node,
            RequestKeyForAccount(account)
        )
    }

    fun shareAccountInfo(
        node: TestStartedNode,
        account: AccountInfo,
        parties: List<Party>
    ) {
        parties.forEach {
            node.services.accountService.shareAccountInfoWithParty(account.identifier.id, it)
        }
    }

    fun inform(
        host: TestStartedNode,
        accountInfo: AccountInfo,
        accountParty: AbstractParty,
        others: List<TestStartedNode>
    ) {
        if (!host.info.legalIdentities[0].equals(accountInfo!!.host)) {
            throw IllegalArgumentException("hosts do not match")
        }
        for (other in others) {
            val future: CordaFuture<*> = host.services.startFlow(SyncKeyMappingInitiator(
                other.info.legalIdentities[0],
                Collections.singletonList(accountParty))).resultFuture
            mockNet.runNetwork()
            future.get()
        }
    }

    // simple state

    fun createSimpleDummyState(
        owner: Party
    ) {
        runFlow(
            issuerNode,
            CreateSimpleDummyState(owner)
        )
    }

    fun createSimpleDummyStateForAccount(
        node: TestStartedNode,
        owner: AbstractParty
    ) {
        runFlow(
            node,
            CreateSimpleDummyStateForAccount(employeeIssuerParty, owner)
        )
    }

    fun updateSimpleDummyState(
        node: TestStartedNode,
        owner: Party
    ) {
        val simpleDummyStateStateAndRef = getStateAndRefs<SimpleDummyState>(node)[0]
        runFlow(
            node,
            UpdateSimpleDummyState(simpleDummyStateStateAndRef, owner)
        )
    }

    fun updateSimpleDummyStateForAccount(
        node: TestStartedNode,
        owner: AbstractParty
    ) {
        val simpleDummyStateStateAndRef = getStateAndRefs<SimpleDummyState>(node)[0]
        runFlow(
            node,
            UpdateSimpleDummyStateForAccount(simpleDummyStateStateAndRef, owner)
        )
    }

    fun deleteSimpleDummyState(
        node: TestStartedNode
    ) {
        val simpleDummyStateStateAndRef = getStateAndRefs<SimpleDummyState>(node)[0]
        runFlow(
            node,
            DeleteSimpleDummyState(simpleDummyStateStateAndRef, issuerParty)
        )
    }

    fun deleteSimpleDummyStateForAccount(
        node: TestStartedNode
    ) {
        val simpleDummyStateStateAndRef = getStateAndRefs<SimpleDummyState>(node)[0]
        runFlow(
            node,
            DeleteSimpleDummyStateForAccount(simpleDummyStateStateAndRef)
        )
    }

    // dummy state requiring acceptance

    fun createDummyStateRequiringAcceptance(
        owner: Party
    ) {
        runFlow(
            issuerNode,
            CreateDummyStateRequiringAcceptance(owner, acceptorParty)
        )
    }

    fun updateDummyStateRequiringAcceptance(
        node: TestStartedNode,
        owner: Party
    ) {
        val dummyStateRequiringAcceptanceStateAndRef = getStateAndRefs<DummyStateRequiringAcceptance>(node)[0]
        runFlow(
            node,
            UpdateDummyStateRequiringAcceptance(dummyStateRequiringAcceptanceStateAndRef, owner)
        )
    }

    fun deleteDummyStateRequiringAcceptance(
        node: TestStartedNode
    ) {
        val dummyStateRequiringAcceptanceStateAndRef = getStateAndRefs<DummyStateRequiringAcceptance>(node)[0]
        runFlow(
            node,
            DeleteDummyStateRequiringAcceptance(dummyStateRequiringAcceptanceStateAndRef)
        )
    }

    // dummy state requiring all participants to sign

    fun createDummyStateRequiringAllParticipantsSignatures(
        owner: Party
    ) {
        runFlow(
            issuerNode,
            CreateDummyStateRequiringAllParticipantsSignatures(owner, acceptorParty)
        )
    }

    fun updateDummyStateRequiringAllParticipantsSignatures(
        node: TestStartedNode,
        owner: Party
    ) {
        val dummyStateRequiringAllParticipantsSignaturesStateAndRef = getStateAndRefs<DummyStateRequiringAllParticipantsSignatures>(node)[0]
        runFlow(
            node,
            UpdateDummyStateRequiringAllParticipantsSignatures(dummyStateRequiringAllParticipantsSignaturesStateAndRef, owner)
        )
    }

    fun deleteDummyStateRequiringAllParticipantsSignatures(
        node: TestStartedNode
    ) {
        val dummyStateRequiringAllParticipantsSignaturesStateAndRef = getStateAndRefs<DummyStateRequiringAllParticipantsSignatures>(node)[0]
        runFlow(
            node,
            DeleteDummyStateRequiringAllParticipantsSignatures(dummyStateRequiringAllParticipantsSignaturesStateAndRef)
        )
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
        runFlow(
            issuerNode,
            IssueTokens(holder, tokenAmount)
        )
    }

    fun transferTokens(
        node: TestStartedNode,
        newHolder: Party,
        tokenAmount: Long
    ) {
        runFlow(
            node,
            TransferTokens(issuerParty, newHolder, tokenAmount)
        )
    }

    fun redeemTokens(
        node: TestStartedNode,
        tokens: List<StateAndRef<FungibleToken>>
    ) {
        runFlow(
            node,
            RedeemTokens(tokens, issuerParty)
        )
    }

    fun getTokens(
        node: TestStartedNode,
        encumbered: Boolean = false
    ): List<StateAndRef<FungibleToken>> {
        val tokens = runFlow(
            node,
            ListTokensFlow()
        )
        return filterStates(tokens, encumbered)
    }

    // DummyStateWithInvalidEqualsMethodContract

    fun createDummyStateWithInvalidEqualsMethod(
        owner: Party,
        quantity: Int
    ) {
        runFlow(
            issuerNode,
            CreateDummyStateWithInvalidEqualsMethod(owner, quantity)
        )
    }

    fun updateDummyStateWithInvalidEqualsMethod(
        node: TestStartedNode,
        owner: Party
    ) {
        val dummyStateWithInvalidEqualsMethodStateAndRef = getStateAndRefs<DummyStateWithInvalidEqualsMethod>(node)[0]
        runFlow(
            node,
            UpdateDummyStateWithInvalidEqualsMethod(dummyStateWithInvalidEqualsMethodStateAndRef, owner)
        )
    }

    fun deleteDummyStateWithInvalidEqualsMethod(
        node: TestStartedNode,
        dummyStateWithInvalidEqualsMethodStateAndRef: StateAndRef<DummyStateWithInvalidEqualsMethod> = getStateAndRefs<DummyStateWithInvalidEqualsMethod>(node)[0]
    ) {
        runFlow(
            node,
            DeleteDummyStateWithInvalidEqualsMethod(dummyStateWithInvalidEqualsMethodStateAndRef)
        )
    }

    // common

    inline fun <reified T : ContractState> getStateAndRefs(
        node: TestStartedNode,
        encumbered: Boolean? = null,
        accountUUID: UUID? = null
    ): List<StateAndRef<T>> {
        val states = if(accountUUID == null)
            node.services.vaultService.queryBy<T>().states
        else
            node.services.vaultService.queryBy<T>(
                criteria = QueryCriteria.VaultQueryCriteria().withExternalIds(listOf(accountUUID))
            ).states
        if(encumbered == null)
            return states
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
        stateRefsToReIssue: List<StateRef>,
        command: CommandData,
        issuer: AbstractParty,
        commandSigners: List<AbstractParty> = listOf(issuer),
        requester: AbstractParty? = null
    ) where T: ContractState {
        runFlow(
            node,
            RequestReIssuance<T>(issuer, stateRefsToReIssue, command, commandSigners, requester)
        )
    }

    fun <T> createReIssuanceRequestAndShareRequiredTransactions(
        node: TestStartedNode,
        statesToReIssue: List<StateAndRef<T>>,
        command: CommandData,
        issuer: AbstractParty,
        commandSigners: List<AbstractParty> = listOf(),
        requester: AbstractParty? = null
    ) where T: ContractState {
        runFlow(
            node,
            RequestReIssuanceAndShareRequiredTransactions<T>(issuer, statesToReIssue.map { it.ref }, command,
                commandSigners, requester)
        )
    }

    inline fun <reified T : ContractState> unlockReIssuedState(
        node: TestStartedNode,
        attachmentSecureHashes: List<SecureHash>,
        command: CommandData,
        commandSigners: List<AbstractParty>? = null,
        reIssuedStateAndRefs: List<StateAndRef<T>> = getStateAndRefs<T>(node, true),
        lockStateAndRef: StateAndRef<ReIssuanceLock<T>> = getStateAndRefs<ReIssuanceLock<T>>(node, encumbered = true)[0]
    ) {
        val signers: List<AbstractParty> = commandSigners ?: listOf(lockStateAndRef.state.data.requester)
        runFlow(
            node,
            UnlockReIssuedStates(reIssuedStateAndRefs, lockStateAndRef, attachmentSecureHashes, command, signers)
        )
    }

    fun <T> reIssueRequestedStates(
        node: TestStartedNode,
        reIssuanceRequest: StateAndRef<ReIssuanceRequest>,
        issuerIsRequiredExitCommandSigner: Boolean
        ) where T: ContractState {
        runFlow(
            node,
            ReIssueStates<T>(reIssuanceRequest, issuerIsRequiredExitCommandSigner)
        )
    }

    fun <T> rejectReIssuanceRequested(
        node: TestStartedNode,
        reIssuanceRequest: StateAndRef<ReIssuanceRequest>
    ) where T: ContractState {
        runFlow(
            node,
            RejectReIssuanceRequest<T>(reIssuanceRequest)
        )
    }

    fun uploadDeletedStateAttachment(
        node: TestStartedNode,
        deleteStateTransactionId: SecureHash = getLedgerTransactions(node).last().id
    ): SecureHash {
        val party = node.info.singleIdentity()

        mockNet.runNetwork()
        val transactionByteArray = runFlow(
            node,
            GenerateTransactionByteArray(deleteStateTransactionId)
        )

        return node.services.attachments.importAttachment(transactionByteArray.inputStream(), party.toString(), null)
    }

    fun <T> deleteReIssuedStatesAndLock(
        node: TestStartedNode,
        reIssuanceLock: StateAndRef<ReIssuanceLock<T>>,
        reIssuedStates: List<StateAndRef<T>>,
        command: CommandData,
        commandSigners: List<AbstractParty>? = null
        ) where T: ContractState {
        val signers: List<AbstractParty> = commandSigners ?: listOf(reIssuanceLock.state.data.requester,
            reIssuanceLock.state.data.issuer)
        runFlow(
            node,
            DeleteReIssuedStatesAndLock<T>(reIssuanceLock, reIssuedStates, command, signers)
        )
    }


    fun getSignedTransactions(
        node: TestStartedNode
    ): List<SignedTransaction> {
        return node.services.validatedTransactions.track().snapshot
    }

    fun getLedgerTransactions(
        node: TestStartedNode
    ): List<LedgerTransaction> {
        return getSignedTransactions(node).map {
            it.toLedgerTransaction(node.services)
        }
    }

    fun getTransactionBackChain(
        node: TestStartedNode,
        txId: SecureHash
    ): Set<SecureHash> {
        val ledgerGraphService = node.services.cordaService(LedgerGraphService::class.java)
        return ledgerGraphService.getBackchain(setOf(txId))
    }

    fun <T> runFlow(
        node: TestStartedNode,
        flowLogic: FlowLogic<T>
    ): T {
        val flowFuture = node.services.startFlow(flowLogic).resultFuture
        mockNet.runNetwork()
        return flowFuture.getOrThrow()
    }

    @InitiatingFlow
    class FlowWrapper<T>(
        private val flow: FlowLogic<T>,
        private val throwError: Boolean
    ) : FlowLogic<T>() {

        @Suspendable
        override fun call(): T {
            if (throwError) {
                error("ERROR")
            }
            return flow.call()

        }
    }
}
