package com.r3.corda.lib.reissuance.dummy_flows.dummy.responders

import com.r3.corda.lib.reissuance.dummy_states.DummyStateRequiringAcceptance
import com.r3.corda.lib.reissuance.dummy_states.DummyStateRequiringAllParticipantsSignatures
import com.r3.corda.lib.reissuance.dummy_states.DummyStateWithInvalidEqualsMethod
import com.r3.corda.lib.reissuance.dummy_states.SimpleDummyState
import com.r3.corda.lib.reissuance.flows.UnlockReissuedStates
import com.r3.corda.lib.reissuance.flows.UnlockReissuedStatesResponder
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import net.corda.core.contracts.requireThat
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.transactions.SignedTransaction

@InitiatedBy(UnlockReissuedStates::class)
class DummyUnlockReissuedStatesResponder(
    otherSession: FlowSession
) : UnlockReissuedStatesResponder(otherSession) {
    override fun checkConstraints(stx: SignedTransaction) {
        requireThat {
            "Reissued states are of type DummyStateRequiringAcceptance or " +
                "DummyStateRequiringAllParticipantsSignatures" using (
                otherOutputs[0].data::class.java == DummyStateRequiringAcceptance::class.java ||
                    otherOutputs[0].data::class.java == DummyStateRequiringAllParticipantsSignatures::class.java ||
                    otherOutputs[0].data::class.java == SimpleDummyState::class.java ||
                    otherOutputs[0].data::class.java == FungibleToken::class.java ||
                    otherOutputs[0].data::class.java == DummyStateWithInvalidEqualsMethod::class.java
                )
        }
    }}