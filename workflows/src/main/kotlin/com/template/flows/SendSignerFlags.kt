package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.*

class SendSignerFlags(
    private val signersSessions: List<FlowSession>,
    private val otherParticipantsSessions: List<FlowSession>
): FlowLogic<Unit>() {

    @Suspendable
    override fun call() {
        signersSessions.forEach {
            it.send(true)
        }
        otherParticipantsSessions.forEach {
            it.send(false)
        }
    }

}
