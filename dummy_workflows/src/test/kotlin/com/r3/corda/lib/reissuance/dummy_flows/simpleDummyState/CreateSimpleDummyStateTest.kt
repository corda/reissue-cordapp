package com.r3.corda.lib.reissuance.dummy_flows.simpleDummyState

import com.r3.corda.lib.reissuance.dummy_flows.AbstractFlowTest
import com.r3.corda.lib.reissuance.dummy_states.SimpleDummyState
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.hasSize
import net.corda.core.identity.AbstractParty
import org.junit.Test

class CreateSimpleDummyStateTest: AbstractFlowTest() {

    @Test
    fun `Create SimpleDummyState`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)

        val simpleDummyStates = getStateAndRefs<SimpleDummyState>(aliceNode)
        assertThat(simpleDummyStates, hasSize(1))
        val simpleDummyState = simpleDummyStates[0].state.data
        assertThat(simpleDummyState.owner, `is`(aliceParty as AbstractParty))
    }

    @Test
    fun `Create SimpleDummyState - accounts on the same host`() {
        initialisePartiesForAccountsOnTheSameHost()
        createSimpleDummyStateForAccount(employeeNode, employeeAliceParty)

        val simpleDummyStates = getStateAndRefs<SimpleDummyState>(employeeNode)
        assertThat(simpleDummyStates, hasSize(1))
        val simpleDummyState = simpleDummyStates[0].state.data
        assertThat(simpleDummyState.owner, `is`(employeeAliceParty))
    }

    @Test
    fun `Create SimpleDummyState - accounts on different hosts`() {
        initialisePartiesForAccountsOnDifferentHosts()
        createSimpleDummyStateForAccount(issuerNode, employeeAliceParty)

        val simpleDummyStates = getStateAndRefs<SimpleDummyState>(aliceNode)
        assertThat(simpleDummyStates, hasSize(1))
        val simpleDummyState = simpleDummyStates[0].state.data
        assertThat(simpleDummyState.owner, `is`(employeeAliceParty))
    }
}