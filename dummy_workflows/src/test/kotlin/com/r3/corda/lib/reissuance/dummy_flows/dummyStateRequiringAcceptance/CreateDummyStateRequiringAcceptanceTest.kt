package com.r3.corda.lib.reissuance.dummy_flows.dummyStateRequiringAcceptance

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasSize
import com.r3.corda.lib.reissuance.dummy_flows.AbstractFlowTest
import com.r3.corda.lib.reissuance.dummy_states.DummyStateRequiringAcceptance
import org.hamcrest.Matchers.`is`
import org.junit.Test

class CreateDummyStateRequiringAcceptanceTest: AbstractFlowTest() {

    @Test
    fun `Create DummyStateRequiringAcceptance`() {
        initialiseParties()
        createDummyStateRequiringAcceptance(aliceParty)

        val dummyStatesRequiringAcceptance = getStateAndRefs<DummyStateRequiringAcceptance>(aliceNode)
        assertThat(dummyStatesRequiringAcceptance, hasSize(1))
        val dummyStateRequiringAcceptance = dummyStatesRequiringAcceptance[0].state.data
        assertThat(dummyStateRequiringAcceptance.acceptor, `is`(acceptorParty))
        assertThat(dummyStateRequiringAcceptance.issuer, `is`(issuerParty))
        assertThat(dummyStateRequiringAcceptance.owner, `is`(aliceParty))
    }

}
