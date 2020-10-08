package com.r3.corda.lib.reissuance.dummy_flows.simpleDummyState

import com.r3.corda.lib.reissuance.dummy_flows.AbstractFlowTest
import com.r3.corda.lib.reissuance.dummy_states.SimpleDummyState
import org.hamcrest.MatcherAssert.*
import org.hamcrest.Matchers.empty
import org.junit.Test

class DeleteSimpleDummyStateTest: AbstractFlowTest() {

    @Test
    fun `Delete SimpleDummyState`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)
        deleteSimpleDummyState(aliceNode)

        val simpleDummyStates = getStateAndRefs<SimpleDummyState>(aliceNode)
        assertThat(simpleDummyStates, empty())
    }

    @Test
    fun `Delete SimpleDummyState - accounts on the same host`() {
        initialisePartiesForAccountsOnTheSameHost()
        createSimpleDummyStateForAccount(employeeNode, employeeAliceParty)
        deleteSimpleDummyStateForAccount(employeeNode)

        val simpleDummyStates = getStateAndRefs<SimpleDummyState>(employeeNode)
        assertThat(simpleDummyStates, empty())
    }

    @Test
    fun `Delete SimpleDummyState - accounts on different hosts`() {
        initialisePartiesForAccountsOnDifferentHosts()
        createSimpleDummyStateForAccount(issuerNode, employeeAliceParty)
        deleteSimpleDummyStateForAccount(aliceNode)

        val simpleDummyStates = getStateAndRefs<SimpleDummyState>(aliceNode)
        assertThat(simpleDummyStates, empty())
    }
}
