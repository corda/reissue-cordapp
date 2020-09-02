package com.template

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.template.contracts.example.SimpleStateContract
import com.template.contracts.example.StateNeedingAcceptanceContract
import com.template.contracts.example.StateNeedingAllParticipantsToSignContract
import com.template.states.example.SimpleState
import com.template.states.example.StateNeedingAcceptance
import com.template.states.example.StateNeedingAllParticipantsToSign
import org.junit.Test

class CreateReIssuanceRequestTest: AbstractFlowTest() {

    @Test
    fun `SimpleState re-issuance request is created`() {
        createSimpleState(aliceParty)
        updateSimpleState(aliceNode, bobParty)
        updateSimpleState(bobNode, charlieParty)
        updateSimpleState(charlieNode, debbieParty)
        updateSimpleState(debbieNode, charlieParty)
        updateSimpleState(charlieNode, bobParty)
        updateSimpleState(bobNode, aliceParty)

        val transactions = getTransactions(aliceNode)

        assertThat(transactions.size, equalTo(7))

        val simpleStateStateAndRef = getStateAndRefs<SimpleState>(aliceNode)[0]

        createReIssuanceRequest(
            aliceNode,
            simpleStateStateAndRef,
            SimpleStateContract.Commands.Create()
        )
    }

    @Test
    fun `StateNeedingAcceptance re-issuance request is created`() {
        createStateNeedingAcceptance(aliceParty)
        updateStateNeedingAcceptance(aliceNode, bobParty)
        updateStateNeedingAcceptance(bobNode, charlieParty)
        updateStateNeedingAcceptance(charlieNode, debbieParty)
        updateStateNeedingAcceptance(debbieNode, charlieParty)
        updateStateNeedingAcceptance(charlieNode, bobParty)
        updateStateNeedingAcceptance(bobNode, aliceParty)

        val transactions = getTransactions(aliceNode)

        assertThat(transactions.size, equalTo(7))

        val stateNeedingAcceptanceStateAndRef = getStateAndRefs<StateNeedingAcceptance>(aliceNode)[0]

        createReIssuanceRequest(
            aliceNode,
            stateNeedingAcceptanceStateAndRef,
            StateNeedingAcceptanceContract.Commands.Create(),
            listOf(issuerParty, acceptorParty)
        )
    }

    @Test
    fun `StateNeedingAllParticipantsToSign re-issuance request is created`() {
        createStateNeedingAllParticipantsToSign(aliceParty)
        updateStateNeedingAllParticipantsToSign(aliceNode, bobParty)
        updateStateNeedingAllParticipantsToSign(bobNode, charlieParty)
        updateStateNeedingAllParticipantsToSign(charlieNode, debbieParty)
        updateStateNeedingAllParticipantsToSign(debbieNode, charlieParty)
        updateStateNeedingAllParticipantsToSign(charlieNode, bobParty)
        updateStateNeedingAllParticipantsToSign(bobNode, aliceParty)

        val transactions = getTransactions(aliceNode)

        assertThat(transactions.size, equalTo(7))

        val stateNeedingAllParticipantsToSignStateAndRef = getStateAndRefs<StateNeedingAllParticipantsToSign>(aliceNode)[0]

        createReIssuanceRequest(
            aliceNode,
            stateNeedingAllParticipantsToSignStateAndRef,
            StateNeedingAllParticipantsToSignContract.Commands.Create(),
            listOf(aliceParty, issuerParty, acceptorParty)
        )
    }

    @Test
    fun `Tokens re-issuance request is created`() {
        issueTokens(aliceParty, 50)

        transferTokens(aliceNode, bobParty, 50)
        transferTokens(bobNode, charlieParty, 50)
        transferTokens(charlieNode, debbieParty, 50)
        transferTokens(debbieNode, bobParty, 50)
        transferTokens(bobNode, aliceParty, 50)

        val transactions = getTransactions(aliceNode)

        assertThat(transactions.size, equalTo(6))

        val tokens = getTokens(aliceNode)

        createTokenReIssuanceRequest(aliceNode, tokens)
    }

}