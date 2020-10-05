package com.template.states.example

import com.template.contracts.example.DummyStateWithInvalidEqualsMethodContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

@BelongsToContract(DummyStateWithInvalidEqualsMethodContract::class)
data class DummyStateWithInvalidEqualsMethod(
    var owner: Party,
    val issuer: Party,
    val quantity: Int
): ContractState {
    override val participants: List<AbstractParty>
        get() = listOf(owner)

    override fun equals(other: Any?): Boolean {
        return true
    }

    override fun hashCode(): Int {
        return 0
    }

}
