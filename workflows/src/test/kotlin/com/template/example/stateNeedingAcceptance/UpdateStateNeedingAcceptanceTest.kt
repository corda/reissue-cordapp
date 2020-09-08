package com.template.example.stateNeedingAcceptance

import com.template.AbstractFlowTest
import org.junit.Test

class UpdateStateNeedingAcceptanceTest: AbstractFlowTest() {

    @Test
    fun `Update state needing acceptance`() {
        initialiseParties()
        createStateNeedingAcceptance(aliceParty)
        updateStateNeedingAcceptance(aliceNode, bobParty)
    }

    @Test
    fun `Update state needing acceptance many times`() {
        initialiseParties()
        createStateNeedingAcceptance(aliceParty)
        updateStateNeedingAcceptance(aliceNode, bobParty)
        updateStateNeedingAcceptance(bobNode, charlieParty)
        updateStateNeedingAcceptance(charlieNode, debbieParty)
        updateStateNeedingAcceptance(debbieNode, charlieParty)
        updateStateNeedingAcceptance(charlieNode, bobParty)
        updateStateNeedingAcceptance(bobNode, aliceParty)
    }
}
