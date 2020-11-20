package com.r3.corda.lib.reissuance.dummy_flows.shell.dummyStateRequiringAcceptance

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.reissuance.dummy_flows.dummy.dummyStateRequiringAcceptance.DeleteDummyStateRequiringAcceptance
import com.r3.corda.lib.reissuance.dummy_states.DummyStateRequiringAcceptance
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria

@StartableByRPC
class DeleteDummyStateRequiringAcceptanceByRef(
    private val originalStateRefString: String
): FlowLogic<SecureHash>() {
    @Suspendable
    override fun call(): SecureHash {
        val originalStateRef = parseStateReference(originalStateRefString)
        val criteria = QueryCriteria.VaultQueryCriteria(stateRefs = listOf(originalStateRef))
        val originalStateAndRefs: List<StateAndRef<DummyStateRequiringAcceptance>> =
            serviceHub.vaultService.queryBy<DummyStateRequiringAcceptance>(criteria).states
        val originalStateAndRef = originalStateAndRefs[0]
        return subFlow(
            DeleteDummyStateRequiringAcceptance(originalStateAndRef)
        )
    }

    fun parseStateReference(stateRefStr: String): StateRef {
        val (secureHashStr, indexStr) = stateRefStr.dropLast(1).split("(")
        return StateRef(SecureHash.parse(secureHashStr), Integer.parseInt(indexStr))
    }

}
