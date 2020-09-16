package com.template.example.dummyStateRequiringAllParticipantsSignatures

import com.template.AbstractFlowTest
import org.junit.Test

class DeleteDummyStateRequiringAllParticipantsSignaturesTest: AbstractFlowTest() {

    @Test
    fun `Delete DummyStateRequiringAllParticipantsSignatures`() {
        initialiseParties()
        createDummyStateRequiringAllParticipantsSignatures(aliceParty)
        deleteDummyStateRequiringAllParticipantsSignatures(aliceNode)
    }

}
