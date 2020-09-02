package com.template.example.simpleState

import com.template.AbstractFlowTest
import org.junit.Test

class DeleteSimpleStateTest: AbstractFlowTest() {

    @Test
    fun `Delete simple state`() {
        createSimpleState(aliceParty)
        deleteSimpleState(aliceNode)
    }
}