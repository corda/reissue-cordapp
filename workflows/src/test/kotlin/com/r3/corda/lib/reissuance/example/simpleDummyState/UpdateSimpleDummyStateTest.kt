package com.r3.corda.lib.reissuance.example.simpleDummyState

import com.r3.corda.lib.reissuance.AbstractFlowTest
import com.r3.corda.lib.reissuance.states.example.SimpleDummyState
import net.corda.core.identity.AbstractParty
import org.hamcrest.MatcherAssert.*
import org.hamcrest.Matchers.*
import org.junit.Test

class UpdateSimpleDummyStateTest: AbstractFlowTest() {

    @Test
    fun `Update SimpleDummyState`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)
        updateSimpleDummyState(aliceNode, bobParty)

        val simpleDummyStates = getStateAndRefs<SimpleDummyState>(bobNode)
        assertThat(simpleDummyStates, hasSize(1))
        val simpleDummyState = simpleDummyStates[0].state.data
        assertThat(simpleDummyState.owner, `is`(bobParty as AbstractParty))
    }

    @Test
    fun `Update SimpleDummyState many times`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)
        updateSimpleDummyState(aliceNode, bobParty)
        updateSimpleDummyState(bobNode, charlieParty)
        updateSimpleDummyState(charlieNode, debbieParty)
        updateSimpleDummyState(debbieNode, charlieParty)
        updateSimpleDummyState(charlieNode, bobParty)
        updateSimpleDummyState(bobNode, aliceParty)

        val simpleDummyStates = getStateAndRefs<SimpleDummyState>(aliceNode)
        assertThat(simpleDummyStates, hasSize(1))
        val simpleDummyState = simpleDummyStates[0].state.data
        assertThat(simpleDummyState.owner, `is`(aliceParty as AbstractParty))
    }

    @Test
    fun `Update SimpleDummyState - accounts on the same host`() {
        initialisePartiesForAccountsOnTheSameHost()
        createSimpleDummyStateForAccount(employeeNode, employeeAliceParty)
        updateSimpleDummyStateForAccount(employeeNode, employeeBobParty)

        val simpleDummyStates = getStateAndRefs<SimpleDummyState>(employeeNode)
        assertThat(simpleDummyStates, hasSize(1))
        val simpleDummyState = simpleDummyStates[0].state.data
        assertThat(simpleDummyState.owner, `is`(employeeBobParty))
    }

    @Test
    fun `Update SimpleDummyState - accounts on different hosts`() {
        initialisePartiesForAccountsOnDifferentHosts()
        createSimpleDummyStateForAccount(issuerNode, employeeAliceParty)
        updateSimpleDummyStateForAccount(aliceNode, employeeBobParty)

        val simpleDummyStates = getStateAndRefs<SimpleDummyState>(bobNode)
        assertThat(simpleDummyStates, hasSize(1))
        val simpleDummyState = simpleDummyStates[0].state.data
        assertThat(simpleDummyState.owner, `is`(employeeBobParty))
    }
}