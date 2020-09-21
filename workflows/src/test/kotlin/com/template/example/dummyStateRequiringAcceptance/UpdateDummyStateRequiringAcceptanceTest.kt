package com.template.example.dummyStateRequiringAcceptance

import com.template.AbstractFlowTest
import com.template.states.example.DummyStateRequiringAcceptance
import org.hamcrest.MatcherAssert.*
import org.hamcrest.Matchers.*
import org.junit.Test

class UpdateDummyStateRequiringAcceptanceTest: AbstractFlowTest() {

    @Test
    fun `Update DummyStateRequiringAcceptance`() {
        initialiseParties()
        createDummyStateRequiringAcceptance(aliceParty)
        updateDummyStateRequiringAcceptance(aliceNode, bobParty)

        val dummyStatesRequiringAcceptance = getStateAndRefs<DummyStateRequiringAcceptance>(bobNode)
        assertThat(dummyStatesRequiringAcceptance, hasSize(1))
        val dummyStateRequiringAcceptance = dummyStatesRequiringAcceptance[0].state.data
        assertThat(dummyStateRequiringAcceptance.acceptor, `is`(acceptorParty))
        assertThat(dummyStateRequiringAcceptance.issuer, `is`(issuerParty))
        assertThat(dummyStateRequiringAcceptance.owner, `is`(bobParty))
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

        val dummyStatesRequiringAcceptance = getStateAndRefs<DummyStateRequiringAcceptance>(aliceNode)
        assertThat(dummyStatesRequiringAcceptance, hasSize(1))
        val dummyStateRequiringAcceptance = dummyStatesRequiringAcceptance[0].state.data
        assertThat(dummyStateRequiringAcceptance.acceptor, `is`(acceptorParty))
        assertThat(dummyStateRequiringAcceptance.issuer, `is`(issuerParty))
        assertThat(dummyStateRequiringAcceptance.owner, `is`(aliceParty))
    }
}
