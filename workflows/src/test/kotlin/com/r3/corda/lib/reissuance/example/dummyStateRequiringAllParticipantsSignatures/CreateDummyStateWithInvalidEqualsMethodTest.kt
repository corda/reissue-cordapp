package com.r3.corda.lib.reissuance.example.dummyStateRequiringAllParticipantsSignatures

import com.r3.corda.lib.reissuance.AbstractFlowTest
import com.r3.corda.lib.reissuance.dummy_states.DummyStateRequiringAllParticipantsSignatures
import org.hamcrest.MatcherAssert.*
import org.hamcrest.Matchers.*
import org.junit.Test

class CreateDummyStateWithInvalidEqualsMethodTest: AbstractFlowTest() {

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
