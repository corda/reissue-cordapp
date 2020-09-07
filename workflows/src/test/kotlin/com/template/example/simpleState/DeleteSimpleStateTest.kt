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
    fun `Delete simple state for account`() {
        initialisePartiesForAccountsOnTheSameHost()
        createSimpleStateForAccount(employeeNode, employeeAliceParty)
        deleteSimpleStateForAccount()
    }
}
