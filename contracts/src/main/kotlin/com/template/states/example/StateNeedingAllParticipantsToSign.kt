package com.template.states.example

import com.template.contracts.example.SimpleStateContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

@BelongsToContract(SimpleStateContract::class)
class StateNeedingAllParticipantsToSign(
    val owner: Party,
    val party1: Party,
    val party2: Party
): ContractState {
    override val participants: List<AbstractParty>
        get() = listOf(owner, party1, party2)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StateNeedingAllParticipantsToSign

        if (owner != other.owner) return false
        if (party1 != other.party1) return false
        if (party2 != other.party2) return false

        return true
    }

    override fun hashCode(): Int {
        var result = owner.hashCode()
        result = 31 * result + party1.hashCode()
        result = 31 * result + party2.hashCode()
        return result
    }


}
