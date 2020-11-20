package com.r3.corda.lib.reissuance.dummy_flows.dummy.responders

import com.r3.corda.lib.reissuance.dummy_states.DummyStateRequiringAcceptance
import com.r3.corda.lib.reissuance.dummy_states.DummyStateRequiringAllParticipantsSignatures
import com.r3.corda.lib.reissuance.flows.ReissueStates
import com.r3.corda.lib.reissuance.flows.ReissueStatesResponder
import net.corda.core.contracts.requireThat
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.transactions.SignedTransaction

@InitiatedBy(ReissueStates::class)
class DummyReissueStatesResponder(
    otherSession: FlowSession
) : ReissueStatesResponder(otherSession) {

    override fun checkConstraints(stx: SignedTransaction) {
        // other states (SimpleDummyState, DummyStateWithInvalidEqualsMethod, FungibleToken) don't need responder to
        // sign reissuance transaction
        requireThat {
            "Reissued states are of type DummyStateRequiringAcceptance or " +
                "DummyStateRequiringAllParticipantsSignatures" using (
                    otherOutputs[0].data::class.java == DummyStateRequiringAcceptance::class.java ||
                    otherOutputs[0].data::class.java == DummyStateRequiringAllParticipantsSignatures::class.java
                )
        }
    }
}
