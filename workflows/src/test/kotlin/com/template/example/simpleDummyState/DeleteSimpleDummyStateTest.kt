package com.template.example.simpleDummyState

import com.template.AbstractFlowTest
import org.junit.Test

class DeleteSimpleDummyStateTest: AbstractFlowTest() {

    @Test
    fun `Delete SimpleDummyState`() {
        initialiseParties()
        createSimpleDummyState(aliceParty)
        deleteSimpleDummyState(aliceNode)
    }

    @Test
    fun `Delete SimpleDummyState - accounts on the same host`() {
        initialisePartiesForAccountsOnTheSameHost()
        createSimpleDummyStateForAccount(employeeNode, employeeAliceParty)
        deleteSimpleDummyStateForAccount(employeeNode)
    }

    @Test
    fun `Delete SimpleDummyState - accounts on different hosts`() {
        initialisePartiesForAccountsOnDifferentHosts()
        createSimpleDummyStateForAccount(issuerNode, employeeAliceParty)
        deleteSimpleDummyStateForAccount(aliceNode)
    }
}
