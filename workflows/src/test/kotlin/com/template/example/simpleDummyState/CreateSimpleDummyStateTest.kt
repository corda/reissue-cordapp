package com.template.example.simpleDummyState

import com.template.AbstractFlowTest
import org.junit.Test

class CreateSimpleDummyStateTest: AbstractFlowTest() {

    @Test
    fun `Create SimpleDummyState`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)
    }

    @Test
    fun `Create SimpleDummyState - accounts on the same host`() {
        initialisePartiesForAccountsOnTheSameHost()
        createSimpleDummyStateForAccount(employeeNode, employeeAliceParty)
    }

    @Test
    fun `Create SimpleDummyState - accounts on different hosts`() {
        initialisePartiesForAccountsOnDifferentHosts()
        createSimpleDummyStateForAccount(issuerNode, employeeAliceParty)
    }
}