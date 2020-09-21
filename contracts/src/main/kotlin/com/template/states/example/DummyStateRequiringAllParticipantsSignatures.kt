package com.template.states.example

import com.template.contracts.example.DummyStateRequiringAllParticipantsSignaturesContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

@BelongsToContract(DummyStateRequiringAllParticipantsSignaturesContract::class)
data class DummyStateRequiringAllParticipantsSignatures(
    var owner: Party,
    val issuer: Party,
    val other: Party
): ContractState {
    override val participants: List<AbstractParty>
        get() = listOf(owner, issuer, other)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DummyStateRequiringAllParticipantsSignatures

        if (owner != other.owner) return false
        if (issuer != other.issuer) return false
        if (this.other != other.other) return false

        return true
    }

    override fun hashCode(): Int {
        var result = owner.hashCode()
        result = 31 * result + issuer.hashCode()
        result = 31 * result + other.hashCode()
        return result
    }


}
