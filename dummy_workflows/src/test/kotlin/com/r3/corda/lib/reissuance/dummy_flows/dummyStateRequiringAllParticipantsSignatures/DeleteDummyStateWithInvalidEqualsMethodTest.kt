package com.r3.corda.lib.reissuance.dummy_flows.dummyStateRequiringAllParticipantsSignatures

import com.r3.corda.lib.reissuance.dummy_flows.AbstractFlowTest
import com.r3.corda.lib.reissuance.dummy_states.DummyStateRequiringAllParticipantsSignatures
import org.hamcrest.MatcherAssert.*
import org.hamcrest.Matchers.empty
import org.junit.Test

class DeleteDummyStateWithInvalidEqualsMethodTest: AbstractFlowTest() {

    @Test
    fun `Delete DummyStateRequiringAllParticipantsSignatures`() {
        initialiseParties()
        createDummyStateRequiringAllParticipantsSignatures(aliceParty)
        deleteDummyStateRequiringAllParticipantsSignatures(aliceNode)

        val dummyStatesRequiringAllParticipantsSignatures = getStateAndRefs<DummyStateRequiringAllParticipantsSignatures>(aliceNode)
        assertThat(dummyStatesRequiringAllParticipantsSignatures, empty())
    }

}
