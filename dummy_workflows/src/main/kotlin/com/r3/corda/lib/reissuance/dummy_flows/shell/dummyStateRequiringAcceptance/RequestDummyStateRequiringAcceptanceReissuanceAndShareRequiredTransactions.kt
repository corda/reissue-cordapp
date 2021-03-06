package com.r3.corda.lib.reissuance.dummy_flows.shell.dummyStateRequiringAcceptance

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.reissuance.dummy_contracts.DummyStateRequiringAcceptanceContract
import com.r3.corda.lib.reissuance.dummy_states.DummyStateRequiringAcceptance
import com.r3.corda.lib.reissuance.flows.RequestReissuanceAndShareRequiredTransactions
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.AbstractParty

@StartableByRPC
class RequestDummyStateRequiringAcceptanceReissuanceAndShareRequiredTransactions(
    private val issuer: AbstractParty,
    private val stateRefStringsToReissue: List<String>,
    private val acceptor: AbstractParty
): FlowLogic<SecureHash>() {

    @Suspendable
    override fun call(): SecureHash {
        return subFlow(RequestReissuanceAndShareRequiredTransactions<DummyStateRequiringAcceptance>(
            issuer,
            stateRefStringsToReissue.map { parseStateReference(it) },
            DummyStateRequiringAcceptanceContract.Commands.Create(),
            listOf(acceptor)
        ))
    }

    fun parseStateReference(stateRefStr: String): StateRef {
        val (secureHashStr, indexStr) = stateRefStr.dropLast(1).split("(")
        return StateRef(SecureHash.parse(secureHashStr), Integer.parseInt(indexStr))
    }
}
