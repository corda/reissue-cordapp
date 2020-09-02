package com.template.example.stateNeedingAcceptance

import com.template.AbstractFlowTest
import org.junit.Test

class CreateStateNeedingAllParticipantsToSignTest: AbstractFlowTest() {

    @Test
    fun `Create state needing all participants to sign`() {
        createStateNeedingAllParticipantsToSign(aliceParty)
    }

}
