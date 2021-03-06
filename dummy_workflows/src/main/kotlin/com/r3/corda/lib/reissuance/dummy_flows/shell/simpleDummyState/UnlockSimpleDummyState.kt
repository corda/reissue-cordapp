package com.r3.corda.lib.reissuance.dummy_flows.shell.simpleDummyState

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.reissuance.dummy_contracts.SimpleDummyStateContract
import com.r3.corda.lib.reissuance.dummy_states.SimpleDummyState
import com.r3.corda.lib.reissuance.flows.UnlockReissuedStates
import com.r3.corda.lib.reissuance.states.ReissuanceLock
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria

@StartableByRPC
class UnlockSimpleDummyState(
    private val reissuedStatesRefStrings: List<String>,
    private val reissuanceLockRefString: String,
    private val deletedStateTransactionHashes: List<SecureHash>
): FlowLogic<SecureHash>() {

    @Suspendable
    override fun call(): SecureHash {
        val reissuanceLockRef = parseStateReference(reissuanceLockRefString)
        val reissuanceLockStateAndRef = serviceHub.vaultService.queryBy<ReissuanceLock<SimpleDummyState>>(
            criteria= QueryCriteria.VaultQueryCriteria(stateRefs = listOf(reissuanceLockRef))
        ).states[0]

        val reissuedStatesRefs = reissuedStatesRefStrings.map { parseStateReference(it) }
        val reissuedStatesStateAndRefs = serviceHub.vaultService.queryBy<SimpleDummyState>(
            criteria= QueryCriteria.VaultQueryCriteria(stateRefs = reissuedStatesRefs)
        ).states

        return subFlow(UnlockReissuedStates(
            reissuedStatesStateAndRefs,
            reissuanceLockStateAndRef,
            deletedStateTransactionHashes,
            SimpleDummyStateContract.Commands.Update()
        ))
    }

    fun parseStateReference(stateRefStr: String): StateRef {
        val (secureHashStr, indexStr) = stateRefStr.dropLast(1).split("(")
        return StateRef(SecureHash.parse(secureHashStr), Integer.parseInt(indexStr))
    }

}
