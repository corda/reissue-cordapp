package com.template.example.dummyStateWithInvalidEqualsMethodContract

import com.template.AbstractFlowTest
import com.template.states.example.SimpleDummyState
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
