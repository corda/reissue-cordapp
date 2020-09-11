package com.template

import com.r3.corda.lib.tokens.contracts.commands.IssueTokenCommand
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
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

        val simpleStateRef = getStateAndRefs<SimpleState>(aliceNode)[0].ref
        createReIssuanceRequestAndShareRequiredTransactions<SimpleState>(
            aliceNode,
            listOf(simpleStateRef),
            SimpleStateContract.Commands.Create(),
            issuerParty
        )
    }

    @Test
    fun `StateNeedingAcceptance re-issuance request is created`() {
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
    }

    @Test
    fun `StateNeedingAllParticipantsToSign re-issuance request is created`() {
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
    }

    @Test
    fun `Tokens re-issuance request is created`() {
        initialiseParties()
        issueTokens(aliceParty, 50)

        val tokenRefs = getTokens(aliceNode).map { it.ref }
        createReIssuanceRequestAndShareRequiredTransactions<FungibleToken>(
            aliceNode,
            tokenRefs,
            IssueTokenCommand(issuedTokenType, tokenRefs.indices.toList()),
            issuerParty
        )
    }

    @Test
    fun `SimpleState re-issuance request is created - accounts on the same host`() {
        initialisePartiesForAccountsOnTheSameHost()
        createSimpleStateForAccount(employeeNode, employeeAliceParty)

        val simpleStateRef = getStateAndRefs<SimpleState>(employeeNode)[0].ref
        createReIssuanceRequestAndShareRequiredTransactions<SimpleState>(
            employeeNode,
            listOf(simpleStateRef),
            SimpleStateContract.Commands.Create(),
            employeeIssuerParty,
            requester = employeeAliceParty
        )
    }

    @Test
    fun `SimpleState re-issuance request is created - accounts on different hosts`() {
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
    }

    @Test(expected = TransactionVerificationException::class)
    fun `Request re-issuance of 0 states can't be created`() {
        initialiseParties()
        createReIssuanceRequestAndShareRequiredTransactions<SimpleState>(
            aliceNode,
            listOf(),
            SimpleStateContract.Commands.Create(),
            issuerParty
        )
    }
}
