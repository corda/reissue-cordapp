package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty

class GenerateRequiredFlowSessions(
    private val parties: List<AbstractParty>
): FlowLogic<List<FlowSession>>() {

    @Suspendable
    override fun call(): List<FlowSession> {
        return parties
            .map { serviceHub.identityService.partyFromKey(it.owningKey)!! } // get host
            .filter { it != ourIdentity }
            .distinct()
            .map { initiateFlow(it) }
    }

}
