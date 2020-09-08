package com.template.example.simpleState

import com.template.AbstractFlowTest
import org.junit.Test

class DeleteSimpleStateTest: AbstractFlowTest() {

    @Test
    fun `Delete simple state`() {
        initialiseParties()
        createSimpleState(aliceParty)
        deleteSimpleState(aliceNode)
    }

    @Test
    fun `Delete simple state - accounts on the same host`() {
        initialisePartiesForAccountsOnTheSameHost()
        createSimpleStateForAccount(employeeNode, employeeAliceParty)
        deleteSimpleStateForAccount(employeeNode)
    }

    @Test
    fun `Delete simple state - accounts on different hosts`() {
        initialisePartiesForAccountOnDifferentHosts()
        createSimpleStateForAccount(issuerNode, employeeAliceParty)
        deleteSimpleStateForAccount(aliceNode)
    }
}
