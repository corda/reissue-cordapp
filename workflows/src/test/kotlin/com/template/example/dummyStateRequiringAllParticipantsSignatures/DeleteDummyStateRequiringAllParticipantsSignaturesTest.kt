package com.template.example.dummyStateRequiringAllParticipantsSignatures

import com.template.AbstractFlowTest
import com.template.states.example.DummyStateRequiringAllParticipantsSignatures
import org.hamcrest.MatcherAssert.*
import org.hamcrest.Matchers.empty
import org.junit.Test

class DeleteDummyStateRequiringAllParticipantsSignaturesTest: AbstractFlowTest() {

    @Test
    fun `Delete DummyStateRequiringAllParticipantsSignatures`() {
        initialiseParties()
        createDummyStateRequiringAllParticipantsSignatures(aliceParty)
        deleteDummyStateRequiringAllParticipantsSignatures(aliceNode)

        val dummyStatesRequiringAllParticipantsSignatures = getStateAndRefs<DummyStateRequiringAllParticipantsSignatures>(aliceNode)
        assertThat(dummyStatesRequiringAllParticipantsSignatures, empty())
    }

}
