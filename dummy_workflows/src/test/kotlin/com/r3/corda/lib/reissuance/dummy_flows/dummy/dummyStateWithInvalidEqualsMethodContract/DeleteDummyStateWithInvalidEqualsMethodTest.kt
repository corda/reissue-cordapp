package com.r3.corda.lib.reissuance.dummy_flows.dummy.dummyStateWithInvalidEqualsMethodContract

import com.r3.corda.lib.reissuance.dummy_flows.AbstractFlowTest
import com.r3.corda.lib.reissuance.dummy_states.SimpleDummyState
import org.hamcrest.MatcherAssert.*
import org.hamcrest.Matchers.empty
import org.junit.Test

class DeleteDummyStateWithInvalidEqualsMethodTest: AbstractFlowTest() {

    @Test
    fun `Delete DummyStateWithInvalidEqualsMethod`() {
        initialiseParties()
        createDummyStateWithInvalidEqualsMethod(aliceParty, 5)
        deleteDummyStateWithInvalidEqualsMethod(aliceNode)

        val simpleDummyStates = getStateAndRefs<SimpleDummyState>(aliceNode)
        assertThat(simpleDummyStates, empty())
    }

}
