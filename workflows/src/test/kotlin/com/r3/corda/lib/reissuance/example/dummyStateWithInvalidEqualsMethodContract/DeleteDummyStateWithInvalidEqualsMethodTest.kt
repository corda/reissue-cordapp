package com.r3.corda.lib.reissuance.example.dummyStateWithInvalidEqualsMethodContract

import com.r3.corda.lib.reissuance.AbstractFlowTest
import com.r3.corda.lib.reissuance.states.example.SimpleDummyState
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
