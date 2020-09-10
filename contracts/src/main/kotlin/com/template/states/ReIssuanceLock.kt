package com.template.states

import com.template.contracts.ReIssuanceLockContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.AbstractParty

@BelongsToContract(ReIssuanceLockContract::class)
class ReIssuanceLock<T>(
    val issuer: AbstractParty,
    val requester: AbstractParty,
    val lockedStates: List<StateAndRef<T>> // we need state as well to get participants
): ContractState where T: ContractState {

    override val participants: List<AbstractParty>
        // participants are the same for every locked state (contract requirement)
        get() = lockedStates[0].state.data.participants

}
