package com.template.example.stateNeedingAcceptance

import com.template.AbstractFlowTest
import org.junit.Test

class DeleteStateNeedingAllParticipantsToSignTest: AbstractFlowTest() {

    @Test
    fun `Delete state needing all participants to sign`() {
        initialiseParties()
        createStateNeedingAllParticipantsToSign(aliceParty)
        deleteStateNeedingAllParticipantsToSign(aliceNode)
    }

}
