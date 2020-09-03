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

class CreateReIssuanceRequestTest: AbstractFlowTest() {

    @Test
    fun `SimpleState re-issuance request is created`() {
        createSimpleState(aliceParty)

        val simpleStateStateAndRef = getStateAndRefs<SimpleState>(aliceNode)[0]
        createReIssuanceRequest(
            aliceNode,
            listOf(simpleStateStateAndRef),
            SimpleStateContract.Commands.Create()
        )
    }

    @Test
    fun `StateNeedingAcceptance re-issuance request is created`() {
        createStateNeedingAcceptance(aliceParty)

        val stateNeedingAcceptanceStateAndRef = getStateAndRefs<StateNeedingAcceptance>(aliceNode)[0]
        createReIssuanceRequest(
            aliceNode,
            listOf(stateNeedingAcceptanceStateAndRef),
            StateNeedingAcceptanceContract.Commands.Create(),
            commandSigners = listOf(issuerParty, acceptorParty)
        )
    }

    @Test
    fun `StateNeedingAllParticipantsToSign re-issuance request is created`() {
        createStateNeedingAllParticipantsToSign(aliceParty)

        val stateNeedingAllParticipantsToSignStateAndRef = getStateAndRefs<StateNeedingAllParticipantsToSign>(aliceNode)[0]
        createReIssuanceRequest(
            aliceNode,
            listOf(stateNeedingAllParticipantsToSignStateAndRef),
            StateNeedingAllParticipantsToSignContract.Commands.Create(),
            commandSigners = listOf(aliceParty, issuerParty, acceptorParty)
        )
    }

    @Test
    fun `Tokens re-issuance request is created`() {
        issueTokens(aliceParty, 50)

        val tokens = getTokens(aliceNode)
        createReIssuanceRequest(aliceNode, tokens, IssueTokenCommand(issuedTokenType, tokens.indices.toList()))
    }

    @Test
    fun `SimpleState re-issuance request is created when holder is an account`() {
        createSimpleStateForAccount(employeeAliceParty)

        val simpleStateStateAndRef = getStateAndRefs<SimpleState>(employeeNode)[0]
        createReIssuanceRequest(
            employeeNode,
            listOf(simpleStateStateAndRef),
            SimpleStateContract.Commands.Create()
        )
    }

    @Test(expected = TransactionVerificationException::class)
    fun `Request re-issuance of 0 states can't be created`() {
        createReIssuanceRequest(
            aliceNode,
            listOf(),
            SimpleStateContract.Commands.Create()
        )
    }
}