package com.template.example.dummyStateRequiringAllParticipantsSignatures

import com.template.AbstractFlowTest
import com.template.states.example.DummyStateRequiringAllParticipantsSignatures
import org.hamcrest.MatcherAssert.*
import org.hamcrest.Matchers.*
import org.junit.Test

class CreateDummyStateRequiringAllParticipantsSignaturesTest: AbstractFlowTest() {

    @Test
    fun `Create DummyStateRequiringAllParticipantsSignatures`() {
        initialiseParties()
        createDummyStateRequiringAllParticipantsSignatures(aliceParty)

        val dummyStatesRequiringAllParticipantsSignatures = getStateAndRefs<DummyStateRequiringAllParticipantsSignatures>(aliceNode)
        assertThat(dummyStatesRequiringAllParticipantsSignatures, hasSize(1))
        val dummyStateRequiringAllParticipantsSignatures = dummyStatesRequiringAllParticipantsSignatures[0].state.data
        assertThat(dummyStateRequiringAllParticipantsSignatures.other, `is`(acceptorParty))
        assertThat(dummyStateRequiringAllParticipantsSignatures.issuer, `is`(issuerParty))
        assertThat(dummyStateRequiringAllParticipantsSignatures.owner, `is`(aliceParty))
    }

}
