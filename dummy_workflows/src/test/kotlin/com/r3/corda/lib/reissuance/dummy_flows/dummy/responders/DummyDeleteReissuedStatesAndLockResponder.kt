package com.r3.corda.lib.reissuance.dummy_flows.dummy.responders

import com.r3.corda.lib.reissuance.flows.DeleteReissuedStatesAndLock
import com.r3.corda.lib.reissuance.flows.DeleteReissuedStatesAndLockResponder
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.transactions.SignedTransaction

@InitiatedBy(DeleteReissuedStatesAndLock::class)
class DummyDeleteReissuedStatesAndLockResponder(
    otherSession: FlowSession
) : DeleteReissuedStatesAndLockResponder(otherSession) {
    override fun checkConstraints(stx: SignedTransaction) {}
}
