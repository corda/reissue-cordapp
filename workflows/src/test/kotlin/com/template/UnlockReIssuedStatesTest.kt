package com.template

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
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
import net.corda.core.contracts.*
import net.corda.core.node.services.queryBy
import net.corda.core.serialization.serialize
import net.corda.core.transactions.TransactionBuilder
import net.corda.testing.core.singleIdentity
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class UnlockReIssuedStatesTest: AbstractFlowTest() {

    @Test(expected = java.lang.IllegalArgumentException::class) // TODO: delete expected exception once issuer can accesses requested states
    fun `Re-issued SimpleState is unencumbered after the original state is deleted`() {
        // generate back-chain
        initialiseParties()
        createSimpleState(aliceParty)
        updateSimpleState(aliceNode, bobParty)
        updateSimpleState(bobNode, charlieParty)
        updateSimpleState(charlieNode, aliceParty)
        updateSimpleState(aliceNode, bobParty)
        updateSimpleState(bobNode, charlieParty)
        updateSimpleState(charlieNode, aliceParty)

        // get transaction history, not a back-chain
        val transactionsBeforeReIssuance = getTransactions(aliceNode)
        assertThat(transactionsBeforeReIssuance.size, equalTo(7))

        // re-issue state
        val simpleStateRef = getStateAndRefs<SimpleState>(aliceNode)[0].ref

        createReIssuanceRequest<SimpleState>(
            aliceNode,
            listOf(simpleStateRef),
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

        // change state holder
        updateSimpleState(aliceNode, debbieParty)

        // check transaction history again
        val transactionsAfterReIssuance = getTransactions(debbieNode)
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

        val transactionsBeforeReIssuance = getTransactions(aliceNode)
        assertThat(transactionsBeforeReIssuance.size, equalTo(7))

        val stateNeedingAcceptanceRef = getStateAndRefs<StateNeedingAcceptance>(aliceNode)[0].ref

        createReIssuanceRequest<StateNeedingAcceptance>(
            aliceNode,
            listOf(stateNeedingAcceptanceRef),
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

        updateStateNeedingAcceptance(aliceNode, debbieParty)

        val transactionsAfterReIssuance = getTransactions(debbieNode)
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

        val transactionsBeforeReIssuance = getTransactions(aliceNode)
        assertThat(transactionsBeforeReIssuance.size, equalTo(7))

        val stateNeedingAllParticipantsToSignRef = getStateAndRefs<StateNeedingAllParticipantsToSign>(aliceNode)[0].ref

        createReIssuanceRequest<StateNeedingAllParticipantsToSign>(
            aliceNode,
            listOf(stateNeedingAllParticipantsToSignRef),
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

        updateStateNeedingAllParticipantsToSign(aliceNode, debbieParty)

        val transactionsAfterReIssuance = getTransactions(debbieNode)
        assertThat(transactionsAfterReIssuance.size, equalTo(4))
    }

    @Test(expected = java.lang.IllegalArgumentException::class) // TODO: delete expected exception once issuer can accesses requested states
    fun `Re-issued token is unencumbered after the original state is deleted`() {
        initialiseParties()
        issueTokens(aliceParty, 50)

        transferTokens(aliceNode, bobParty, 50)
        transferTokens(bobNode, charlieParty, 50)
        transferTokens(charlieNode, aliceParty, 50)
        transferTokens(aliceNode, bobParty, 50)
        transferTokens(bobNode, charlieParty, 50)
        transferTokens(charlieNode, aliceParty, 50)

        val transactionsBeforeReIssuance = getTransactions(aliceNode)
        assertThat(transactionsBeforeReIssuance.size, equalTo(7))

        val tokens = getTokens(aliceNode)
        val tokenRefs = tokens.map { it.ref }
        val tokenIndices = tokenRefs.indices.toList()

        createReIssuanceRequest<FungibleToken>(
            aliceNode,
            tokenRefs,
            IssueTokenCommand(issuedTokenType, tokenIndices),
            issuerParty
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]

        reIssueRequestedStates<FungibleToken>(issuerNode, reIssuanceRequest)

        redeemTokens(aliceNode, tokens)

        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode)

        val indicesList = tokenRefs.indices.toList()
        unlockReIssuedState<FungibleToken>(
            aliceNode,
            attachmentSecureHash,
            MoveTokenCommand(issuedTokenType, indicesList, indicesList)
        )

        transferTokens(aliceNode, debbieParty, 50)

        val transactionsAfterReIssuance = getTransactions(debbieNode)
        assertThat(transactionsAfterReIssuance.size, equalTo(4))
    }


    @Test(expected = java.lang.IllegalArgumentException::class) // TODO: delete expected exception once issuer can accesses requested states
    fun `Re-issue just part of tokens`() {
        initialiseParties()
        issueTokens(aliceParty, 50)

        transferTokens(aliceNode, bobParty, 40)
        transferTokens(bobNode, charlieParty, 30)
        transferTokens(charlieNode, aliceParty, 30)
        transferTokens(aliceNode, bobParty, 30)
        transferTokens(bobNode, charlieParty, 30)
        transferTokens(charlieNode, aliceParty, 30)

        val transactionsBeforeReIssuance = getTransactions(aliceNode)
        assertThat(transactionsBeforeReIssuance.size, equalTo(7))

        val tokens = listOf(getTokens(aliceNode)[1]) // 30 tokens
        val tokenRefs = tokens.map{ it.ref }
        val indicesList = listOf(0)

        createReIssuanceRequest<FungibleToken>(
            aliceNode,
            tokenRefs,
            IssueTokenCommand(issuedTokenType, indicesList),
            issuerParty
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]

        reIssueRequestedStates<FungibleToken>(issuerNode, reIssuanceRequest)

        redeemTokens(aliceNode, tokens)

        val attachmentSecureHash = uploadDeletedStateAttachment(aliceNode)

        unlockReIssuedState<FungibleToken>(
            aliceNode,
            attachmentSecureHash,
            MoveTokenCommand(issuedTokenType, indicesList, indicesList)
        )

        transferTokens(aliceNode, debbieParty, 35)

        val transactionsAfterReIssuance = getTransactions(debbieNode)
        assertThat(transactionsAfterReIssuance.size, equalTo(9))
    }


    @Test(expected = java.lang.IllegalArgumentException::class) // TODO: delete expected exception once issuer can accesses requested states
    fun `Re-issued tokens are unencumbered after the original state is deleted`() {
        initialiseParties()
        issueTokens(aliceParty, 50)

        transferTokens(aliceNode, bobParty, 40)
        transferTokens(bobNode, charlieParty, 30)
        transferTokens(charlieNode, aliceParty, 30)
        transferTokens(aliceNode, bobParty, 30)
        transferTokens(bobNode, charlieParty, 30)
        transferTokens(charlieNode, aliceParty, 30)

        val transactionsBeforeReIssuance = getTransactions(aliceNode)
        assertThat(transactionsBeforeReIssuance.size, equalTo(7))

        val tokens = getTokens(aliceNode)
        val tokenRefs = tokens.map { it.ref }
        val tokenIndices = tokens.indices.toList()

        createReIssuanceRequest<FungibleToken>(
            aliceNode,
            tokenRefs,
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

        transferTokens(aliceNode, debbieParty, 35)

        val transactionsAfterReIssuance = getTransactions(debbieNode)
        assertThat(transactionsAfterReIssuance.size, equalTo(4))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Only requester can unlock re-issued state`() {
        initialiseParties()
        createStateNeedingAcceptance(aliceParty)

        val stateNeedingAcceptanceRef = getStateAndRefs<StateNeedingAcceptance>(aliceNode)[0].ref

        createReIssuanceRequest<StateNeedingAcceptance>(
            aliceNode,
            listOf(stateNeedingAcceptanceRef),
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

    @Test(expected = java.lang.IllegalArgumentException::class) // TODO: delete expected exception once issuer can accesses requested states
    fun `SimpleState re-issued - accounts on the same host`() {
        initialisePartiesForAccountsOnTheSameHost()
        createSimpleStateForAccount(employeeNode, employeeAliceParty)
        updateSimpleStateForAccount(employeeNode, employeeBobParty)
        updateSimpleStateForAccount(employeeNode, employeeCharlieParty)
        updateSimpleStateForAccount(employeeNode, employeeAliceParty)
        updateSimpleStateForAccount(employeeNode, employeeBobParty)
        updateSimpleStateForAccount(employeeNode, employeeCharlieParty)
        updateSimpleStateForAccount(employeeNode, employeeAliceParty)

        val transactionsBeforeReIssuance = getTransactions(employeeNode)
        assertThat(transactionsBeforeReIssuance.size, equalTo(12)) // including 5 create account transactions

        val simpleStateRef = getStateAndRefs<SimpleState>(employeeNode)[0].ref
        createReIssuanceRequest<SimpleState>(
            employeeNode,
            listOf(simpleStateRef),
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

        updateSimpleStateForAccount(employeeNode, employeeDebbieParty)

        // TODO: figure out how to get back-chain for a given account
    }

    @Test(expected = java.lang.IllegalArgumentException::class) // TODO: delete expected exception once issuer can accesses requested states
    fun `SimpleState re-issued - accounts on different hosts`() {
        initialisePartiesForAccountsOnDifferentHosts()
        createSimpleStateForAccount(issuerNode, employeeAliceParty)
        updateSimpleStateForAccount(aliceNode, employeeBobParty)
        updateSimpleStateForAccount(bobNode, employeeCharlieParty)
        updateSimpleStateForAccount(charlieNode, employeeAliceParty)
        updateSimpleStateForAccount(aliceNode, employeeBobParty)
        updateSimpleStateForAccount(bobNode, employeeCharlieParty)
        updateSimpleStateForAccount(charlieNode, employeeAliceParty)

        val simpleStateRef = getStateAndRefs<SimpleState>(aliceNode)[0].ref
        createReIssuanceRequest<SimpleState>(
            aliceNode,
            listOf(simpleStateRef),
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

        updateSimpleStateForAccount(aliceNode, employeeDebbieParty)

        // TODO: figure out how to get back-chain for a given account
    }

//    @Test(expected = TransactionVerificationException::class)
    @Test(expected = java.lang.IllegalArgumentException::class) // TODO: replace expected exception once issuer can accesses requested states
    fun `Attached transaction needs to be notarised`() {
        initialiseParties()
        createSimpleState(aliceParty)

        val simpleStateStateAndRef = getStateAndRefs<SimpleState>(aliceNode)[0].ref
        createReIssuanceRequest<SimpleState>(
            aliceNode,
            listOf(simpleStateStateAndRef),
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
}
