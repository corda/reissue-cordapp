package com.template.example.simpleState

import com.template.AbstractFlowTest
import org.junit.Test

class CreateSimpleStateTest: AbstractFlowTest() {

    @Test
    fun `Create simple state`() {
        initialiseParties()
        createSimpleState(aliceParty)
    }

    @Test
    fun `Create simple state - accounts on the same host`() {
        initialisePartiesForAccountsOnTheSameHost()
        createSimpleStateForAccount(employeeNode, employeeAliceParty)
    }

    @Test
    fun `Create simple state - accounts on different hosts`() {
        initialisePartiesForAccountsOnDifferentHosts()
        createSimpleStateForAccount(issuerNode, employeeAliceParty)
    }
}