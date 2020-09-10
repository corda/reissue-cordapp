package com.template

import com.r3.corda.lib.tokens.contracts.commands.IssueTokenCommand
import com.template.contracts.example.SimpleStateContract
import com.template.contracts.example.StateNeedingAcceptanceContract
import com.template.contracts.example.StateNeedingAllParticipantsToSignContract
import com.template.states.example.SimpleState
import com.template.states.example.StateNeedingAcceptance
import com.template.states.example.StateNeedingAllParticipantsToSign
import net.corda.core.contracts.TransactionVerificationException
import org.junit.Test

class RequestReIssuanceTest: AbstractFlowTest() {

    @Test
    fun `SimpleState re-issuance request is created`() {
        initialiseParties()
        createSimpleState(aliceParty)

        val simpleStateStateAndRef = getStateAndRefs<SimpleState>(aliceNode) // a list of 1 SimpleState
        createReIssuanceRequest(
            aliceNode,
            simpleStateStateAndRef,
            SimpleStateContract.Commands.Create(),
            issuerParty
        )
    }

    @Test
    fun `StateNeedingAcceptance re-issuance request is created`() {
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
    }

    @Test
    fun `StateNeedingAllParticipantsToSign re-issuance request is created`() {
        initialiseParties()
        createStateNeedingAllParticipantsToSign(aliceParty)

        val stateNeedingAllParticipantsToSignStateAndRef = getStateAndRefs<StateNeedingAllParticipantsToSign>(aliceNode) // a list of 1 SimpleState
        createReIssuanceRequest(
            aliceNode,
            stateNeedingAllParticipantsToSignStateAndRef,
            StateNeedingAllParticipantsToSignContract.Commands.Create(),
            issuerParty,
            listOf(aliceParty, issuerParty, acceptorParty)
        )
    }

    @Test
    fun `Tokens re-issuance request is created`() {
        initialiseParties()
        issueTokens(aliceParty, 50)

        val tokens = getTokens(aliceNode)
        createReIssuanceRequest(
            aliceNode,
            tokens,
            IssueTokenCommand(issuedTokenType, tokens.indices.toList()),
            issuerParty
        )
    }

    @Test
    fun `SimpleState re-issuance request is created - accounts on the same host`() {
        initialisePartiesForAccountsOnTheSameHost()
        createSimpleStateForAccount(employeeNode, employeeAliceParty)

        val simpleStateStateAndRef = getStateAndRefs<SimpleState>(employeeNode)[0]
        createReIssuanceRequest(
            employeeNode,
            listOf(simpleStateStateAndRef),
            SimpleStateContract.Commands.Create(),
            employeeIssuerParty,
            requester = employeeAliceParty
        )
    }

    @Test
    fun `SimpleState re-issuance request is created - accounts on different hosts`() {
        initialisePartiesForAccountsOnDifferentHosts()
        createSimpleStateForAccount(issuerNode, employeeAliceParty)

        val simpleStateStateAndRef = getStateAndRefs<SimpleState>(aliceNode)[0]
        createReIssuanceRequest(
            aliceNode,
            listOf(simpleStateStateAndRef),
            SimpleStateContract.Commands.Create(),
            employeeIssuerParty,
            requester = employeeAliceParty
        )
    }

    @Test(expected = TransactionVerificationException::class)
    fun `Request re-issuance of 0 states can't be created`() {
        initialiseParties()
        createReIssuanceRequest(
            aliceNode,
            listOf(),
            SimpleStateContract.Commands.Create(),
            issuerParty
        )
    }
}
