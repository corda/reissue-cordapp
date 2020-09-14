package com.template.states.example

import com.template.contracts.example.SimpleDummyStateContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty

@BelongsToContract(SimpleDummyStateContract::class)
class SimpleDummyState(
    val owner: AbstractParty,
    val quantity: Int = 5
): ContractState {
    override val participants: List<AbstractParty>
        get() = listOf(owner)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SimpleDummyState

        if (owner != other.owner) return false
        if (quantity != other.quantity) return false

        return true
    }

    override fun hashCode(): Int {
        var result = owner.hashCode()
        result = 31 * result + quantity
        return result
    }

}
