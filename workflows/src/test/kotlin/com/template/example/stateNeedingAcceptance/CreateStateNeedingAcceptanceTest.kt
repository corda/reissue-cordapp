package com.template.example.stateNeedingAcceptance

import com.template.AbstractFlowTest
import org.junit.Test

class CreateStateNeedingAcceptanceTest: AbstractFlowTest() {

    @Test
    fun `Create state needing acceptance`() {
        initialiseParties()
        createStateNeedingAcceptance(aliceParty)
    }

}
