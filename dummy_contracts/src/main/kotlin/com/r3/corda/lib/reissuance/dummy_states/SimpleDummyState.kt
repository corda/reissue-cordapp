package com.r3.corda.lib.reissuance.dummy_states

import com.r3.corda.lib.reissuance.dummy_contracts.SimpleDummyStateContract
import com.r3.corda.lib.reissuance.states.ReissuableState
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty

@BelongsToContract(SimpleDummyStateContract::class)
data class SimpleDummyState(
    val owner: AbstractParty
): ContractState, ReissuableState<SimpleDummyState> {
    override val participants: List<AbstractParty>
        get() = listOf(owner)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SimpleDummyState

        if (owner != other.owner) return false

        return true
    }

    override fun hashCode(): Int {
        return owner.hashCode()
    }

    override fun createReissuance(): SimpleDummyState {
        return this.copy()
    }

    override fun isEqualForReissuance(otherState: SimpleDummyState): Boolean {
        return this == otherState
    }

}
