package com.r3.corda.lib.reissuance.dummy_flows

import com.r3.corda.lib.reissuance.dummy_states.DummyStateWithInvalidEqualsMethod
import com.r3.corda.lib.reissuance.dummy_states.SimpleDummyState
import org.hamcrest.MatcherAssert.*
import org.hamcrest.Matchers.*
import org.junit.Test

class DeleteSimpleDummyStateAndCreateDummyStateWithInvalidEqualsMethodTest: AbstractFlowTest() {

    @Test
    fun `Delete SimpleDummyState and create DummyStateWithInvalidEqualsMethod`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)
        deleteSimpleDummyStateAndCreateDummyStateWithInvalidEqualsMethod(aliceNode)

        val simpleDummyStates = getStateAndRefs<SimpleDummyState>(aliceNode)
        val dummyStatesWithInvalidEqualsMethod = getStateAndRefs<DummyStateWithInvalidEqualsMethod>(aliceNode)
        assertThat(simpleDummyStates, empty())
        assertThat(dummyStatesWithInvalidEqualsMethod, hasSize(`is`(1)))
    }

}
