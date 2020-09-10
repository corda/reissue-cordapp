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
import java.lang.IllegalArgumentException

class ReIssueStatesTest: AbstractFlowTest() {

    @Test(expected = IllegalArgumentException::class) // TODO: delete expected exception once issuer can accesses requested states
    fun `SimpleState is re-issued`() {
        initialiseParties()
        createSimpleState(aliceParty)

        val simpleStateRef = getStateAndRefs<SimpleState>(aliceNode)[0].ref
        createReIssuanceRequest<SimpleState>(
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

    @Test(expected = IllegalArgumentException::class) // TODO: delete expected exception once issuer can accesses requested states
    fun `Tokens are re-issued`() {
        initialiseParties()
        issueTokens(aliceParty, 50)

        val tokenRefs = getTokens(aliceNode).map { it.ref }
        val issuerTokenRefs = getTokens(issuerNode).map { it.ref }

        createReIssuanceRequest<FungibleToken>(
            aliceNode,
            tokenRefs,
            IssueTokenCommand(issuedTokenType, tokenRefs.indices.toList()),
            issuerParty
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<FungibleToken>(issuerNode, reIssuanceRequest)
    }

    @Test(expected = IllegalArgumentException::class) // TODO: delete expected exception once issuer can accesses requested states
    fun `SimpleState re-issued - accounts on the same host`() {
        initialisePartiesForAccountsOnTheSameHost()
        createSimpleStateForAccount(employeeNode, employeeAliceParty)

        val simpleStateRef = getStateAndRefs<SimpleState>(employeeNode)[0].ref
        createReIssuanceRequest<SimpleState>(
            employeeNode,
            listOf(simpleStateRef),
            SimpleStateContract.Commands.Create(),
            employeeIssuerParty
        )

        val reIssuanceRequest = employeeNode.services.vaultService.queryBy<ReIssuanceRequest>().states[0]
        reIssueRequestedStates<SimpleState>(employeeNode, reIssuanceRequest)
    }

    @Test(expected = IllegalArgumentException::class) // TODO: delete expected exception once issuer can accesses requested states
    fun `SimpleState re-issued - accounts on different hosts`() {
        initialisePartiesForAccountsOnDifferentHosts()
        createSimpleStateForAccount(issuerNode, employeeAliceParty)

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
    }
}
