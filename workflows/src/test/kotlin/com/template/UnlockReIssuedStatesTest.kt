package com.template

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import com.r3.corda.lib.tokens.contracts.commands.IssueTokenCommand
import com.r3.corda.lib.tokens.contracts.commands.MoveTokenCommand
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.template.contracts.example.SimpleDummyStateContract
import com.template.contracts.example.DummyStateRequiringAcceptanceContract
import com.template.contracts.example.DummyStateRequiringAllParticipantsSignaturesContract
import com.template.states.ReIssuanceRequest
import com.template.states.example.SimpleDummyState
import com.template.states.example.DummyStateRequiringAcceptance
import com.template.states.example.DummyStateRequiringAllParticipantsSignatures
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.crypto.SecureHash
import net.corda.core.node.services.queryBy
import net.corda.core.serialization.serialize
import net.corda.core.transactions.TransactionBuilder
import net.corda.testing.core.singleIdentity
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class UnlockReIssuedStatesTest: AbstractFlowTest() {

    @Test
    fun `Re-issued SimpleDummyState is unencumbered after the original state is deleted`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)
        updateSimpleDummyState(aliceNode, bobParty)
        updateSimpleDummyState(bobNode, charlieParty)
        updateSimpleDummyState(charlieNode, aliceParty)
        updateSimpleDummyState(aliceNode, bobParty)
        updateSimpleDummyState(bobNode, charlieParty)
        updateSimpleDummyState(charlieNode, aliceParty)

        val transactionsBeforeReIssuance = getLedgerTransactions(aliceNode)
        assertThat(transactionsBeforeReIssuance.size, `is`(7))

        val simpleDummyState = getStateAndRefs<SimpleDummyState>(aliceNode)[0]
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(simpleDummyState),
            SimpleDummyStateContract.Commands.Create(),
            issuerParty
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]

        reIssueRequestedStates<SimpleDummyState>(issuerNode, reIssuanceRequest)

        deleteSimpleDummyState(aliceNode)

        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode)

        unlockReIssuedState<SimpleDummyState>(
            aliceNode, attachmentSecureHash,
            SimpleDummyStateContract.Commands.Update()
        )

        val encumberedStates = getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = true)
        val unencumberedStates = getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = false)
        assertThat(encumberedStates, hasSize(`is`(0)))
        assertThat(unencumberedStates, hasSize(`is`(1)))
        assertThat(unencumberedStates[0].state.data, `is`(simpleDummyState.state.data))

        updateSimpleDummyState(aliceNode, debbieParty)
        val transactionsAfterReIssuance = getLedgerTransactions(debbieNode)
        assertThat(transactionsAfterReIssuance.size, `is`(4))

        val backChainTransactionsIds = getTransactionBackChain(debbieNode, transactionsAfterReIssuance.last().id)
        assertThat(backChainTransactionsIds, `is`(transactionsAfterReIssuance.map { it.id }.toSet()))
    }

    @Test
    fun `Re-issued DummyStateRequiringAcceptance is unencumbered after the original state is deleted`() {
        initialiseParties()
        createDummyStateRequiringAcceptance(aliceParty)
        updateDummyStateRequiringAcceptance(aliceNode, bobParty)
        updateDummyStateRequiringAcceptance(bobNode, charlieParty)
        updateDummyStateRequiringAcceptance(charlieNode, aliceParty)
        updateDummyStateRequiringAcceptance(aliceNode, bobParty)
        updateDummyStateRequiringAcceptance(bobNode, charlieParty)
        updateDummyStateRequiringAcceptance(charlieNode, aliceParty)

        val transactionsBeforeReIssuance = getLedgerTransactions(aliceNode)
        assertThat(transactionsBeforeReIssuance.size, `is`(7))

        val dummyStateRequiringAcceptance = getStateAndRefs<DummyStateRequiringAcceptance>(aliceNode)[0]
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(dummyStateRequiringAcceptance),
            DummyStateRequiringAcceptanceContract.Commands.Create(),
            issuerParty,
            listOf(issuerParty, acceptorParty)
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]

        reIssueRequestedStates<DummyStateRequiringAcceptance>(issuerNode, reIssuanceRequest)

        deleteDummyStateRequiringAcceptance(aliceNode)

        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode)

        unlockReIssuedState<DummyStateRequiringAcceptance>(
            aliceNode, attachmentSecureHash,
            DummyStateRequiringAcceptanceContract.Commands.Update(),
            listOf(aliceParty, issuerParty, acceptorParty)
        )

        val encumberedStates = getStateAndRefs<DummyStateRequiringAcceptance>(aliceNode, encumbered = true)
        val unencumberedStates = getStateAndRefs<DummyStateRequiringAcceptance>(aliceNode, encumbered = false)
        assertThat(encumberedStates, hasSize(`is`(0)))
        assertThat(unencumberedStates, hasSize(`is`(1)))
        assertThat(unencumberedStates[0].state.data, `is`(dummyStateRequiringAcceptance.state.data))

        updateDummyStateRequiringAcceptance(aliceNode, debbieParty)
        val transactionsAfterReIssuance = getLedgerTransactions(debbieNode)
        assertThat(transactionsAfterReIssuance.size, `is`(4))

        val backChainTransactionsIds = getTransactionBackChain(debbieNode, transactionsAfterReIssuance.last().id)
        assertThat(backChainTransactionsIds, `is`(transactionsAfterReIssuance.map { it.id }.toSet()))
    }

    @Test
    fun `DummyStateRequiringAllParticipantsSignatures is re-issued`() {
        initialiseParties()
        createDummyStateRequiringAllParticipantsSignatures(aliceParty)
        updateDummyStateRequiringAllParticipantsSignatures(aliceNode, bobParty)
        updateDummyStateRequiringAllParticipantsSignatures(bobNode, charlieParty)
        updateDummyStateRequiringAllParticipantsSignatures(charlieNode, aliceParty)
        updateDummyStateRequiringAllParticipantsSignatures(aliceNode, bobParty)
        updateDummyStateRequiringAllParticipantsSignatures(bobNode, charlieParty)
        updateDummyStateRequiringAllParticipantsSignatures(charlieNode, aliceParty)

        val transactionsBeforeReIssuance = getLedgerTransactions(aliceNode)
        assertThat(transactionsBeforeReIssuance.size, `is`(7))

        val dummyStateRequiringAllParticipantsSignatures = getStateAndRefs<DummyStateRequiringAllParticipantsSignatures>(aliceNode)[0]

        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(dummyStateRequiringAllParticipantsSignatures),
            DummyStateRequiringAllParticipantsSignaturesContract.Commands.Create(),
            issuerParty,
            listOf(aliceParty, issuerParty, acceptorParty)
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]

        reIssueRequestedStates<DummyStateRequiringAllParticipantsSignatures>(issuerNode, reIssuanceRequest)

        deleteDummyStateRequiringAllParticipantsSignatures(aliceNode)

        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode)

        unlockReIssuedState<DummyStateRequiringAllParticipantsSignatures>(
            aliceNode,
            attachmentSecureHash,
            DummyStateRequiringAllParticipantsSignaturesContract.Commands.Update(),
            listOf(aliceParty, issuerParty, acceptorParty)
        )

        val encumberedStates = getStateAndRefs<DummyStateRequiringAllParticipantsSignatures>(aliceNode, encumbered = true)
        val unencumberedStates = getStateAndRefs<DummyStateRequiringAllParticipantsSignatures>(aliceNode, encumbered = false)
        assertThat(encumberedStates, hasSize(`is`(0)))
        assertThat(unencumberedStates, hasSize(`is`(1)))
        assertThat(unencumberedStates[0].state.data, `is`(dummyStateRequiringAllParticipantsSignatures.state.data))

        updateDummyStateRequiringAllParticipantsSignatures(aliceNode, debbieParty)
        val transactionsAfterReIssuance = getLedgerTransactions(debbieNode)
        assertThat(transactionsAfterReIssuance.size, `is`(4))

        val backChainTransactionsIds = getTransactionBackChain(debbieNode, transactionsAfterReIssuance.last().id)
        assertThat(backChainTransactionsIds, `is`(transactionsAfterReIssuance.map { it.id }.toSet()))
    }

    @Test
    fun `Re-issued token is unencumbered after the original state is deleted`() {
        initialiseParties()
        issueTokens(aliceParty, 50)

        transferTokens(aliceNode, bobParty, 50)
        transferTokens(bobNode, charlieParty, 50)
        transferTokens(charlieNode, aliceParty, 50)
        transferTokens(aliceNode, bobParty, 50)
        transferTokens(bobNode, charlieParty, 50)
        transferTokens(charlieNode, aliceParty, 50)

        val transactionsBeforeReIssuance = getLedgerTransactions(aliceNode)
        assertThat(transactionsBeforeReIssuance.size, `is`(7))

        val tokens = getTokens(aliceNode)
        val tokenIndices = tokens.indices.toList()

        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            tokens,
            IssueTokenCommand(issuedTokenType, tokenIndices),
            issuerParty
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]

        reIssueRequestedStates<FungibleToken>(issuerNode, reIssuanceRequest)

        redeemTokens(aliceNode, tokens)

        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode)

        unlockReIssuedState<FungibleToken>(
            aliceNode,
            attachmentSecureHash,
            MoveTokenCommand(issuedTokenType, tokenIndices, tokenIndices)
        )

        val encumberedStates = getStateAndRefs<FungibleToken>(aliceNode, encumbered = true)
        val unencumberedStates = getStateAndRefs<FungibleToken>(aliceNode, encumbered = false)
        assertThat(encumberedStates, hasSize(`is`(0)))
        assertThat(unencumberedStates, hasSize(`is`(1)))
        assertThat(unencumberedStates[0].state.data, `is`(tokens[0].state.data))

        transferTokens(aliceNode, debbieParty, 50)
        val transactionsAfterReIssuance = getLedgerTransactions(debbieNode)
        assertThat(transactionsAfterReIssuance.size, `is`(4))

        val backChainTransactionsIds = getTransactionBackChain(debbieNode, transactionsAfterReIssuance.last().id)
        assertThat(backChainTransactionsIds, `is`(transactionsAfterReIssuance.map { it.id }.toSet()))
    }


    @Test
    fun `Re-issue just part of tokens`() {
        initialiseParties()
        issueTokens(aliceParty, 50)

        transferTokens(aliceNode, bobParty, 40)
        transferTokens(bobNode, charlieParty, 30)
        transferTokens(charlieNode, aliceParty, 30)
        transferTokens(aliceNode, bobParty, 30)
        transferTokens(bobNode, charlieParty, 30)
        transferTokens(charlieNode, aliceParty, 30)

        val transactionsBeforeReIssuance = getLedgerTransactions(aliceNode)
        assertThat(transactionsBeforeReIssuance.size, `is`(7))

        val tokensToReIssue = listOf(getTokens(aliceNode)[1]) // 30 tokens
        val indicesList = listOf(0)

        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            tokensToReIssue,
            IssueTokenCommand(issuedTokenType, indicesList),
            issuerParty
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]

        reIssueRequestedStates<FungibleToken>(issuerNode, reIssuanceRequest)

        redeemTokens(aliceNode, tokensToReIssue)

        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode)

        unlockReIssuedState<FungibleToken>(
            aliceNode,
            attachmentSecureHash,
            MoveTokenCommand(issuedTokenType, indicesList, indicesList)
        )

        val encumberedStates = getTokens(aliceNode, encumbered = true)
        val unencumberedStates = getTokens(aliceNode, encumbered = false)
        assertThat(encumberedStates, hasSize(`is`(0)))
        assertThat(unencumberedStates, hasSize(`is`(2)))
        assertThat(unencumberedStates.map { it.state.data }, hasItem(tokensToReIssue[0].state.data))

        transferTokens(aliceNode, debbieParty, 35)
        val transactionsAfterReIssuance = getLedgerTransactions(debbieNode)
        assertThat(transactionsAfterReIssuance.size, `is`(9))

        val backChainTransactionsIds = getTransactionBackChain(debbieNode, transactionsAfterReIssuance.last().id)
        assertThat(backChainTransactionsIds, `is`(transactionsAfterReIssuance.map { it.id }.toSet()))
    }


    @Test
    fun `Re-issued tokens are unencumbered after the original state is deleted`() {
        initialiseParties()
        issueTokens(aliceParty, 50)

        transferTokens(aliceNode, bobParty, 40)
        transferTokens(bobNode, charlieParty, 30)
        transferTokens(charlieNode, aliceParty, 30)
        transferTokens(aliceNode, bobParty, 30)
        transferTokens(bobNode, charlieParty, 30)
        transferTokens(charlieNode, aliceParty, 30)

        val transactionsBeforeReIssuance = getLedgerTransactions(aliceNode)
        assertThat(transactionsBeforeReIssuance.size, `is`(7))

        val tokensToReIssue = getTokens(aliceNode)
        val tokenIndices = tokensToReIssue.indices.toList()

        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            tokensToReIssue,
            IssueTokenCommand(issuedTokenType, tokenIndices),
            issuerParty
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]

        reIssueRequestedStates<FungibleToken>(issuerNode, reIssuanceRequest)

        redeemTokens(aliceNode, tokensToReIssue)

        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode)

        unlockReIssuedState<FungibleToken>(
            aliceNode,
            attachmentSecureHash,
            MoveTokenCommand(issuedTokenType, tokenIndices, tokenIndices)
        )

        val encumberedStates = getTokens(aliceNode, encumbered = true)
        val unencumberedStates = getTokens(aliceNode, encumbered = false)
        assertThat(encumberedStates, hasSize(`is`(0)))
        assertThat(unencumberedStates, hasSize(`is`(2)))
        assertThat(unencumberedStates.map { it.state.data }, `is`(tokensToReIssue.map { it.state.data }))

        transferTokens(aliceNode, debbieParty, 35)
        val transactionsAfterReIssuance = getLedgerTransactions(debbieNode)
        assertThat(transactionsAfterReIssuance.size, `is`(4))

        val backChainTransactionsIds = getTransactionBackChain(debbieNode, transactionsAfterReIssuance.last().id)
        assertThat(backChainTransactionsIds, `is`(transactionsAfterReIssuance.map { it.id }.toSet()))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Only requester can unlock re-issued state`() {
        initialiseParties()
        createDummyStateRequiringAcceptance(aliceParty)

        val dummyStateRequiringAcceptance = getStateAndRefs<DummyStateRequiringAcceptance>(aliceNode)[0]

        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(dummyStateRequiringAcceptance),
            DummyStateRequiringAcceptanceContract.Commands.Create(),
            issuerParty,
            listOf(issuerParty, acceptorParty)
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]

        reIssueRequestedStates<DummyStateRequiringAcceptance>(issuerNode, reIssuanceRequest)

        deleteDummyStateRequiringAcceptance(aliceNode)

        // issuer creates attachment and tries to unlock state
        val attachmentSecureHash = uploadDeletedStateAttachment(issuerNode)

        unlockReIssuedState<DummyStateRequiringAcceptance>(
            issuerNode,
            attachmentSecureHash,
            DummyStateRequiringAcceptanceContract.Commands.Update(),
            listOf(aliceParty, issuerParty, acceptorParty)
        )
    }

    @Test
    fun `SimpleDummyState re-issued - accounts on the same host`() {
        initialisePartiesForAccountsOnTheSameHost()
        createSimpleDummyStateForAccount(employeeNode, employeeAliceParty)
        updateSimpleDummyStateForAccount(employeeNode, employeeBobParty)
        updateSimpleDummyStateForAccount(employeeNode, employeeCharlieParty)
        updateSimpleDummyStateForAccount(employeeNode, employeeAliceParty)
        updateSimpleDummyStateForAccount(employeeNode, employeeBobParty)
        updateSimpleDummyStateForAccount(employeeNode, employeeCharlieParty)
        updateSimpleDummyStateForAccount(employeeNode, employeeAliceParty)

        val transactionsBeforeReIssuance = getLedgerTransactions(employeeNode)
        assertThat(transactionsBeforeReIssuance.size, `is`(12)) // including 5 create account transactions

        val simpleDummyState = getStateAndRefs<SimpleDummyState>(employeeNode,
            accountUUID = employeeAliceAccount.identifier.id)[0]
        createReIssuanceRequestAndShareRequiredTransactions(
            employeeNode,
            listOf(simpleDummyState),
            SimpleDummyStateContract.Commands.Create(),
            employeeIssuerParty,
            requester = employeeAliceParty
        )

        val reIssuanceRequest = employeeNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]

        reIssueRequestedStates<SimpleDummyState>(employeeNode, reIssuanceRequest)

        deleteSimpleDummyStateForAccount(employeeNode)

        val attachmentSecureHash = uploadDeletedStateAttachment(employeeNode)

        unlockReIssuedState<SimpleDummyState>(
            employeeNode,
            attachmentSecureHash,
            SimpleDummyStateContract.Commands.Update()
        )

        val encumberedStates = getStateAndRefs<SimpleDummyState>(employeeNode, encumbered = true,
            accountUUID = employeeAliceAccount.identifier.id)
        val unencumberedStates = getStateAndRefs<SimpleDummyState>(employeeNode, encumbered = false,
            accountUUID = employeeAliceAccount.identifier.id)
        assertThat(encumberedStates, hasSize(`is`(0)))
        assertThat(unencumberedStates, hasSize(`is`(1)))
        assertThat(unencumberedStates[0].state.data, `is`(simpleDummyState.state.data))

        updateSimpleDummyStateForAccount(employeeNode, employeeDebbieParty)

        val transactionsAfterReIssuance = getLedgerTransactions(employeeNode)
        assertThat(transactionsAfterReIssuance.size, `is`(17)) // transactions available to all accounts

        val backChainTransactionsIds = getTransactionBackChain(employeeNode, transactionsAfterReIssuance.last().id)
        assertThat(backChainTransactionsIds.size, `is`(4)) // transactions available the given account
        assertThat(transactionsAfterReIssuance.map { it.id }, hasItems(*backChainTransactionsIds.toTypedArray()))
    }

    @Test
    fun `SimpleDummyState re-issued - accounts on different hosts`() {
        initialisePartiesForAccountsOnDifferentHosts()
        createSimpleDummyStateForAccount(issuerNode, employeeAliceParty)
        updateSimpleDummyStateForAccount(aliceNode, employeeBobParty)
        updateSimpleDummyStateForAccount(bobNode, employeeCharlieParty)
        updateSimpleDummyStateForAccount(charlieNode, employeeAliceParty)
        updateSimpleDummyStateForAccount(aliceNode, employeeBobParty)
        updateSimpleDummyStateForAccount(bobNode, employeeCharlieParty)
        updateSimpleDummyStateForAccount(charlieNode, employeeAliceParty)

        val simpleDummyState = getStateAndRefs<SimpleDummyState>(aliceNode, accountUUID = employeeAliceAccount.identifier.id)[0]
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(simpleDummyState),
            SimpleDummyStateContract.Commands.Create(),
            employeeIssuerParty,
            requester = employeeAliceParty
        )

        val reIssuanceRequest = aliceNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<SimpleDummyState>(issuerNode, reIssuanceRequest)

        deleteSimpleDummyStateForAccount(aliceNode)
        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode)

        unlockReIssuedState<SimpleDummyState>(
            aliceNode,
            attachmentSecureHash,
            SimpleDummyStateContract.Commands.Update()
        )

        val encumberedStates = getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = true,
            accountUUID = employeeAliceAccount.identifier.id)
        val unencumberedStates = getStateAndRefs<SimpleDummyState>(aliceNode, encumbered = false,
            accountUUID = employeeAliceAccount.identifier.id)
        assertThat(encumberedStates, hasSize(`is`(0)))
        assertThat(unencumberedStates, hasSize(`is`(1)))
        assertThat(unencumberedStates[0].state.data, `is`(simpleDummyState.state.data))

        updateSimpleDummyStateForAccount(aliceNode, employeeDebbieParty)

        val transactionsAfterReIssuance = getLedgerTransactions(debbieNode)
        assertThat(transactionsAfterReIssuance.size, `is`(9)) // transactions available to all accounts

        val backChainTransactionsIds = getTransactionBackChain(debbieNode, transactionsAfterReIssuance.last().id)
        assertThat(backChainTransactionsIds.size, `is`(4)) // transactions available the given account
        assertThat(transactionsAfterReIssuance.map { it.id }, hasItems(*backChainTransactionsIds.toTypedArray()))
    }

    @Test(expected = TransactionVerificationException::class)
    fun `Attached transaction needs to be notarised`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)

        val simpleDummyState = getStateAndRefs<SimpleDummyState>(aliceNode)[0]
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(simpleDummyState),
            SimpleDummyStateContract.Commands.Create(),
            issuerParty
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<SimpleDummyState>(issuerNode, reIssuanceRequest)

        val originalStateAndRef = getStateAndRefs<SimpleDummyState>(aliceNode)[0]
        val transactionBuilder = TransactionBuilder(notary = notaryParty)
        transactionBuilder.addInputState(originalStateAndRef)
        transactionBuilder.addCommand(SimpleDummyStateContract.Commands.Delete(), listOf(aliceParty.owningKey))
        var signedTransaction = aliceNode.services.signInitialTransaction(transactionBuilder)

        val serializedLedgerTransactionBytes = signedTransaction.serialize().bytes

        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            val entry = ZipEntry("SignedTransaction")
            zos.putNextEntry(entry)
            zos.write(serializedLedgerTransactionBytes)
            zos.closeEntry()
        }
        baos.close()

        val attachmentSecureHash = aliceNode.services.attachments.importAttachment(
            baos.toByteArray().inputStream(), aliceNode.info.singleIdentity().toString(), null)

        unlockReIssuedState<SimpleDummyState>(
            aliceNode, attachmentSecureHash,
            SimpleDummyStateContract.Commands.Update()
        )
    }

    @Test(expected = TransactionVerificationException::class)
    fun `Re-issued SimpleDummyState cannot be unlocked if the original state is consumed`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)

        val simpleDummyState = getStateAndRefs<SimpleDummyState>(aliceNode)[0]
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(simpleDummyState),
            SimpleDummyStateContract.Commands.Create(),
            issuerParty
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<SimpleDummyState>(issuerNode, reIssuanceRequest)

        updateSimpleDummyState(aliceNode, bobParty)
        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode)

        unlockReIssuedState<SimpleDummyState>(
            aliceNode, attachmentSecureHash,
            SimpleDummyStateContract.Commands.Update()
        )
    }


    @Test(expected = TransactionVerificationException::class)
    fun `Re-issued DummyStateRequiringAcceptance cannot be unlocked if the original state is consumed`() {
        initialiseParties()
        createDummyStateRequiringAcceptance(aliceParty)

        val dummyStateRequiringAcceptance = getStateAndRefs<DummyStateRequiringAcceptance>(aliceNode)[0]
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(dummyStateRequiringAcceptance),
            DummyStateRequiringAcceptanceContract.Commands.Create(),
            issuerParty,
            listOf(issuerParty, acceptorParty)
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]

        reIssueRequestedStates<DummyStateRequiringAcceptance>(issuerNode, reIssuanceRequest)

        updateDummyStateRequiringAcceptance(aliceNode, bobParty)

        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode)

        unlockReIssuedState<DummyStateRequiringAcceptance>(
            aliceNode, attachmentSecureHash,
            DummyStateRequiringAcceptanceContract.Commands.Update(),
            listOf(aliceParty, issuerParty, acceptorParty)
        )
    }
}
