package com.r3.corda.lib.reissuance.dummy_flows.dummy.responders

import com.r3.corda.lib.reissuance.flows.ReissueStates
import com.r3.corda.lib.reissuance.flows.ReissueStatesResponder
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.transactions.SignedTransaction

@InitiatedBy(ReissueStates::class)
class DummyReissueStatesResponder(
    otherSession: FlowSession
) : ReissueStatesResponder(otherSession) {
    override fun checkSignedTransaction(stx: SignedTransaction) {}
}
