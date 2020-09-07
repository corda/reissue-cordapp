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

class ReIssueStateTest: AbstractFlowTest() {

    @Test
    fun `SimpleState is re-issued`() {
        initialiseParties()
        createSimpleState(aliceParty)

        val simpleStateStateAndRef = getStateAndRefs<SimpleState>(aliceNode) // a list of 1 SimpleState

        createReIssuanceRequest(
            aliceNode,
            simpleStateStateAndRef,
            SimpleStateContract.Commands.Create(),
            issuerParty
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest<SimpleState>>().states[0]

        reIssueRequestedStates(reIssuanceRequest)

    }

    @Test
    fun `StateNeedingAcceptance is re-issued`() {
        initialiseParties()
        createStateNeedingAcceptance(aliceParty)

        val stateNeedingAcceptanceStateAndRef = getStateAndRefs<StateNeedingAcceptance>(aliceNode) // a list of 1 SimpleState

        createReIssuanceRequest(
            aliceNode,
            stateNeedingAcceptanceStateAndRef,
            StateNeedingAcceptanceContract.Commands.Create(),
            issuerParty,
            listOf(issuerParty, acceptorParty)
        )
        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest<StateNeedingAcceptance>>().states[0]

        reIssueRequestedStates(reIssuanceRequest)
    }

    @Test
    fun `StateNeedingAllParticipantsToSign is re-issued`() {
        initialiseParties()
        createStateNeedingAllParticipantsToSign(aliceParty)

        val stateNeedingAllParticipantsToSignStateAndRef = getStateAndRefs<StateNeedingAllParticipantsToSign>(aliceNode)[0]

        createReIssuanceRequest(
            aliceNode,
            listOf(stateNeedingAllParticipantsToSignStateAndRef),
            StateNeedingAllParticipantsToSignContract.Commands.Create(),
            issuerParty,
            listOf(aliceParty, issuerParty, acceptorParty)
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest<StateNeedingAllParticipantsToSign>>().states[0]

        reIssueRequestedStates(reIssuanceRequest)
    }

    @Test
    fun `Tokens are re-issued`() {
        initialiseParties()
        issueTokens(aliceParty, 50)

        val tokens = getTokens(aliceNode)

        createReIssuanceRequest(
            aliceNode,
            tokens,
            IssueTokenCommand(issuedTokenType, tokens.indices.toList()),
            issuerParty
        )

        val reIssuanceRequest = issuerNode.services.vaultService.queryBy<ReIssuanceRequest<FungibleToken>>().states[0]

        reIssueRequestedStates(reIssuanceRequest)
    }

    @Test
    fun `SimpleState re-issued for an account`() {
        initialisePartiesForAccountsOnTheSameHost()
        createSimpleStateForAccount(employeeNode, employeeAliceParty)

        val simpleStateStateAndRef = getStateAndRefs<SimpleState>(employeeNode)[0]
        createReIssuanceRequest(
            employeeNode,
            listOf(simpleStateStateAndRef),
            SimpleStateContract.Commands.Create(),
            employeeIssuerParty
        )

        val reIssuanceRequest = employeeNode.services.vaultService.queryBy<ReIssuanceRequest<SimpleState>>().states[0]

        reIssueRequestedStates(reIssuanceRequest, employeeNode)
    }
}