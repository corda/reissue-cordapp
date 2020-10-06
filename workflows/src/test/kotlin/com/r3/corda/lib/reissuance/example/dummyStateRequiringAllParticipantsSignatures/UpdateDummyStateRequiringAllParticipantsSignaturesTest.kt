package com.r3.corda.lib.reissuance.example.dummyStateRequiringAllParticipantsSignatures

import com.r3.corda.lib.reissuance.AbstractFlowTest
import com.r3.corda.lib.reissuance.states.example.DummyStateRequiringAllParticipantsSignatures
import org.hamcrest.MatcherAssert.*
import org.hamcrest.Matchers.*
import org.junit.Test

class UpdateDummyStateRequiringAllParticipantsSignaturesTest: AbstractFlowTest() {

    @Test
    fun `Update DummyStateRequiringAllParticipantsSignatures`() {
        initialiseParties()
        createDummyStateRequiringAllParticipantsSignatures(aliceParty)
        updateDummyStateRequiringAllParticipantsSignatures(aliceNode, bobParty)

        val dummyStatesRequiringAllParticipantsSignatures = getStateAndRefs<DummyStateRequiringAllParticipantsSignatures>(bobNode)
        assertThat(dummyStatesRequiringAllParticipantsSignatures, hasSize(1))
        val dummyStateRequiringAllParticipantsSignatures = dummyStatesRequiringAllParticipantsSignatures[0].state.data
        assertThat(dummyStateRequiringAllParticipantsSignatures.other, `is`(acceptorParty))
        assertThat(dummyStateRequiringAllParticipantsSignatures.issuer, `is`(issuerParty))
        assertThat(dummyStateRequiringAllParticipantsSignatures.owner, `is`(bobParty))
    }


    @Test
    fun `Update DummyStateRequiringAllParticipantsSignatures many times`() {
        initialiseParties()
        createDummyStateRequiringAllParticipantsSignatures(aliceParty)
        updateDummyStateRequiringAllParticipantsSignatures(aliceNode, bobParty)
        updateDummyStateRequiringAllParticipantsSignatures(bobNode, charlieParty)
        updateDummyStateRequiringAllParticipantsSignatures(charlieNode, debbieParty)
        updateDummyStateRequiringAllParticipantsSignatures(debbieNode, charlieParty)
        updateDummyStateRequiringAllParticipantsSignatures(charlieNode, bobParty)
        updateDummyStateRequiringAllParticipantsSignatures(bobNode, aliceParty)

        val dummyStatesRequiringAllParticipantsSignatures = getStateAndRefs<DummyStateRequiringAllParticipantsSignatures>(aliceNode)
        assertThat(dummyStatesRequiringAllParticipantsSignatures, hasSize(1))
        val dummyStateRequiringAllParticipantsSignatures = dummyStatesRequiringAllParticipantsSignatures[0].state.data
        assertThat(dummyStateRequiringAllParticipantsSignatures.other, `is`(acceptorParty))
        assertThat(dummyStateRequiringAllParticipantsSignatures.issuer, `is`(issuerParty))
        assertThat(dummyStateRequiringAllParticipantsSignatures.owner, `is`(aliceParty))
    }

}
