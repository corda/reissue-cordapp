package com.r3.corda.lib.reissuance.dummy_flows.shell

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.reissuance.dummy_flows.dummy.simpleDummyState.DeleteSimpleDummyState
import com.r3.corda.lib.reissuance.dummy_states.SimpleDummyState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria

@StartableByRPC
class DeleteSimpleDummyStateByRef(
    private val originalStateRefString: String,
    private val issuer: Party
): FlowLogic<SecureHash>() {
    @Suspendable
    override fun call(): SecureHash {
        val originalStateRef = parseStateReference(originalStateRefString)
        val criteria = QueryCriteria.VaultQueryCriteria(stateRefs = listOf(originalStateRef))
        val originalStateAndRefs: List<StateAndRef<SimpleDummyState>> = serviceHub.vaultService.queryBy<SimpleDummyState>(criteria).states
        val originalStateAndRef = originalStateAndRefs[0]
        return subFlow(
            DeleteSimpleDummyState(originalStateAndRef, issuer)
        )
    }

    fun parseStateReference(stateRefStr: String): StateRef {
        val (secureHashStr, indexStr) = stateRefStr.dropLast(1).split("(")
        return StateRef(SecureHash.parse(secureHashStr), Integer.parseInt(indexStr))
    }

}
