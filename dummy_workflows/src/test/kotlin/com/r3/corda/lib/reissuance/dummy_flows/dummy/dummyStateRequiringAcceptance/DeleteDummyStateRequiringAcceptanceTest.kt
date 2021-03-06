package com.r3.corda.lib.reissuance.dummy_flows.dummy.dummyStateRequiringAcceptance

import com.r3.corda.lib.reissuance.dummy_flows.AbstractFlowTest
import com.r3.corda.lib.reissuance.dummy_states.DummyStateRequiringAcceptance
import org.hamcrest.MatcherAssert.*
import org.hamcrest.Matchers.empty
import org.junit.Test

class DeleteDummyStateRequiringAcceptanceTest: AbstractFlowTest() {

    @Test
    fun `Delete DummyStateRequiringAcceptance`() {
        initialiseParties()
        createDummyStateRequiringAcceptance(aliceParty)
        deleteDummyStateRequiringAcceptance(aliceNode)

        val dummyStatesRequiringAcceptance = getStateAndRefs<DummyStateRequiringAcceptance>(aliceNode)
        assertThat(dummyStatesRequiringAcceptance, empty())
    }

}
