package com.template.example.dummyStateWithInvalidEqualsMethodContract

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.hasSize
import com.template.AbstractFlowTest
import com.template.states.example.DummyStateWithInvalidEqualsMethod
import org.junit.Test

class CreateDummyStateWithInvalidEqualsMethodTest: AbstractFlowTest() {

    @Test
    fun `Create DummyStateWithInvalidEqualsMethod`() {
        initialiseParties()
        createDummyStateWithInvalidEqualsMethod(aliceParty, 5)

        val simpleDummyStatesWithInvalidEqualsMethod = getStateAndRefs<DummyStateWithInvalidEqualsMethod>(aliceNode)
        assertThat(simpleDummyStatesWithInvalidEqualsMethod, hasSize(1))
        val simpleDummyStateWithInvalidEqualsMethod = simpleDummyStatesWithInvalidEqualsMethod[0].state.data
        assertThat(simpleDummyStateWithInvalidEqualsMethod.owner, `is`(aliceParty))
        assertThat(simpleDummyStateWithInvalidEqualsMethod.issuer, `is`(issuerParty))
        assertThat(simpleDummyStateWithInvalidEqualsMethod.quantity, `is`(5))
    }
}