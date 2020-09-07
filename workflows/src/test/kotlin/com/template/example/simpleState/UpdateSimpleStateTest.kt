package com.template.example.simpleState

import com.template.AbstractFlowTest
import org.junit.Test

class UpdateSimpleStateTest: AbstractFlowTest() {

    @Test
    fun `Update simple state`() {
        initialiseParties()
        createSimpleState(aliceParty)
        updateSimpleState(aliceNode, bobParty)
    }

    @Test
    fun `Update simple state many times`() {
        initialiseParties()
        createSimpleState(aliceParty)
        updateSimpleState(aliceNode, bobParty)
        updateSimpleState(bobNode, charlieParty)
        updateSimpleState(charlieNode, debbieParty)
        updateSimpleState(debbieNode, charlieParty)
        updateSimpleState(charlieNode, bobParty)
        updateSimpleState(bobNode, aliceParty)
    }

    @Test
    fun `Update simple state for account`() {
        initialisePartiesForAccountsOnTheSameHost()
        createSimpleStateForAccount(employeeNode, employeeAliceParty)
        updateSimpleStateForAccount(employeeBobParty)
    }
}