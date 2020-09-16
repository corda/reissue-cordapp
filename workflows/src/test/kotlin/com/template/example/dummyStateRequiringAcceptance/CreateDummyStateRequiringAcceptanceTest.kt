package com.template.example.dummyStateRequiringAcceptance

import com.template.AbstractFlowTest
import org.junit.Test

class CreateDummyStateRequiringAcceptanceTest: AbstractFlowTest() {

    @Test
    fun `Create DummyStateRequiringAcceptance`() {
        initialiseParties()
        createDummyStateRequiringAcceptance(aliceParty)
    }

}
