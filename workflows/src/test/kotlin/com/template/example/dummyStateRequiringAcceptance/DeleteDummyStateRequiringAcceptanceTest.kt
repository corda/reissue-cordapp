package com.template.example.dummyStateRequiringAcceptance

import com.template.AbstractFlowTest
import com.template.states.example.DummyStateRequiringAcceptance
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
