package com.r3.corda.lib.reissuance.states.example

import com.r3.corda.lib.reissuance.contracts.example.SimpleDummyStateContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty

@BelongsToContract(SimpleDummyStateContract::class)
data class SimpleDummyState(
    val owner: AbstractParty
): ContractState {
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

}
