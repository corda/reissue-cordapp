package com.template.example.dummyStateRequiringAllParticipantsSignatures

import com.template.AbstractFlowTest
import org.junit.Test

class CreateDummyStateRequiringAllParticipantsSignaturesTest: AbstractFlowTest() {

    @Test
    fun `Create DummyStateRequiringAllParticipantsSignatures`() {
        initialiseParties()
        createDummyStateRequiringAllParticipantsSignatures(aliceParty)
    }

}
