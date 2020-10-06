package com.r3.corda.lib.reissuance.states.example

import com.r3.corda.lib.reissuance.contracts.example.DummyStateRequiringAcceptanceContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

@BelongsToContract(DummyStateRequiringAcceptanceContract::class)
data class DummyStateRequiringAcceptance(
    var owner: Party,
    val issuer: Party,
    val acceptor: Party
): ContractState {
    override val participants: List<AbstractParty>
        get() = listOf(owner, issuer, acceptor)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DummyStateRequiringAcceptance

        if (owner != other.owner) return false
        if (issuer != other.issuer) return false
        if (acceptor != other.acceptor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = owner.hashCode()
        result = 31 * result + issuer.hashCode()
        result = 31 * result + acceptor.hashCode()
        return result
    }


}
