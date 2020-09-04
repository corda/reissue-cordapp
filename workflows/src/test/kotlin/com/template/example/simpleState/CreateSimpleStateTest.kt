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
    fun `Create simple state - accounts`() {
        initialiseForAccounts()
        createSimpleStateForAccount(employeeAliceParty)
    }
}