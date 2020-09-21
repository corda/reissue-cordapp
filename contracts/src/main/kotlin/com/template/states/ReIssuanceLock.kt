package com.template.states

import com.template.contracts.ReIssuanceLockContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.AbstractParty
import net.corda.core.serialization.CordaSerializable

@BelongsToContract(ReIssuanceLockContract::class)
data class ReIssuanceLock<T>(
    val issuer: AbstractParty,
    val requester: AbstractParty,
    val originalStates: List<StateAndRef<T>>, // we need state as well to get participants
    val reIssuesStatesStatus: ReIssuanceStatus = ReIssuanceStatus.RE_ISSUED
): ContractState where T: ContractState {

    @CordaSerializable
    enum class ReIssuanceStatus {
        RE_ISSUED,
        USED
    }

    override val participants: List<AbstractParty>
        // participants are the same for every locked state (contract requirement)
        get() = (originalStates[0].state.data.participants + listOf(issuer, requester)).distinct()

}
