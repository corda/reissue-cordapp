package com.r3.corda.lib.reissuance.example.dummyStateWithInvalidEqualsMethodContract

import com.r3.corda.lib.reissuance.AbstractFlowTest
import com.r3.corda.lib.reissuance.states.example.DummyStateWithInvalidEqualsMethod
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.hasSize
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