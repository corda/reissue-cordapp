package com.r3.corda.lib.reissuance.dummy_flows

import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.r3.corda.lib.accounts.workflows.internal.accountService
import com.r3.corda.lib.ci.workflows.SyncKeyMappingInitiator
import com.r3.corda.lib.reissuance.dummy_flows.dummy.ModifiedDeleteReissuedStatesAndLock
import com.r3.corda.lib.reissuance.dummy_flows.dummy.ModifiedUnlockReissuedStates
import com.r3.corda.lib.reissuance.dummy_flows.dummy.dummyStateRequiringAcceptance.CreateDummyStateRequiringAcceptance
import com.r3.corda.lib.reissuance.dummy_flows.dummy.dummyStateRequiringAcceptance.DeleteDummyStateRequiringAcceptance
import com.r3.corda.lib.reissuance.dummy_flows.dummy.dummyStateRequiringAcceptance.UpdateDummyStateRequiringAcceptance
import com.r3.corda.lib.reissuance.dummy_flows.dummy.dummyStateRequiringAllParticipantsSignatures.CreateDummyStateRequiringAllParticipantsSignatures
import com.r3.corda.lib.reissuance.dummy_flows.dummy.dummyStateRequiringAllParticipantsSignatures.DeleteDummyStateRequiringAllParticipantsSignatures
import com.r3.corda.lib.reissuance.dummy_flows.dummy.dummyStateRequiringAllParticipantsSignatures.UpdateDummyStateRequiringAllParticipantsSignatures
import com.r3.corda.lib.reissuance.dummy_flows.dummy.simpleDummyState.*
import com.r3.corda.lib.reissuance.dummy_flows.dummy.tokens.IssueTokens
import com.r3.corda.lib.reissuance.dummy_flows.dummy.tokens.ListTokensFlow
import com.r3.corda.lib.reissuance.dummy_flows.dummy.tokens.RedeemTokens
import com.r3.corda.lib.reissuance.dummy_flows.dummy.tokens.TransferTokens
import com.r3.corda.lib.reissuance.dummy_flows.dummyStateWithInvalidEqualsMethod.CreateDummyStateWithInvalidEqualsMethod
import com.r3.corda.lib.reissuance.dummy_flows.dummyStateWithInvalidEqualsMethod.DeleteDummyStateWithInvalidEqualsMethod
import com.r3.corda.lib.reissuance.dummy_flows.dummyStateWithInvalidEqualsMethod.UpdateDummyStateWithInvalidEqualsMethod
import com.r3.corda.lib.reissuance.dummy_states.DummyStateRequiringAcceptance
import com.r3.corda.lib.reissuance.dummy_states.DummyStateRequiringAllParticipantsSignatures
import com.r3.corda.lib.reissuance.dummy_states.DummyStateWithInvalidEqualsMethod
import com.r3.corda.lib.reissuance.dummy_states.SimpleDummyState
import com.r3.corda.lib.reissuance.flows.*
import com.r3.corda.lib.reissuance.states.ReissuanceLock
import com.r3.corda.lib.reissuance.states.ReissuanceRequest
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import net.corda.core.concurrent.CordaFuture
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.FlowLogic
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
                findCordapp("com.r3.corda.lib.reissuance.dummy_contracts"),
                findCordapp("com.r3.corda.lib.reissuance.flows"),
                findCordapp("com.r3.corda.lib.reissuance.dummy_flows")
            ),
            notarySpecs = listOf(MockNetworkNotarySpec(DUMMY_NOTARY_NAME, false)),
            initialNetworkParameters = testNetworkParameters(
                minimumPlatformVersion = 8 // 4.6
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
    ): SecureHash {
        return runFlow(
            issuerNode,
            CreateSimpleDummyState(owner)
        )
    }

    fun createSimpleDummyStateForAccount(
        node: TestStartedNode,
        owner: AbstractParty
    ): SecureHash {
        return runFlow(
            node,
            CreateSimpleDummyStateForAccount(employeeIssuerParty, owner)
        )
    }

    fun updateSimpleDummyState(
        node: TestStartedNode,
        owner: Party
    ): SecureHash {
        val simpleDummyStateStateAndRef = getStateAndRefs<SimpleDummyState>(node)[0]
        return runFlow(
            node,
            UpdateSimpleDummyState(simpleDummyStateStateAndRef, owner)
        )
    }

    fun updateSimpleDummyStateForAccount(
        node: TestStartedNode,
        owner: AbstractParty
    ): SecureHash {
        val simpleDummyStateStateAndRef = getStateAndRefs<SimpleDummyState>(node)[0]
        return runFlow(
            node,
            UpdateSimpleDummyStateForAccount(simpleDummyStateStateAndRef, owner)
        )
    }

    fun deleteSimpleDummyState(
        node: TestStartedNode
    ): SecureHash {
        val simpleDummyStateStateAndRef = getStateAndRefs<SimpleDummyState>(node)[0]
        return runFlow(
            node,
            DeleteSimpleDummyState(simpleDummyStateStateAndRef, issuerParty)
        )
    }

    fun deleteSimpleDummyStateForAccount(
        node: TestStartedNode
    ): SecureHash {
        val simpleDummyStateStateAndRef = getStateAndRefs<SimpleDummyState>(node)[0]
        return runFlow(
            node,
            DeleteSimpleDummyStateForAccount(simpleDummyStateStateAndRef)
        )
    }

    // dummy state requiring acceptance

    fun createDummyStateRequiringAcceptance(
        owner: Party
    ): SecureHash {
        return runFlow(
            issuerNode,
            CreateDummyStateRequiringAcceptance(owner, acceptorParty)
        )
    }

    fun updateDummyStateRequiringAcceptance(
        node: TestStartedNode,
        owner: Party
    ): SecureHash {
        val dummyStateRequiringAcceptanceStateAndRef = getStateAndRefs<DummyStateRequiringAcceptance>(node)[0]
        return runFlow(
            node,
            UpdateDummyStateRequiringAcceptance(dummyStateRequiringAcceptanceStateAndRef, owner)
        )
    }

    fun deleteDummyStateRequiringAcceptance(
        node: TestStartedNode
    ): SecureHash {
        val dummyStateRequiringAcceptanceStateAndRef = getStateAndRefs<DummyStateRequiringAcceptance>(node)[0]
        return runFlow(
            node,
            DeleteDummyStateRequiringAcceptance(dummyStateRequiringAcceptanceStateAndRef)
        )
    }

    // dummy state requiring all participants to sign

    fun createDummyStateRequiringAllParticipantsSignatures(
        owner: Party
    ): SecureHash {
        return runFlow(
            issuerNode,
            CreateDummyStateRequiringAllParticipantsSignatures(owner, acceptorParty)
        )
    }

    fun updateDummyStateRequiringAllParticipantsSignatures(
        node: TestStartedNode,
        owner: Party
    ): SecureHash {
        val dummyStateRequiringAllParticipantsSignaturesStateAndRef = getStateAndRefs<DummyStateRequiringAllParticipantsSignatures>(node)[0]
        return runFlow(
            node,
            UpdateDummyStateRequiringAllParticipantsSignatures(dummyStateRequiringAllParticipantsSignaturesStateAndRef, owner)
        )
    }

    fun deleteDummyStateRequiringAllParticipantsSignatures(
        node: TestStartedNode
    ): SecureHash {
        val dummyStateRequiringAllParticipantsSignaturesStateAndRef = getStateAndRefs<DummyStateRequiringAllParticipantsSignatures>(node)[0]
        return runFlow(
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
    ): SecureHash {
        return runFlow(
            issuerNode,
            IssueTokens(holder, tokenAmount)
        )
    }

    fun transferTokens(
        node: TestStartedNode,
        newHolder: Party,
        tokenAmount: Long
    ): SecureHash {
        return runFlow(
            node,
            TransferTokens(issuerParty, newHolder, tokenAmount)
        )
    }

    fun redeemTokens(
        node: TestStartedNode,
        tokens: List<StateAndRef<FungibleToken>>
    ): SecureHash {
        return runFlow(
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
    ): SecureHash {
        return runFlow(
            issuerNode,
            CreateDummyStateWithInvalidEqualsMethod(owner, quantity)
        )
    }

    fun updateDummyStateWithInvalidEqualsMethod(
        node: TestStartedNode,
        owner: Party
    ): SecureHash {
        val dummyStateWithInvalidEqualsMethodStateAndRef = getStateAndRefs<DummyStateWithInvalidEqualsMethod>(node)[0]
        return runFlow(
            node,
            UpdateDummyStateWithInvalidEqualsMethod(dummyStateWithInvalidEqualsMethodStateAndRef, owner)
        )
    }

    fun deleteDummyStateWithInvalidEqualsMethod(
        node: TestStartedNode,
        dummyStateWithInvalidEqualsMethodStateAndRef: StateAndRef<DummyStateWithInvalidEqualsMethod> = getStateAndRefs<DummyStateWithInvalidEqualsMethod>(node)[0]
    ): SecureHash {
        return runFlow(
            node,
            DeleteDummyStateWithInvalidEqualsMethod(dummyStateWithInvalidEqualsMethodStateAndRef)
        )
    }

    // common

    fun deleteSimpleDummyStateAndCreateDummyStateWithInvalidEqualsMethod(
        node: TestStartedNode
    ): SecureHash {
        val simpleDummyStateStateAndRef = getStateAndRefs<SimpleDummyState>(node)[0]
        return runFlow(
            node,
            DeleteSimpleDummyStateAndCreateDummyStateWithInvalidEqualsMethod(simpleDummyStateStateAndRef, issuerParty)
        )
    }

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
        return filterStates(states, encumbered)
    }

    inline fun <reified T : ContractState> filterStates(
        states: List<StateAndRef<T>>,
        encumbered: Boolean?
    ): List<StateAndRef<T>> {
        if(encumbered == null)
            return states
        if(encumbered)
            return states.filter { it.state.encumbrance != null }
        return states.filter { it.state.encumbrance == null }
    }

    fun <T> createReissuanceRequest(
        node: TestStartedNode,
        stateRefsToReissue: List<StateRef>,
        command: CommandData,
        issuer: AbstractParty,
        commandSigners: List<AbstractParty> = listOf(),
        requester: AbstractParty? = null
    ): SecureHash where T: ContractState {
        return runFlow(
            node,
            RequestReissuance<T>(issuer, stateRefsToReissue, command, commandSigners, requester)
        )
    }

    fun <T> createReissuanceRequestAndShareRequiredTransactions(
        node: TestStartedNode,
        statesToReissue: List<StateAndRef<T>>,
        command: CommandData,
        issuer: AbstractParty,
        extraCommandSigners: List<AbstractParty> = listOf(),
        requester: AbstractParty? = null
    ): SecureHash where T: ContractState {
        return runFlow(
            node,
            RequestReissuanceAndShareRequiredTransactions<T>(issuer, statesToReissue.map { it.ref }, command,
                extraCommandSigners, requester)
        )
    }

    fun shareTransaction(
        node: TestStartedNode,
        issuer: Party,
        transactionId: SecureHash
    ) {
        runFlow(
            node,
            ShareTransactionWithAnotherParty(issuer, transactionId)
        )
    }

    inline fun <reified T : ContractState> unlockReissuedState(
        node: TestStartedNode,
        attachmentSecureHashes: List<SecureHash>,
        command: CommandData,
        reissuedStateAndRefs: List<StateAndRef<T>>,
        lockStateAndRef: StateAndRef<ReissuanceLock<T>>,
        extraCommandSigners: List<AbstractParty> = listOf()
    ): SecureHash {
        return runFlow(
            node,
            UnlockReissuedStates(reissuedStateAndRefs, lockStateAndRef, attachmentSecureHashes, command,
                extraCommandSigners)
        )
    }

    inline fun <reified T : ContractState> unlockReissuedStateUsingModifiedFlow(
        node: TestStartedNode,
        signedTransactionByteArrays: List<ByteArray>,
        command: CommandData,
        reissuedStateAndRefs: List<StateAndRef<T>>,
        lockStateAndRef: StateAndRef<ReissuanceLock<T>>,
        extraCommandSigners: List<AbstractParty> = listOf()
    ): SecureHash {
        return runFlow(
            node,
            ModifiedUnlockReissuedStates(reissuedStateAndRefs, lockStateAndRef, signedTransactionByteArrays, command,
                extraCommandSigners)
        )
    }

    fun <T> reissueRequestedStates(
        node: TestStartedNode,
        reissuanceRequest: StateAndRef<ReissuanceRequest>,
        extraExitCommandSigners: List<AbstractParty>
    ) : SecureHash where T: ContractState {
        return runFlow(
            node,
            ReissueStates<T>(reissuanceRequest, extraExitCommandSigners)
        )
    }

    fun <T> rejectReissuanceRequested(
        node: TestStartedNode,
        reissuanceRequest: StateAndRef<ReissuanceRequest>
    ): SecureHash where T: ContractState {
        return runFlow(
            node,
            RejectReissuanceRequest<T>(reissuanceRequest)
        )
    }

    fun <T> deleteReissuedStatesAndLock(
        node: TestStartedNode,
        reissuanceLock: StateAndRef<ReissuanceLock<T>>,
        reissuedStates: List<StateAndRef<T>>,
        command: CommandData,
        commandSigners: List<AbstractParty>? = null
    ): SecureHash where T: ContractState {
        val signers: List<AbstractParty> = commandSigners ?: listOf(reissuanceLock.state.data.requester,
            reissuanceLock.state.data.issuer)
        return runFlow(
            node,
            DeleteReissuedStatesAndLock<T>(reissuanceLock, reissuedStates, command, signers)
        )
    }

    fun deleteReissuedStatesAndLockUsingModifiedFlow(
        node: TestStartedNode,
        reissuanceLock: StateAndRef<ReissuanceLock<SimpleDummyState>>,
        reissuedStates: List<StateAndRef<SimpleDummyState>>,
        command: CommandData,
        commandSigners: List<AbstractParty>? = null
    ): SecureHash {
        val signers: List<AbstractParty> = commandSigners ?: listOf(reissuanceLock.state.data.requester,
            reissuanceLock.state.data.issuer)
        return runFlow(
            node,
            ModifiedDeleteReissuedStatesAndLock(reissuanceLock, reissuedStates, command, signers)
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
        return runFlow(
            node,
            GetTransactionBackChain(txId)
        )
    }

    fun <T> runFlow(
        node: TestStartedNode,
        flowLogic: FlowLogic<T>
    ): T {
        val flowFuture = node.services.startFlow(flowLogic).resultFuture
        mockNet.runNetwork()
        return flowFuture.getOrThrow()
    }

}
