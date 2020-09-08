package com.template.example.stateNeedingAcceptance

import com.template.AbstractFlowTest
import org.junit.Test

class DeleteStateNeedingAcceptanceTest: AbstractFlowTest() {

    @Test
    fun `Delete state needing acceptance`() {
        initialiseParties()
        createStateNeedingAcceptance(aliceParty)
        deleteStateNeedingAcceptance(aliceNode)
    }

}
