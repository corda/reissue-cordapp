package com.template.example.dummyStateRequiringAllParticipantsSignatures

import com.template.AbstractFlowTest
import org.junit.Test

class UpdateDummyStateRequiringAllParticipantsSignaturesTest: AbstractFlowTest() {

    @Test
    fun `Update DummyStateRequiringAllParticipantsSignatures`() {
        initialiseParties()
        createDummyStateRequiringAllParticipantsSignatures(aliceParty)
        updateDummyStateRequiringAllParticipantsSignatures(aliceNode, bobParty)
    }


    @Test
    fun `Update DummyStateRequiringAllParticipantsSignatures many times`() {
        initialiseParties()
        createDummyStateRequiringAllParticipantsSignatures(aliceParty)
        updateDummyStateRequiringAllParticipantsSignatures(aliceNode, bobParty)
        updateDummyStateRequiringAllParticipantsSignatures(bobNode, charlieParty)
        updateDummyStateRequiringAllParticipantsSignatures(charlieNode, debbieParty)
        updateDummyStateRequiringAllParticipantsSignatures(debbieNode, charlieParty)
        updateDummyStateRequiringAllParticipantsSignatures(charlieNode, bobParty)
        updateDummyStateRequiringAllParticipantsSignatures(bobNode, aliceParty)
    }

}
