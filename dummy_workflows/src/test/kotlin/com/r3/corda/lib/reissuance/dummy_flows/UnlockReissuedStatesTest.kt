package com.r3.corda.lib.reissuance.dummy_flows

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import com.r3.corda.lib.reissuance.dummy_contracts.DummyStateRequiringAcceptanceContract
import com.r3.corda.lib.reissuance.dummy_contracts.DummyStateRequiringAllParticipantsSignaturesContract
import com.r3.corda.lib.reissuance.dummy_contracts.DummyStateWithInvalidEqualsMethodContract
import com.r3.corda.lib.reissuance.dummy_contracts.SimpleDummyStateContract
import com.r3.corda.lib.reissuance.states.ReissuanceRequest
import com.r3.corda.lib.reissuance.dummy_states.DummyStateRequiringAcceptance
import com.r3.corda.lib.reissuance.dummy_states.DummyStateRequiringAllParticipantsSignatures
import com.r3.corda.lib.reissuance.dummy_states.DummyStateWithInvalidEqualsMethod
import com.r3.corda.lib.reissuance.dummy_states.SimpleDummyState
import com.r3.corda.lib.reissuance.states.ReissuanceLock
import com.r3.corda.lib.reissuance.utils.convertSignedTransactionToByteArray
import com.r3.corda.lib.tokens.contracts.commands.IssueTokenCommand
import com.r3.corda.lib.tokens.contracts.commands.MoveTokenCommand
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import net.corda.core.contracts.*
import net.corda.core.crypto.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.transactions.*
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.internal.TestStartedNode
import org.junit.Test
import java.util.*

class UnlockReissuedStatesTest: AbstractFlowTest() {

    private fun createStateAndGenerateBackChain(
        createState: (Party) -> SecureHash,
        updateState: (TestStartedNode, Party) -> SecureHash
    ): List<SecureHash> {
        val transactionIds = mutableListOf<SecureHash>()
        transactionIds.add(createState(aliceParty))
        transactionIds.add(updateState(aliceNode, bobParty))
        transactionIds.add(updateState(bobNode, charlieParty))
        transactionIds.add(updateState(charlieNode, aliceParty))
        transactionIds.add(updateState(aliceNode, bobParty))
        transactionIds.add(updateState(bobNode, charlieParty))
        transactionIds.add(updateState(charlieNode, aliceParty))
        return transactionIds
    }

    private fun createStateAndGenerateBackChainForAccount(
        createState: (TestStartedNode, AbstractParty) -> SecureHash,
        updateState: (TestStartedNode, AbstractParty) -> SecureHash,
        onOneNode: Boolean
    ): List<SecureHash> {
        val (issuer, alice, bob, charlie) = if(onOneNode) (0..4).map { employeeNode } else
            listOf(issuerNode, aliceNode, bobNode, charlieNode)
        val transactionIds = mutableListOf<SecureHash>()
        transactionIds.add(createState(issuer, employeeAliceParty))
        transactionIds.add(updateState(alice, employeeBobParty))
        transactionIds.add(updateState(bob, employeeCharlieParty))
        transactionIds.add(updateState(charlie, employeeAliceParty))
        transactionIds.add(updateState(alice, employeeBobParty))
        transactionIds.add(updateState(bob, employeeCharlieParty))
        transactionIds.add(updateState(charlie, employeeAliceParty))
        return transactionIds
    }

    private fun createStateAndGenerateBackChainForTokens(
        createState: (Party, Long) -> SecureHash,
        updateState: (TestStartedNode, Party, Long) -> SecureHash,
        args: List<Long>
    ): List<SecureHash> {
        val transactionIds = mutableListOf<SecureHash>()
        transactionIds.add(createState(aliceParty, args[0]))
        transactionIds.add(updateState(aliceNode, bobParty, args[1]))
        transactionIds.add(updateState(bobNode, charlieParty, args[2]))
        transactionIds.add(updateState(charlieNode, aliceParty, args[3]))
        transactionIds.add(updateState(aliceNode, bobParty, args[4]))
        transactionIds.add(updateState(bobNode, charlieParty, args[5]))
        transactionIds.add(updateState(charlieNode, aliceParty, args[6]))
        return transactionIds
    }

    private inline fun <reified T> verifyUnlockedStates(
        stateAndRefs: List<StateAndRef<T>>,
        node: TestStartedNode = aliceNode,
        accountUUID: UUID? = null
    ) where T: ContractState {
        val encumberedStates = getStateAndRefs<T>(node, encumbered = true, accountUUID = accountUUID)
        val unencumberedStates = getStateAndRefs<T>(node, encumbered = false, accountUUID = accountUUID)
        assertThat(encumberedStates, empty())
        assertThat(unencumberedStates, hasSize(`is`(stateAndRefs.size)))
        assertThat(unencumberedStates.map { it.state.data },
            hasItems(*stateAndRefs.map { it.state.data }.toTypedArray()))
    }


    private fun verifyUnlockedTokens(
        stateAndRefs: List<StateAndRef<FungibleToken>>,
        extraTokens: Int = 0
    ) {
        val encumberedStates = getTokens(aliceNode, encumbered = true)
        val unencumberedStates = getTokens(aliceNode, encumbered = false)
        assertThat(encumberedStates, empty())
        assertThat(unencumberedStates, hasSize(`is`(stateAndRefs.size + extraTokens)))
        assertThat(unencumberedStates.map { it.state.data },
            hasItems(*stateAndRefs.map { it.state.data }.toTypedArray()))
    }

    private fun verifyTransactionBackChain(
        expectedTransactionIds: List<SecureHash>,
        node: TestStartedNode = aliceNode
    ) {
        val transactionBackChain = getTransactionBackChain(node, expectedTransactionIds.last())
        assertThat(transactionBackChain, hasSize(`is`(expectedTransactionIds.size)))
        assertThat(transactionBackChain, hasItems(*expectedTransactionIds.toTypedArray()))    
    }

    @Test
    fun `Re-issued SimpleDummyState is unencumbered after the original state is deleted`() {
        initialiseParties()
        val transactionIds = createStateAndGenerateBackChain(::createSimpleDummyState, ::updateSimpleDummyState)
        verifyTransactionBackChain(transactionIds)

        val statesToReissue = getStateAndRefs<SimpleDummyState>(aliceNode)
        val requestReissuanceTransactionId = createReissuanceRequestAndShareRequiredTransactions(aliceNode,
            statesToReissue, SimpleDummyStateContract.Commands.Create(), issuerParty)

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]
        val reissueStatesTransactionId = reissueRequestedStates<SimpleDummyState>(issuerNode, reissuanceRequest,
            listOf())

        val exitTransactionId = deleteSimpleDummyState(aliceNode)
        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode, exitTransactionId)
        val unlockReissuedStatesTransactionId = unlockReissuedState(
            aliceNode, listOf(attachmentSecureHash), SimpleDummyStateContract.Commands.Update(),
            getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = true),
            getStateAndRefs<ReissuanceLock<SimpleDummyState>>(aliceNode, encumbered = true)[0]
        )

        verifyUnlockedStates(statesToReissue)
        verifyTransactionBackChain(listOf(requestReissuanceTransactionId,
            reissueStatesTransactionId, unlockReissuedStatesTransactionId))
    }

    @Test
    fun `Re-issued DummyStateRequiringAcceptance is unencumbered after the original state is deleted`() {
        initialiseParties()
        val transactionIds = createStateAndGenerateBackChain(::createDummyStateRequiringAcceptance,
            ::updateDummyStateRequiringAcceptance)
        verifyTransactionBackChain(transactionIds)

        val statesToReissue = getStateAndRefs<DummyStateRequiringAcceptance>(aliceNode)
        val requestReissuanceTransactionId = createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            statesToReissue,
            DummyStateRequiringAcceptanceContract.Commands.Create(),
            issuerParty,
            listOf(issuerParty, acceptorParty)
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]
        val reissueStatesTransactionId = reissueRequestedStates<DummyStateRequiringAcceptance>(issuerNode,
            reissuanceRequest, listOf())

        val exitTransactionId = deleteDummyStateRequiringAcceptance(aliceNode)
        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode, exitTransactionId)
        val unlockReissuedStatesTransactionId = unlockReissuedState<DummyStateRequiringAcceptance>(aliceNode,
            listOf(attachmentSecureHash), DummyStateRequiringAcceptanceContract.Commands.Update(),
            getStateAndRefs<DummyStateRequiringAcceptance>(aliceNode, encumbered = true),
            getStateAndRefs<ReissuanceLock<DummyStateRequiringAcceptance>>(aliceNode, encumbered = true)[0],
            listOf(aliceParty, issuerParty, acceptorParty))

        verifyUnlockedStates(statesToReissue)
        verifyTransactionBackChain(listOf(requestReissuanceTransactionId,
            reissueStatesTransactionId, unlockReissuedStatesTransactionId))
    }

    @Test
    fun `DummyStateRequiringAllParticipantsSignatures is re-issued`() {
        initialiseParties()
        val transactionIds = createStateAndGenerateBackChain(::createDummyStateRequiringAllParticipantsSignatures,
            ::updateDummyStateRequiringAllParticipantsSignatures)
        verifyTransactionBackChain(transactionIds)

        val statesToReissue = getStateAndRefs<DummyStateRequiringAllParticipantsSignatures>(aliceNode)
        val requestReissuanceTransactionId = createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            statesToReissue,
            DummyStateRequiringAllParticipantsSignaturesContract.Commands.Create(),
            issuerParty,
            listOf(aliceParty, issuerParty, acceptorParty)
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]
        val reissueStatesTransactionId = reissueRequestedStates<DummyStateRequiringAllParticipantsSignatures>(
            issuerNode, reissuanceRequest, listOf(issuerParty))

        val exitTransactionId = deleteDummyStateRequiringAllParticipantsSignatures(aliceNode)
        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode, exitTransactionId)
        val unlockReissuedStatesTransactionId = unlockReissuedState<DummyStateRequiringAllParticipantsSignatures>(
            aliceNode, listOf(attachmentSecureHash),
            DummyStateRequiringAllParticipantsSignaturesContract.Commands.Update(),
            getStateAndRefs<DummyStateRequiringAllParticipantsSignatures>(aliceNode, encumbered = true),
            getStateAndRefs<ReissuanceLock<DummyStateRequiringAllParticipantsSignatures>>(aliceNode, encumbered = true)[0],
            listOf(aliceParty, issuerParty, acceptorParty)
        )

        verifyUnlockedStates(statesToReissue)
        verifyTransactionBackChain(listOf(requestReissuanceTransactionId,
            reissueStatesTransactionId, unlockReissuedStatesTransactionId))
    }

    @Test
    fun `Re-issued token is unencumbered after the original state is deleted`() {
        initialiseParties()
        val transactionIds = createStateAndGenerateBackChainForTokens(::issueTokens, ::transferTokens, (0..7).map { 50L })
        verifyTransactionBackChain(transactionIds)

        val statesToReissue = getTokens(aliceNode)
        val tokenIndices = statesToReissue.indices.toList()
        val requestReissuanceTransactionId = createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            statesToReissue,
            IssueTokenCommand(issuedTokenType, tokenIndices),
            issuerParty
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]
        val reissueStatesTransactionId = reissueRequestedStates<FungibleToken>(issuerNode, reissuanceRequest,
            listOf(issuerParty))

        val exitTransactionId = redeemTokens(aliceNode, statesToReissue)
        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode, exitTransactionId)
        val unlockReissuedStatesTransactionId = unlockReissuedState<FungibleToken>(aliceNode,
            listOf(attachmentSecureHash), MoveTokenCommand(issuedTokenType, tokenIndices, tokenIndices),
            getStateAndRefs<FungibleToken>(aliceNode, encumbered = true),
            getStateAndRefs<ReissuanceLock<FungibleToken>>(aliceNode, encumbered = true)[0]
        )

        verifyUnlockedTokens(statesToReissue)
        verifyTransactionBackChain(listOf(requestReissuanceTransactionId,
            reissueStatesTransactionId, unlockReissuedStatesTransactionId))
    }


    @Test
    fun `Re-issue just part of tokens`() {
        initialiseParties()
        val transactionIds = createStateAndGenerateBackChainForTokens(::issueTokens, ::transferTokens,
            listOf(50, 40, 30, 30, 30, 30, 30))
        verifyTransactionBackChain(transactionIds)

        val statesToReissue = listOf(getTokens(aliceNode)[1]) // 30 tokens
        val indicesList = listOf(0)
        val requestReissuanceTransactionId =  createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            statesToReissue,
            IssueTokenCommand(issuedTokenType, indicesList),
            issuerParty
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]
        val reissueStatesTransactionId = reissueRequestedStates<FungibleToken>(issuerNode, reissuanceRequest,
            listOf(issuerParty))

        val exitTransactionId = redeemTokens(aliceNode, statesToReissue)
        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode, exitTransactionId)
        val unlockReissuedStatesTransactionId = unlockReissuedState<FungibleToken>(aliceNode,
            listOf(attachmentSecureHash), MoveTokenCommand(issuedTokenType, indicesList, indicesList),
            getStateAndRefs<FungibleToken>(aliceNode, encumbered = true),
            getStateAndRefs<ReissuanceLock<FungibleToken>>(aliceNode, encumbered = true)[0]
        )

        verifyUnlockedTokens(statesToReissue, extraTokens=1)
        verifyTransactionBackChain(listOf(requestReissuanceTransactionId,
            reissueStatesTransactionId, unlockReissuedStatesTransactionId))
    }


    @Test
    fun `Re-issued tokens are unencumbered after the original state is deleted`() {
        initialiseParties()
        val transactionIds = createStateAndGenerateBackChainForTokens(::issueTokens, ::transferTokens,
            listOf(50, 40, 30, 30, 30, 30, 30))
        verifyTransactionBackChain(transactionIds)

        val statesToReissue = getTokens(aliceNode)
        val tokenIndices = statesToReissue.indices.toList()
        val requestReissuanceTransactionId = createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            statesToReissue,
            IssueTokenCommand(issuedTokenType, tokenIndices),
            issuerParty
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]
        val reissueStatesTransactionId = reissueRequestedStates<FungibleToken>(issuerNode, reissuanceRequest,
            listOf(issuerParty))

        val exitTransactionId = redeemTokens(aliceNode, statesToReissue)
        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode, exitTransactionId)
        val unlockReissuedStatesTransactionId = unlockReissuedState<FungibleToken>(aliceNode,
            listOf(attachmentSecureHash), MoveTokenCommand(issuedTokenType, tokenIndices, tokenIndices),
            getStateAndRefs<FungibleToken>(aliceNode, encumbered = true),
            getStateAndRefs<ReissuanceLock<FungibleToken>>(aliceNode, encumbered = true)[0]
        )

        verifyUnlockedTokens(statesToReissue)
        verifyTransactionBackChain(listOf(requestReissuanceTransactionId,
            reissueStatesTransactionId, unlockReissuedStatesTransactionId))
    }

    @Test
    fun `Many exit transactions`() {
        initialiseParties()
        val transactionIds = createStateAndGenerateBackChainForTokens(::issueTokens, ::transferTokens,
            listOf(50, 40, 30, 30, 30, 30, 30))
        verifyTransactionBackChain(transactionIds)

        val statesToReissue = getTokens(aliceNode)
        val indicesList = statesToReissue.indices.toList()
        val requestReissuanceTransactionId = createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            statesToReissue,
            IssueTokenCommand(issuedTokenType, indicesList),
            issuerParty
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]
        val reissueStatesTransactionId = reissueRequestedStates<FungibleToken>(issuerNode, reissuanceRequest,
            listOf(issuerParty))

        val exitTransactionIds = listOf(
            redeemTokens(aliceNode, listOf(statesToReissue[0])),
            redeemTokens(aliceNode, listOf(statesToReissue[1]))
        )
        val attachmentSecureHashes = exitTransactionIds.map {
            uploadDeletedStateAttachment(aliceNode, it)
        }
        val unlockReissuedStatesTransactionId = unlockReissuedState<FungibleToken>(aliceNode,
            attachmentSecureHashes, MoveTokenCommand(issuedTokenType, indicesList, indicesList),
            getStateAndRefs<FungibleToken>(aliceNode, encumbered = true),
            getStateAndRefs<ReissuanceLock<FungibleToken>>(aliceNode, encumbered = true)[0]
        )

        verifyUnlockedTokens(statesToReissue)
        verifyTransactionBackChain(listOf(requestReissuanceTransactionId,
            reissueStatesTransactionId, unlockReissuedStatesTransactionId))
    }

    @Test(expected = TransactionVerificationException::class)
    fun `DeleteSimpleDummyStateAndCreateDummyStateWithInvalidEqualsMethod can't be used to unlock re-issued SimpleDummyState `() {
        initialiseParties()
        createSimpleDummyState(aliceParty)

        val simpleDummyState = getStateAndRefs<SimpleDummyState>(aliceNode)[0]
        createReissuanceRequestAndShareRequiredTransactions(aliceNode,
            listOf(simpleDummyState), SimpleDummyStateContract.Commands.Create(), issuerParty)

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]
        reissueRequestedStates<SimpleDummyState>(issuerNode, reissuanceRequest,
            listOf())

        val exitTransactionId = deleteSimpleDummyStateAndCreateDummyStateWithInvalidEqualsMethod(aliceNode)
        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode, exitTransactionId)
        unlockReissuedState(
            aliceNode, listOf(attachmentSecureHash), SimpleDummyStateContract.Commands.Update(),
            getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = true),
            getStateAndRefs<ReissuanceLock<SimpleDummyState>>(aliceNode, encumbered = true)[0]
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Only requester can unlock re-issued state`() {
        initialiseParties()
        createDummyStateRequiringAcceptance(aliceParty)

        val dummyStateRequiringAcceptance = getStateAndRefs<DummyStateRequiringAcceptance>(aliceNode)[0]
        createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(dummyStateRequiringAcceptance),
            DummyStateRequiringAcceptanceContract.Commands.Create(),
            issuerParty,
            listOf(issuerParty, acceptorParty)
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]
        reissueRequestedStates<DummyStateRequiringAcceptance>(issuerNode, reissuanceRequest,
            listOf(issuerParty))

        val exitTransactionId = deleteDummyStateRequiringAcceptance(aliceNode)
        // issuer creates attachment and tries to unlock state
        val attachmentSecureHash = uploadDeletedStateAttachment(issuerNode, exitTransactionId)
        unlockReissuedState<DummyStateRequiringAcceptance>(
            issuerNode,
            listOf(attachmentSecureHash),
            DummyStateRequiringAcceptanceContract.Commands.Update(),
            getStateAndRefs<DummyStateRequiringAcceptance>(aliceNode, encumbered = true),
            getStateAndRefs<ReissuanceLock<DummyStateRequiringAcceptance>>(aliceNode, encumbered = true)[0],
            listOf(aliceParty, issuerParty, acceptorParty)
            )
    }

    @Test
    fun `SimpleDummyState re-issued - accounts on the same host`() {
        initialisePartiesForAccountsOnTheSameHost()
        val transactionIds = createStateAndGenerateBackChainForAccount(::createSimpleDummyStateForAccount,
            ::updateSimpleDummyStateForAccount, onOneNode = true)
        verifyTransactionBackChain(transactionIds, node = employeeNode)

        val transactionsBeforeReissuance = getLedgerTransactions(employeeNode)
        assertThat(transactionsBeforeReissuance.size, `is`(12)) // including 5 create account transactions

        val statesToReissue = getStateAndRefs<SimpleDummyState>(employeeNode,
            accountUUID = employeeAliceAccount.identifier.id)
        val requestReissuanceTransactionId = createReissuanceRequestAndShareRequiredTransactions(
            employeeNode,
            statesToReissue,
            SimpleDummyStateContract.Commands.Create(),
            employeeIssuerParty,
            requester = employeeAliceParty
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(employeeNode)[0]
        val reissueStatesTransactionId = reissueRequestedStates<SimpleDummyState>(employeeNode, reissuanceRequest,
            listOf())

        val exitTransactionId = deleteSimpleDummyStateForAccount(employeeNode)
        val attachmentSecureHash = uploadDeletedStateAttachment(employeeNode, exitTransactionId)
        val unlockReissuedStatesTransactionId = unlockReissuedState<SimpleDummyState>(
            employeeNode,
            listOf(attachmentSecureHash),
            SimpleDummyStateContract.Commands.Update(),
            getStateAndRefs<SimpleDummyState>(employeeNode, encumbered = true),
            getStateAndRefs<ReissuanceLock<SimpleDummyState>>(employeeNode, encumbered = true)[0]
        )

        verifyUnlockedStates(statesToReissue, node=employeeNode, accountUUID = employeeAliceAccount.identifier.id)
        verifyTransactionBackChain(listOf(requestReissuanceTransactionId,
            reissueStatesTransactionId, unlockReissuedStatesTransactionId), node = employeeNode)
    }

    @Test
    fun `SimpleDummyState re-issued - accounts on different hosts`() {
        initialisePartiesForAccountsOnDifferentHosts()
        val transactionIds = createStateAndGenerateBackChainForAccount(::createSimpleDummyStateForAccount,
            ::updateSimpleDummyStateForAccount, onOneNode = false)
        verifyTransactionBackChain(transactionIds)

        val statesToReissue = getStateAndRefs<SimpleDummyState>(aliceNode,
            accountUUID = employeeAliceAccount.identifier.id)
        val requestReissuanceTransactionId = createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            statesToReissue,
            SimpleDummyStateContract.Commands.Create(),
            employeeIssuerParty,
            requester = employeeAliceParty
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(aliceNode)[0]
        val reissueStatesTransactionId = reissueRequestedStates<SimpleDummyState>(issuerNode, reissuanceRequest,
            listOf())

        val exitTransactionId = deleteSimpleDummyStateForAccount(aliceNode)
        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode, exitTransactionId)
        val unlockReissuedStatesTransactionId = unlockReissuedState<SimpleDummyState>(
            aliceNode,
            listOf(attachmentSecureHash),
            SimpleDummyStateContract.Commands.Update(),
            getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = true),
            getStateAndRefs<ReissuanceLock<SimpleDummyState>>(aliceNode, encumbered = true)[0]
        )

        verifyUnlockedStates(statesToReissue, accountUUID = employeeAliceAccount.identifier.id)
        verifyTransactionBackChain(listOf(requestReissuanceTransactionId,
            reissueStatesTransactionId, unlockReissuedStatesTransactionId))
    }

    @Test(expected = TransactionVerificationException::class)
    fun `Attached transaction needs to be notarised`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)

        val simpleDummyState = getStateAndRefs<SimpleDummyState>(aliceNode)[0]
        createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(simpleDummyState),
            SimpleDummyStateContract.Commands.Create(),
            issuerParty
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]
        reissueRequestedStates<SimpleDummyState>(issuerNode, reissuanceRequest,
            listOf(issuerParty))

        val originalStateAndRef = getStateAndRefs<SimpleDummyState>(aliceNode)[0]
        val transactionBuilder = TransactionBuilder(notary = notaryParty)
        transactionBuilder.addInputState(originalStateAndRef)
        transactionBuilder.addCommand(SimpleDummyStateContract.Commands.Delete(), listOf(aliceParty.owningKey))
        var signedTransaction = aliceNode.services.signInitialTransaction(transactionBuilder)

        val transactionByteArray = convertSignedTransactionToByteArray(signedTransaction)
        val attachmentSecureHash = aliceNode.services.attachments.importAttachment(
            transactionByteArray.inputStream(), aliceNode.info.singleIdentity().toString(), null)

        unlockReissuedState<SimpleDummyState>(
            aliceNode, 
            listOf(attachmentSecureHash),
            SimpleDummyStateContract.Commands.Update(),
            getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = true),
            getStateAndRefs<ReissuanceLock<SimpleDummyState>>(aliceNode, encumbered = true)[0]
        )
    }

    @Test(expected = TransactionVerificationException::class)
    fun `Re-issued SimpleDummyState cannot be unlocked if the original state is consumed`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)

        val simpleDummyState = getStateAndRefs<SimpleDummyState>(aliceNode)[0]
        createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(simpleDummyState),
            SimpleDummyStateContract.Commands.Create(),
            issuerParty
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]
        reissueRequestedStates<SimpleDummyState>(issuerNode, reissuanceRequest,
            listOf(issuerParty))

        val updateTransactionId = updateSimpleDummyState(aliceNode, bobParty)
        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode, updateTransactionId)

        unlockReissuedState<SimpleDummyState>(
            aliceNode,
            listOf(attachmentSecureHash),
            SimpleDummyStateContract.Commands.Update(),
            getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = true),
            getStateAndRefs<ReissuanceLock<SimpleDummyState>>(aliceNode, encumbered = true)[0]
        )
    }


    @Test(expected = TransactionVerificationException::class)
    fun `Re-issued DummyStateRequiringAcceptance cannot be unlocked if the original state is consumed`() {
        initialiseParties()
        createDummyStateRequiringAcceptance(aliceParty)

        val dummyStateRequiringAcceptance = getStateAndRefs<DummyStateRequiringAcceptance>(aliceNode)[0]
        createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(dummyStateRequiringAcceptance),
            DummyStateRequiringAcceptanceContract.Commands.Create(),
            issuerParty,
            listOf(issuerParty, acceptorParty)
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]
        reissueRequestedStates<DummyStateRequiringAcceptance>(issuerNode, reissuanceRequest,
            listOf(issuerParty))

        val updateTransactionId = updateDummyStateRequiringAcceptance(aliceNode, bobParty)
        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode, updateTransactionId)
        unlockReissuedState<DummyStateRequiringAcceptance>(
            aliceNode, 
            listOf(attachmentSecureHash),
            DummyStateRequiringAcceptanceContract.Commands.Update(),
            getStateAndRefs<DummyStateRequiringAcceptance>(aliceNode, encumbered = true),
            getStateAndRefs<ReissuanceLock<DummyStateRequiringAcceptance>>(aliceNode, encumbered = true)[0],
            listOf(aliceParty, issuerParty, acceptorParty)
        )
    }

    @Test(expected = TransactionVerificationException::class)
    fun `Invalid equals method doesn't allow cheating by attaching another state exit as reference is checked`() {
        initialiseParties()
        createDummyStateWithInvalidEqualsMethod(aliceParty, 10)

        val dummyStateWithInvalidEqualsMethod = getStateAndRefs<DummyStateWithInvalidEqualsMethod>(aliceNode)[0]
        createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(dummyStateWithInvalidEqualsMethod),
            DummyStateWithInvalidEqualsMethodContract.Commands.Create(),
            issuerParty
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]
        reissueRequestedStates<DummyStateWithInvalidEqualsMethod>(issuerNode, reissuanceRequest,
            listOf(issuerParty))


        createDummyStateWithInvalidEqualsMethod(aliceParty, 5)
        val stateAndRefToDelete = getStateAndRefs<DummyStateWithInvalidEqualsMethod>(aliceNode)
        val exitTransactionId = deleteDummyStateWithInvalidEqualsMethod(aliceNode, stateAndRefToDelete.last())
        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode, exitTransactionId)

        unlockReissuedState<DummyStateWithInvalidEqualsMethod>(
            aliceNode,
            listOf(attachmentSecureHash),
            DummyStateWithInvalidEqualsMethodContract.Commands.Update(),
            getStateAndRefs<DummyStateWithInvalidEqualsMethod>(aliceNode, encumbered = true),
            getStateAndRefs<ReissuanceLock<DummyStateWithInvalidEqualsMethod>>(aliceNode, encumbered = true)[0]
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `State can't be re-issued again after the re-issued state has been unlocked`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)

        val simpleDummyState = getStateAndRefs<SimpleDummyState>(aliceNode)[0]
        createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(simpleDummyState),
            SimpleDummyStateContract.Commands.Create(),
            issuerParty
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]
        reissueRequestedStates<SimpleDummyState>(issuerNode, reissuanceRequest,
            listOf())

        val exitTransactionId = deleteSimpleDummyState(aliceNode)
        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode, exitTransactionId)

        unlockReissuedState<SimpleDummyState>(
            aliceNode,
            listOf(attachmentSecureHash),
            SimpleDummyStateContract.Commands.Update(),
            getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = true),
            getStateAndRefs<ReissuanceLock<SimpleDummyState>>(aliceNode, encumbered = true)[0]
        )

        createReissuanceRequest<SimpleDummyState>(
            aliceNode,
            listOf(simpleDummyState.ref),
            SimpleDummyStateContract.Commands.Create(),
            issuerParty
        )
        val reissuanceRequest2 = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]
        reissueRequestedStates<SimpleDummyState>(issuerNode, reissuanceRequest2,
            listOf(issuerParty))

    }

    @Test(expected = IllegalArgumentException::class)
    fun `Requester can't pretend to be a notary to forge exit transaction`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)

        val simpleDummyState = getStateAndRefs<SimpleDummyState>(aliceNode)[0]
        createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(simpleDummyState),
            SimpleDummyStateContract.Commands.Create(),
            issuerParty
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]

        reissueRequestedStates<SimpleDummyState>(issuerNode, reissuanceRequest,
            listOf(issuerParty))

        val transactionBuilder = TransactionBuilder(notary = aliceParty)
        transactionBuilder.addInputState(simpleDummyState)
        transactionBuilder.addCommand(DummyStateRequiringAcceptanceContract.Commands.Update(), listOf(aliceParty.owningKey))
        val signedTransaction = aliceNode.services.signInitialTransaction(transactionBuilder)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Requester can't forge exit transaction`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)

        val simpleDummyState = getStateAndRefs<SimpleDummyState>(aliceNode)[0]
        createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(simpleDummyState),
            SimpleDummyStateContract.Commands.Create(),
            issuerParty
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]

        reissueRequestedStates<SimpleDummyState>(issuerNode, reissuanceRequest,
            listOf(issuerParty))

        val transactionBuilder = TransactionBuilder(notary = aliceParty)
        transactionBuilder.addInputState(simpleDummyState)
        transactionBuilder.addCommand(DummyStateRequiringAcceptanceContract.Commands.Update(), listOf(aliceParty.owningKey))
        val signedTransaction = aliceNode.services.signInitialTransaction(transactionBuilder)
    }

    @Test(expected = TransactionVerificationException::class)
    fun `Requester can't forge signed transaction by creating another class derived from TraversableTransaction`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)
        deleteSimpleDummyState(aliceNode)
        val signedDeleteTransaction = getSignedTransactions(aliceNode).last()

        createSimpleDummyState(aliceParty)

        val simpleDummyState = getStateAndRefs<SimpleDummyState>(aliceNode)[0]
        createReissuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(simpleDummyState),
            SimpleDummyStateContract.Commands.Create(),
            issuerParty
        )

        val reissuanceRequest = getStateAndRefs<ReissuanceRequest>(issuerNode)[0]

        reissueRequestedStates<SimpleDummyState>(issuerNode, reissuanceRequest,
            listOf(issuerParty))

        val transactionBuilder = TransactionBuilder(notary = notaryParty)
        transactionBuilder.addInputState(simpleDummyState)
        transactionBuilder.addCommand(DummyStateRequiringAcceptanceContract.Commands.Update(), listOf(aliceParty.owningKey))
        val initiallySignedTransaction = aliceNode.services.signInitialTransaction(transactionBuilder)

        val wireTransaction = initiallySignedTransaction.coreTransaction as WireTransaction
        val testWireTransaction = TestWireTransaction(wireTransaction.componentGroups, wireTransaction.privacySalt, id=signedDeleteTransaction.id)

        val forgedSignedTransaction = SignedTransaction(testWireTransaction, signedDeleteTransaction.sigs)

        val signedTransactionByteArray = convertSignedTransactionToByteArray(forgedSignedTransaction)
        val attachmentSecureHash = aliceNode.services.attachments.importAttachment(signedTransactionByteArray.inputStream(), aliceParty.toString(), null)

        unlockReissuedState<SimpleDummyState>(
            aliceNode,
            listOf(attachmentSecureHash),
            SimpleDummyStateContract.Commands.Update(),
            getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = true),
            getStateAndRefs<ReissuanceLock<SimpleDummyState>>(aliceNode, encumbered = true)[0]
        )
    }

    class TestWireTransaction(componentGroups: List<ComponentGroup>,
                              val privacySalt: PrivacySalt = PrivacySalt(),
                              override val id: SecureHash
    ): TraversableTransaction(componentGroups)
}
