package net.corda.samples.reissuance.candies.flows.wrappedReissuanceFlows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.reissuance.dummy_contracts.SimpleDummyStateContract
import com.r3.corda.lib.reissuance.dummy_states.SimpleDummyState
import com.r3.corda.lib.reissuance.flows.RequestReissuanceAndShareRequiredTransactions
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.AbstractParty

// Note: There is no need to generate a separate flow calling RequestReissuanceAndShareRequiredTransactions.
// RequestReissuanceAndShareRequiredTransactions can be used directly to request state re-issuance and share required
// transactions (proving that states to be re-issued are valid) with the issuer.
// RequestCandyCouponReissuanceAndShareRequiredTransactions has been created to make it easier to use node shell.

@StartableByRPC
class RequestSimpleDummyStateReissuanceAndShareRequiredTransactions(
    private val issuer: AbstractParty,
    private val stateRefStringsToReissue: List<String>
): FlowLogic<SecureHash>() {

    @Suspendable
    override fun call(): SecureHash {
        return subFlow(RequestReissuanceAndShareRequiredTransactions<SimpleDummyState>(
            issuer,
            stateRefStringsToReissue.map { parseStateReference(it) },
            SimpleDummyStateContract.Commands.Create()
        ))
    }

    fun parseStateReference(stateRefStr: String): StateRef {
        val (secureHashStr, indexStr) = stateRefStr.dropLast(1).split("(")
        return StateRef(SecureHash.parse(secureHashStr), Integer.parseInt(indexStr))
    }
}
