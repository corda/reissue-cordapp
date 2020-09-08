package com.template.states.example

import com.template.contracts.example.SimpleStateContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty

@BelongsToContract(SimpleStateContract::class)
class SimpleState(
    val owner: AbstractParty
): ContractState {
    override val participants: List<AbstractParty>
        get() = listOf(owner)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SimpleState

        if (owner != other.owner) return false

        return true
    }

    override fun hashCode(): Int {
        return owner.hashCode()
    }
}
