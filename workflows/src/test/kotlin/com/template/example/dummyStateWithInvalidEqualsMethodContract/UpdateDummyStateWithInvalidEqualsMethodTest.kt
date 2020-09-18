package com.template.example.dummyStateWithInvalidEqualsMethodContract

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.hasSize
import com.template.AbstractFlowTest
import com.template.states.example.DummyStateWithInvalidEqualsMethod
import org.junit.Test

class UpdateDummyStateWithInvalidEqualsMethodTest: AbstractFlowTest() {

    @Test
    fun `Update DummyStateWithInvalidEqualsMethod`() {
        initialiseParties()
        createDummyStateWithInvalidEqualsMethod(aliceParty, 5)
        updateDummyStateWithInvalidEqualsMethod(aliceNode, bobParty)

        val simpleDummyStatesWithInvalidEqualsMethod = getStateAndRefs<DummyStateWithInvalidEqualsMethod>(bobNode)
        assertThat(simpleDummyStatesWithInvalidEqualsMethod, hasSize(1))
        val simpleDummyStateWithInvalidEqualsMethod = simpleDummyStatesWithInvalidEqualsMethod[0].state.data
        assertThat(simpleDummyStateWithInvalidEqualsMethod.owner, `is`(bobParty))
        assertThat(simpleDummyStateWithInvalidEqualsMethod.issuer, `is`(issuerParty))
        assertThat(simpleDummyStateWithInvalidEqualsMethod.quantity, `is`(5))
    }
}