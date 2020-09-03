package com.template.example.simpleState

import com.template.AbstractFlowTest
import org.junit.Test

class DeleteSimpleStateTest: AbstractFlowTest() {

    @Test
    fun `Delete simple state`() {
        createSimpleState(aliceParty)
        deleteSimpleState(aliceNode)
    }

    @Test
    fun `Delete simple state for account`() {
        createSimpleStateForAccount(employeeAliceParty)
        deleteSimpleStateForAccount()
    }
}
