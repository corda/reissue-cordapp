package com.r3.corda.lib.reissuance.states

import com.r3.corda.lib.reissuance.contracts.ReissuanceLockContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.AbstractParty
import net.corda.core.serialization.CordaSerializable

@BelongsToContract(ReissuanceLockContract::class)
data class ReissuanceLock<T>(
    val issuer: AbstractParty,
    val requester: AbstractParty,
    val originalStates: List<StateAndRef<T>>,
    val extraAssetExitCommandSigners: List<AbstractParty>,
    val status: ReissuanceLockStatus = ReissuanceLockStatus.ACTIVE
    ): ContractState where T: ContractState {

    @CordaSerializable
    enum class ReissuanceLockStatus {
        ACTIVE,
        INACTIVE
    }

    override val participants: List<AbstractParty>
        get() = listOf(issuer, requester)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ReissuanceLock<*>

        if (issuer != other.issuer) return false
        if (requester != other.requester) return false
        if (originalStates != other.originalStates) return false
        if (extraAssetExitCommandSigners != other.extraAssetExitCommandSigners) return false
        if (status != other.status) return false

        return true
    }

    override fun hashCode(): Int {
        var result = issuer.hashCode()
        result = 31 * result + requester.hashCode()
        result = 31 * result + originalStates.hashCode()
        result = 31 * result + extraAssetExitCommandSigners.hashCode()
        result = 31 * result + status.hashCode()
        return result
    }

}
