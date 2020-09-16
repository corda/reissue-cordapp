package com.template.example.dummyStateRequiringAcceptance

import com.template.AbstractFlowTest
import org.junit.Test

class DeleteDummyStateRequiringAcceptanceTest: AbstractFlowTest() {

    @Test
    fun `Delete DummyStateRequiringAcceptance`() {
        initialiseParties()
        createDummyStateRequiringAcceptance(aliceParty)
        deleteDummyStateRequiringAcceptance(aliceNode)
    }

}
