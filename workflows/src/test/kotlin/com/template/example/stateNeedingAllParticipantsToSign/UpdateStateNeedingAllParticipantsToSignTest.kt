package com.template.example.stateNeedingAcceptance

import com.template.AbstractFlowTest
import org.junit.Test

class UpdateStateNeedingAllParticipantsToSignTest: AbstractFlowTest() {

    @Test
    fun `Update state needing all participants to sign`() {
        initialiseParties()
        createStateNeedingAllParticipantsToSign(aliceParty)
        updateStateNeedingAllParticipantsToSign(aliceNode, bobParty)
    }


    @Test
    fun `Update state needing all participants to sign many times`() {
        initialiseParties()
        createStateNeedingAllParticipantsToSign(aliceParty)
        updateStateNeedingAllParticipantsToSign(aliceNode, bobParty)
        updateStateNeedingAllParticipantsToSign(bobNode, charlieParty)
        updateStateNeedingAllParticipantsToSign(charlieNode, debbieParty)
        updateStateNeedingAllParticipantsToSign(debbieNode, charlieParty)
        updateStateNeedingAllParticipantsToSign(charlieNode, bobParty)
        updateStateNeedingAllParticipantsToSign(bobNode, aliceParty)
    }

}
