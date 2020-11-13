package net.corda.samples.reissuance.candies.flows.wrappedReissuanceFlows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.reissuance.dummy_states.SimpleDummyState
import com.r3.corda.lib.reissuance.flows.ReissueStates
import com.r3.corda.lib.reissuance.states.ReissuanceRequest
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria

// Note: There is no need to generate a separate flow calling ReissueStates.
// ReissueStates can be used directly to re-issue states and generate corresponding re-issuance lock.
// ReissueCandyCoupons has been created to make it easier to use node shell.

@StartableByRPC
class ReissueSimpleDummyState(
    private val reissuanceRequestRefString: String
): FlowLogic<SecureHash>() {

    @Suspendable
    override fun call(): SecureHash {
        val rejectReissuanceRequestRef = parseStateReference(reissuanceRequestRefString)
        val rejectReissuanceRequestStateAndRef = serviceHub.vaultService.queryBy<ReissuanceRequest>(
            criteria= QueryCriteria.VaultQueryCriteria(stateRefs = listOf(rejectReissuanceRequestRef))
        ).states[0]
        return subFlow(ReissueStates<SimpleDummyState>(
            rejectReissuanceRequestStateAndRef, listOf()
        ))
    }

    fun parseStateReference(stateRefStr: String): StateRef {
        val (secureHashStr, indexStr) = stateRefStr.dropLast(1).split("(")
        return StateRef(SecureHash.parse(secureHashStr), Integer.parseInt(indexStr))
    }

}
