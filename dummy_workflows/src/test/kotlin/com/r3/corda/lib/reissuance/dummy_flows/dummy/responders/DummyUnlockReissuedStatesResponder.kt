package com.r3.corda.lib.reissuance.dummy_flows.dummy.responders

import com.r3.corda.lib.reissuance.flows.UnlockReissuedStates
import com.r3.corda.lib.reissuance.flows.UnlockReissuedStatesResponder
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.transactions.SignedTransaction

@InitiatedBy(UnlockReissuedStates::class)
class DummyUnlockReissuedStatesResponder(
    otherSession: FlowSession
) : UnlockReissuedStatesResponder(otherSession) {
    override fun checkSignedTransaction(stx: SignedTransaction) {}
}