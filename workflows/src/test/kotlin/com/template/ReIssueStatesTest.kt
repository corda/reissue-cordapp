package com.template

import com.r3.corda.lib.tokens.contracts.commands.IssueTokenCommand
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.template.contracts.example.SimpleStateContract
import com.template.contracts.example.StateNeedingAcceptanceContract
import com.template.contracts.example.StateNeedingAllParticipantsToSignContract
import com.template.states.ReIssuanceRequest
import com.template.states.example.SimpleState
import com.template.states.example.StateNeedingAcceptance
import com.template.states.example.StateNeedingAllParticipantsToSign
import net.corda.core.node.services.queryBy
import org.junit.Test

class ReIssueStatesTest: AbstractFlowTest() {

    @Test
    fun `SimpleState is re-issued`() {
        initialiseParties()
        createSimpleState(aliceParty)

        val lastSimpleStateTransaction = getSignedTransactions(aliceNode).last()

        val simpleStateRef = getStateAndRefs<SimpleState>(aliceNode)[0].ref
        createReIssuanceRequest<SimpleState>(
            aliceNode,
            listOf(simpleStateRef),
            SimpleStateContract.Commands.Create(),
            issuerParty
        )

        sendSignedTransaction(aliceNode, issuerParty, lastSimpleStateTransaction)

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<SimpleState>(issuerNode, reIssuanceRequest)

    }

    @Test
    fun `StateNeedingAcceptance is re-issued`() {
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
    }

    @Test
    fun `StateNeedingAllParticipantsToSign is re-issued`() {
        initialiseParties()
        createStateNeedingAllParticipantsToSign(aliceParty)

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
    }

    @Test
    fun `Tokens are re-issued`() {
        initialiseParties()
        issueTokens(aliceParty, 50)

        val tokenRefs = getTokens(aliceNode).map { it.ref }

        val lastTokensTransaction = getSignedTransactions(aliceNode).last()

        createReIssuanceRequest<FungibleToken>(
            aliceNode,
            tokenRefs,
            IssueTokenCommand(issuedTokenType, tokenRefs.indices.toList()),
            issuerParty
        )

        sendSignedTransaction(aliceNode, issuerParty, lastTokensTransaction)

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<FungibleToken>(issuerNode, reIssuanceRequest)
    }

    // TODO: many tokens are re-issued

    @Test
    fun `SimpleState re-issued - accounts on the same host`() {
        initialisePartiesForAccountsOnTheSameHost()
        createSimpleStateForAccount(employeeNode, employeeAliceParty)

        val createSimpleStateTransaction = getSignedTransactions(employeeNode).last()

        val simpleStateRef = getStateAndRefs<SimpleState>(employeeNode)[0].ref
        createReIssuanceRequest<SimpleState>(
            employeeNode,
            listOf(simpleStateRef),
            SimpleStateContract.Commands.Create(),
            employeeIssuerParty
        )

        sendSignedTransaction(employeeNode, employeeIssuerParty, createSimpleStateTransaction)

        val reIssuanceRequest = employeeNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<SimpleState>(employeeNode, reIssuanceRequest)
    }

    @Test
    fun `SimpleState re-issued - accounts on different hosts`() {
        initialisePartiesForAccountsOnDifferentHosts()
        createSimpleStateForAccount(issuerNode, employeeAliceParty)

        val createSimpleStateTransaction = getSignedTransactions(aliceNode).last()

        val simpleStateRef = getStateAndRefs<SimpleState>(aliceNode)[0].ref
        createReIssuanceRequest<SimpleState>(
            aliceNode,
            listOf(simpleStateRef),
            SimpleStateContract.Commands.Create(),
            employeeIssuerParty,
            requester = employeeAliceParty
        )

        sendSignedTransaction(aliceNode, issuerParty, createSimpleStateTransaction)

        val reIssuanceRequest = aliceNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<SimpleState>(issuerNode, reIssuanceRequest)
    }
}
