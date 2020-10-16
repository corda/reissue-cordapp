package com.r3.corda.lib.reissuance.dummy_flows.dummy.dummyStateWithInvalidEqualsMethodContract

import com.r3.corda.lib.reissuance.dummy_flows.AbstractFlowTest
import com.r3.corda.lib.reissuance.dummy_states.DummyStateWithInvalidEqualsMethod
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.hasSize
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