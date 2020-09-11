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

        val simpleStateRef = getStateAndRefs<SimpleState>(aliceNode)[0].ref
        createReIssuanceRequestAndShareRequiredTransactions<SimpleState>(
            aliceNode,
            listOf(simpleStateRef),
            SimpleStateContract.Commands.Create(),
            issuerParty
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<SimpleState>(issuerNode, reIssuanceRequest)
    }

    @Test
    fun `StateNeedingAcceptance is re-issued`() {
        initialiseParties()
        createStateNeedingAcceptance(aliceParty)

        val stateNeedingAcceptanceRef = getStateAndRefs<StateNeedingAcceptance>(aliceNode)[0].ref
        createReIssuanceRequestAndShareRequiredTransactions<StateNeedingAcceptance>(
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
        createReIssuanceRequestAndShareRequiredTransactions<StateNeedingAllParticipantsToSign>(
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

        createReIssuanceRequestAndShareRequiredTransactions<FungibleToken>(
            aliceNode,
            tokenRefs,
            IssueTokenCommand(issuedTokenType, tokenRefs.indices.toList()),
            issuerParty
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<FungibleToken>(issuerNode, reIssuanceRequest)
    }

    // TODO: many tokens are re-issued

    @Test
    fun `SimpleState re-issued - accounts on the same host`() {
        initialisePartiesForAccountsOnTheSameHost()
        createSimpleStateForAccount(employeeNode, employeeAliceParty)

        val simpleStateRef = getStateAndRefs<SimpleState>(employeeNode)[0].ref
        createReIssuanceRequestAndShareRequiredTransactions<SimpleState>(
            employeeNode,
            listOf(simpleStateRef),
            SimpleStateContract.Commands.Create(),
            employeeIssuerParty
        )

        val reIssuanceRequest = employeeNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<SimpleState>(employeeNode, reIssuanceRequest)
    }

    @Test
    fun `SimpleState re-issued - accounts on different hosts`() {
        initialisePartiesForAccountsOnDifferentHosts()
        createSimpleStateForAccount(issuerNode, employeeAliceParty)

        val simpleStateRef = getStateAndRefs<SimpleState>(aliceNode)[0].ref
        createReIssuanceRequestAndShareRequiredTransactions<SimpleState>(
            aliceNode,
            listOf(simpleStateRef),
            SimpleStateContract.Commands.Create(),
            employeeIssuerParty,
            requester = employeeAliceParty
        )

        val reIssuanceRequest = aliceNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<SimpleState>(issuerNode, reIssuanceRequest)
    }

    @Test(expected = IllegalArgumentException::class) // issues find state by reference
    fun `State cannot be re-issued if issuer cannot access it`() {
        initialiseParties()
        createSimpleState(aliceParty)

        val simpleStateRef = getStateAndRefs<SimpleState>(aliceNode)[0].ref
        createReIssuanceRequest<SimpleState>( // don't share required transactions
            aliceNode,
            listOf(simpleStateRef),
            SimpleStateContract.Commands.Create(),
            issuerParty
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<SimpleState>(issuerNode, reIssuanceRequest)
    }

    @Test // issuer doesn't have an information about state being consumed
    fun `Consumed state is re-issued when issuer is not a participant`() {
        initialiseParties()
        createSimpleState(aliceParty)

        val simpleStateRef = getStateAndRefs<SimpleState>(aliceNode)[0].ref

        updateSimpleState(aliceNode, bobParty)

        createReIssuanceRequestAndShareRequiredTransactions<SimpleState>(
            aliceNode,
            listOf(simpleStateRef),
            SimpleStateContract.Commands.Create(),
            issuerParty
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<SimpleState>(issuerNode, reIssuanceRequest)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Consumed state isn't re-issued when issuer is a participant`() {
        initialiseParties()
        createStateNeedingAcceptance(aliceParty)

        val stateNeedingAcceptanceRef = getStateAndRefs<StateNeedingAcceptance>(aliceNode)[0].ref
        createReIssuanceRequestAndShareRequiredTransactions<StateNeedingAcceptance>(
            aliceNode,
            listOf(stateNeedingAcceptanceRef),
            StateNeedingAcceptanceContract.Commands.Create(),
            issuerParty,
            listOf(issuerParty, acceptorParty)
        )

        updateStateNeedingAcceptance(aliceNode, bobParty)

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<StateNeedingAcceptance>(issuerNode, reIssuanceRequest)
    }
}
