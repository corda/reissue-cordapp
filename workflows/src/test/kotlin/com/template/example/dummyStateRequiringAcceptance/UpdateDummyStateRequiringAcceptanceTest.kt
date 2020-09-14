package com.template.example.dummyStateRequiringAcceptance

import com.template.AbstractFlowTest
import org.junit.Test

class UpdateDummyStateRequiringAcceptanceTest: AbstractFlowTest() {

    @Test
    fun `Update DummyStateRequiringAcceptance`() {
        initialiseParties()
        createDummyStateRequiringAcceptance(aliceParty)
        updateDummyStateRequiringAcceptance(aliceNode, bobParty)
    }

    @Test
    fun `Update DummyStateRequiringAcceptance many times`() {
        initialiseParties()
        createDummyStateRequiringAcceptance(aliceParty)
        updateDummyStateRequiringAcceptance(aliceNode, bobParty)
        updateDummyStateRequiringAcceptance(bobNode, charlieParty)
        updateDummyStateRequiringAcceptance(charlieNode, debbieParty)
        updateDummyStateRequiringAcceptance(debbieNode, charlieParty)
        updateDummyStateRequiringAcceptance(charlieNode, bobParty)
        updateDummyStateRequiringAcceptance(bobNode, aliceParty)
    }
}
