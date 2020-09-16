package com.template.example.simpleDummyState

import com.template.AbstractFlowTest
import org.junit.Test

class UpdateSimpleDummyStateTest: AbstractFlowTest() {

    @Test
    fun `Update SimpleDummyState`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)
        updateSimpleDummyState(aliceNode, bobParty)
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
    }

    @Test
    fun `Update SimpleDummyState - accounts on the same host`() {
        initialisePartiesForAccountsOnTheSameHost()
        createSimpleDummyStateForAccount(employeeNode, employeeAliceParty)
        updateSimpleDummyStateForAccount(employeeNode, employeeBobParty)
    }

    @Test
    fun `Update SimpleDummyState - accounts on different hosts`() {
        initialisePartiesForAccountsOnDifferentHosts()
        createSimpleDummyStateForAccount(issuerNode, employeeAliceParty)
        updateSimpleDummyStateForAccount(aliceNode, employeeBobParty)
    }
}