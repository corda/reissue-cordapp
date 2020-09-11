package com.template

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasElement
import com.natpryce.hamkrest.hasSize
import com.r3.corda.lib.tokens.contracts.commands.IssueTokenCommand
import com.r3.corda.lib.tokens.contracts.commands.MoveTokenCommand
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.template.contracts.example.SimpleStateContract
import com.template.contracts.example.StateNeedingAcceptanceContract
import com.template.contracts.example.StateNeedingAllParticipantsToSignContract
import com.template.states.ReIssuanceRequest
import com.template.states.example.SimpleState
import com.template.states.example.StateNeedingAcceptance
import com.template.states.example.StateNeedingAllParticipantsToSign
import net.corda.core.contracts.TransactionVerificationException
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
    fun `Re-issued SimpleState is unencumbered after the original state is deleted`() {
        initialiseParties()
        createSimpleState(aliceParty)
        updateSimpleState(aliceNode, bobParty)
        updateSimpleState(bobNode, charlieParty)
        updateSimpleState(charlieNode, aliceParty)
        updateSimpleState(aliceNode, bobParty)
        updateSimpleState(bobNode, charlieParty)
        updateSimpleState(charlieNode, aliceParty)

        val transactionsBeforeReIssuance = getLedgerTransactions(aliceNode)
        assertThat(transactionsBeforeReIssuance.size, equalTo(7))

        val simpleState = getStateAndRefs<SimpleState>(aliceNode)[0]
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(simpleState),
            SimpleStateContract.Commands.Create(),
            issuerParty
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]

        reIssueRequestedStates<SimpleState>(issuerNode, reIssuanceRequest)

        deleteSimpleState(aliceNode)

        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode)

        unlockReIssuedState<SimpleState>(
            aliceNode, attachmentSecureHash,
            SimpleStateContract.Commands.Update()
        )

        val encumberedStates = getStateAndRefs<SimpleState>(aliceNode, encumbered = true)
        val unencumberedStates = getStateAndRefs<SimpleState>(aliceNode, encumbered = false)
        assertThat(encumberedStates, hasSize(equalTo(0)))
        assertThat(unencumberedStates, hasSize(equalTo(1)))
        assertThat(unencumberedStates[0].state.data, equalTo(simpleState.state.data))

        updateSimpleState(aliceNode, debbieParty)
        val transactionsAfterReIssuance = getLedgerTransactions(debbieNode)
        assertThat(transactionsAfterReIssuance.size, equalTo(4))
    }

    @Test
    fun `Re-issued StateNeedingAcceptance is unencumbered after the original state is deleted`() {
        initialiseParties()
        createStateNeedingAcceptance(aliceParty)
        updateStateNeedingAcceptance(aliceNode, bobParty)
        updateStateNeedingAcceptance(bobNode, charlieParty)
        updateStateNeedingAcceptance(charlieNode, aliceParty)
        updateStateNeedingAcceptance(aliceNode, bobParty)
        updateStateNeedingAcceptance(bobNode, charlieParty)
        updateStateNeedingAcceptance(charlieNode, aliceParty)

        val transactionsBeforeReIssuance = getLedgerTransactions(aliceNode)
        assertThat(transactionsBeforeReIssuance.size, equalTo(7))

        val stateNeedingAcceptance = getStateAndRefs<StateNeedingAcceptance>(aliceNode)[0]
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(stateNeedingAcceptance),
            StateNeedingAcceptanceContract.Commands.Create(),
            issuerParty,
            listOf(issuerParty, acceptorParty)
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]

        reIssueRequestedStates<StateNeedingAcceptance>(issuerNode, reIssuanceRequest)

        deleteStateNeedingAcceptance(aliceNode)

        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode)

        unlockReIssuedState<StateNeedingAcceptance>(
            aliceNode, attachmentSecureHash,
            StateNeedingAcceptanceContract.Commands.Update(),
            listOf(aliceParty, issuerParty, acceptorParty)
        )

        val encumberedStates = getStateAndRefs<StateNeedingAcceptance>(aliceNode, encumbered = true)
        val unencumberedStates = getStateAndRefs<StateNeedingAcceptance>(aliceNode, encumbered = false)
        assertThat(encumberedStates, hasSize(equalTo(0)))
        assertThat(unencumberedStates, hasSize(equalTo(1)))
        assertThat(unencumberedStates[0].state.data, equalTo(stateNeedingAcceptance.state.data))

        updateStateNeedingAcceptance(aliceNode, debbieParty)
        val transactionsAfterReIssuance = getLedgerTransactions(debbieNode)
        assertThat(transactionsAfterReIssuance.size, equalTo(4))
    }

    @Test
    fun `StateNeedingAllParticipantsToSign is re-issued`() {
        initialiseParties()
        createStateNeedingAllParticipantsToSign(aliceParty)
        updateStateNeedingAllParticipantsToSign(aliceNode, bobParty)
        updateStateNeedingAllParticipantsToSign(bobNode, charlieParty)
        updateStateNeedingAllParticipantsToSign(charlieNode, aliceParty)
        updateStateNeedingAllParticipantsToSign(aliceNode, bobParty)
        updateStateNeedingAllParticipantsToSign(bobNode, charlieParty)
        updateStateNeedingAllParticipantsToSign(charlieNode, aliceParty)

        val transactionsBeforeReIssuance = getLedgerTransactions(aliceNode)
        assertThat(transactionsBeforeReIssuance.size, equalTo(7))

        val stateNeedingAllParticipantsToSign = getStateAndRefs<StateNeedingAllParticipantsToSign>(aliceNode)[0]

        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(stateNeedingAllParticipantsToSign),
            StateNeedingAllParticipantsToSignContract.Commands.Create(),
            issuerParty,
            listOf(aliceParty, issuerParty, acceptorParty)
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]

        reIssueRequestedStates<StateNeedingAllParticipantsToSign>(issuerNode, reIssuanceRequest)

        deleteStateNeedingAllParticipantsToSign(aliceNode)

        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode)

        unlockReIssuedState<StateNeedingAllParticipantsToSign>(
            aliceNode,
            attachmentSecureHash,
            StateNeedingAllParticipantsToSignContract.Commands.Update(),
            listOf(aliceParty, issuerParty, acceptorParty)
        )

        val encumberedStates = getStateAndRefs<StateNeedingAllParticipantsToSign>(aliceNode, encumbered = true)
        val unencumberedStates = getStateAndRefs<StateNeedingAllParticipantsToSign>(aliceNode, encumbered = false)
        assertThat(encumberedStates, hasSize(equalTo(0)))
        assertThat(unencumberedStates, hasSize(equalTo(1)))
        assertThat(unencumberedStates[0].state.data, equalTo(stateNeedingAllParticipantsToSign.state.data))

        updateStateNeedingAllParticipantsToSign(aliceNode, debbieParty)
        val transactionsAfterReIssuance = getLedgerTransactions(debbieNode)
        assertThat(transactionsAfterReIssuance.size, equalTo(4))
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
        assertThat(transactionsBeforeReIssuance.size, equalTo(7))

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
        assertThat(encumberedStates, hasSize(equalTo(0)))
        assertThat(unencumberedStates, hasSize(equalTo(1)))
        assertThat(unencumberedStates[0].state.data, equalTo(tokens[0].state.data))

        transferTokens(aliceNode, debbieParty, 50)
        val transactionsAfterReIssuance = getLedgerTransactions(debbieNode)
        assertThat(transactionsAfterReIssuance.size, equalTo(4))
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
        assertThat(transactionsBeforeReIssuance.size, equalTo(7))

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
        assertThat(encumberedStates, hasSize(equalTo(0)))
        assertThat(unencumberedStates, hasSize(equalTo(2)))
        assertThat(unencumberedStates.map { it.state.data }, hasElement(tokensToReIssue[0].state.data))

        transferTokens(aliceNode, debbieParty, 35)
        val transactionsAfterReIssuance = getLedgerTransactions(debbieNode)
        assertThat(transactionsAfterReIssuance.size, equalTo(9))
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
        assertThat(transactionsBeforeReIssuance.size, equalTo(7))

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
        assertThat(encumberedStates, hasSize(equalTo(0)))
        assertThat(unencumberedStates, hasSize(equalTo(2)))
        assertThat(unencumberedStates.map { it.state.data }, equalTo(tokensToReIssue.map { it.state.data }))

        transferTokens(aliceNode, debbieParty, 35)
        val transactionsAfterReIssuance = getLedgerTransactions(debbieNode)
        assertThat(transactionsAfterReIssuance.size, equalTo(4))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Only requester can unlock re-issued state`() {
        initialiseParties()
        createStateNeedingAcceptance(aliceParty)

        val stateNeedingAcceptance = getStateAndRefs<StateNeedingAcceptance>(aliceNode)[0]

        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(stateNeedingAcceptance),
            StateNeedingAcceptanceContract.Commands.Create(),
            issuerParty,
            listOf(issuerParty, acceptorParty)
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]

        reIssueRequestedStates<StateNeedingAcceptance>(issuerNode, reIssuanceRequest)

        deleteStateNeedingAcceptance(aliceNode)

        // issuer creates attachment and tries to unlock state
        val attachmentSecureHash = uploadDeletedStateAttachment(issuerNode)

        unlockReIssuedState<StateNeedingAcceptance>(
            issuerNode,
            attachmentSecureHash,
            StateNeedingAcceptanceContract.Commands.Update(),
            listOf(aliceParty, issuerParty, acceptorParty)
        )
    }

    @Test
    fun `SimpleState re-issued - accounts on the same host`() {
        initialisePartiesForAccountsOnTheSameHost()
        createSimpleStateForAccount(employeeNode, employeeAliceParty)
        updateSimpleStateForAccount(employeeNode, employeeBobParty)
        updateSimpleStateForAccount(employeeNode, employeeCharlieParty)
        updateSimpleStateForAccount(employeeNode, employeeAliceParty)
        updateSimpleStateForAccount(employeeNode, employeeBobParty)
        updateSimpleStateForAccount(employeeNode, employeeCharlieParty)
        updateSimpleStateForAccount(employeeNode, employeeAliceParty)

        val transactionsBeforeReIssuance = getLedgerTransactions(employeeNode)
        assertThat(transactionsBeforeReIssuance.size, equalTo(12)) // including 5 create account transactions

        val simpleState = getStateAndRefs<SimpleState>(employeeNode,
            accountUUID = employeeAliceAccount.identifier.id)[0]
        createReIssuanceRequestAndShareRequiredTransactions(
            employeeNode,
            listOf(simpleState),
            SimpleStateContract.Commands.Create(),
            employeeIssuerParty,
            requester = employeeAliceParty
        )

        val reIssuanceRequest = employeeNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]

        reIssueRequestedStates<SimpleState>(employeeNode, reIssuanceRequest)

        deleteSimpleStateForAccount(employeeNode)

        val attachmentSecureHash = uploadDeletedStateAttachment(employeeNode)

        unlockReIssuedState<SimpleState>(
            employeeNode,
            attachmentSecureHash,
            SimpleStateContract.Commands.Update()
        )

        val encumberedStates = getStateAndRefs<SimpleState>(employeeNode, encumbered = true,
            accountUUID = employeeAliceAccount.identifier.id)
        val unencumberedStates = getStateAndRefs<SimpleState>(employeeNode, encumbered = false,
            accountUUID = employeeAliceAccount.identifier.id)
        assertThat(encumberedStates, hasSize(equalTo(0)))
        assertThat(unencumberedStates, hasSize(equalTo(1)))
        assertThat(unencumberedStates[0].state.data, equalTo(simpleState.state.data))

        updateSimpleStateForAccount(employeeNode, employeeDebbieParty)
        // TODO: figure out how to get back-chain for a given account
    }

    @Test
    fun `SimpleState re-issued - accounts on different hosts`() {
        initialisePartiesForAccountsOnDifferentHosts()
        createSimpleStateForAccount(issuerNode, employeeAliceParty)
        updateSimpleStateForAccount(aliceNode, employeeBobParty)
        updateSimpleStateForAccount(bobNode, employeeCharlieParty)
        updateSimpleStateForAccount(charlieNode, employeeAliceParty)
        updateSimpleStateForAccount(aliceNode, employeeBobParty)
        updateSimpleStateForAccount(bobNode, employeeCharlieParty)
        updateSimpleStateForAccount(charlieNode, employeeAliceParty)

        val simpleState = getStateAndRefs<SimpleState>(aliceNode, accountUUID = employeeAliceAccount.identifier.id)[0]
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(simpleState),
            SimpleStateContract.Commands.Create(),
            employeeIssuerParty,
            requester = employeeAliceParty
        )

        val reIssuanceRequest = aliceNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<SimpleState>(issuerNode, reIssuanceRequest)

        deleteSimpleStateForAccount(aliceNode)
        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode)

        unlockReIssuedState<SimpleState>(
            aliceNode,
            attachmentSecureHash,
            SimpleStateContract.Commands.Update()
        )

        val encumberedStates = getStateAndRefs<SimpleState>(aliceNode, encumbered = true,
            accountUUID = employeeAliceAccount.identifier.id)
        val unencumberedStates = getStateAndRefs<SimpleState>(aliceNode, encumbered = false,
            accountUUID = employeeAliceAccount.identifier.id)
        assertThat(encumberedStates, hasSize(equalTo(0)))
        assertThat(unencumberedStates, hasSize(equalTo(1)))
        assertThat(unencumberedStates[0].state.data, equalTo(simpleState.state.data))

        updateSimpleStateForAccount(aliceNode, employeeDebbieParty)
        // TODO: figure out how to get back-chain for a given account
    }

    @Test(expected = TransactionVerificationException::class)
    fun `Attached transaction needs to be notarised`() {
        initialiseParties()
        createSimpleState(aliceParty)

        val simpleState = getStateAndRefs<SimpleState>(aliceNode)[0]
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(simpleState),
            SimpleStateContract.Commands.Create(),
            issuerParty
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<SimpleState>(issuerNode, reIssuanceRequest)

        val originalStateAndRef = getStateAndRefs<SimpleState>(aliceNode)[0]
        val transactionBuilder = TransactionBuilder(notary = notaryParty)
        transactionBuilder.addInputState(originalStateAndRef)
        transactionBuilder.addCommand(SimpleStateContract.Commands.Delete(), listOf(aliceParty.owningKey))
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

        val attachmentSecureHash = aliceNode.services.attachments.importAttachment(baos.toByteArray().inputStream(), aliceNode.info.singleIdentity().toString(), null)

        unlockReIssuedState<SimpleState>(
            aliceNode, attachmentSecureHash,
            SimpleStateContract.Commands.Update()
        )
    }

    @Test(expected = TransactionVerificationException::class)
    fun `Re-issued SimpleState cannot be unlocked if the original state is consumed`() {
        initialiseParties()
        createSimpleState(aliceParty)

        val simpleState = getStateAndRefs<SimpleState>(aliceNode)[0]
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(simpleState),
            SimpleStateContract.Commands.Create(),
            issuerParty
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<SimpleState>(issuerNode, reIssuanceRequest)

        updateSimpleState(aliceNode, bobParty)
        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode)

        unlockReIssuedState<SimpleState>(
            aliceNode, attachmentSecureHash,
            SimpleStateContract.Commands.Update()
        )
    }


    @Test(expected = TransactionVerificationException::class)
    fun `Re-issued StateNeedingAcceptance cannot be unlocked if the original state is consumed`() {
        initialiseParties()
        createStateNeedingAcceptance(aliceParty)

        val stateNeedingAcceptance = getStateAndRefs<StateNeedingAcceptance>(aliceNode)[0]
        createReIssuanceRequestAndShareRequiredTransactions(
            aliceNode,
            listOf(stateNeedingAcceptance),
            StateNeedingAcceptanceContract.Commands.Create(),
            issuerParty,
            listOf(issuerParty, acceptorParty)
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]

        reIssueRequestedStates<StateNeedingAcceptance>(issuerNode, reIssuanceRequest)

        updateStateNeedingAcceptance(aliceNode, bobParty)

        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode)

        unlockReIssuedState<StateNeedingAcceptance>(
            aliceNode, attachmentSecureHash,
            StateNeedingAcceptanceContract.Commands.Update(),
            listOf(aliceParty, issuerParty, acceptorParty)
        )
    }
}
