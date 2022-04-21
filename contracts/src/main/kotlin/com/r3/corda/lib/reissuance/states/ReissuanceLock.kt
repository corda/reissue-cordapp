package com.r3.corda.lib.reissuance.states

import com.r3.corda.lib.reissuance.contracts.ReissuanceLockContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.TimeWindow
import net.corda.core.crypto.CompositeKey
import net.corda.core.crypto.SignableData
import net.corda.core.identity.AbstractParty
import net.corda.core.serialization.CordaSerializable
import java.security.PublicKey

@BelongsToContract(ReissuanceLockContract::class)
data class ReissuanceLock(
    val issuer: AbstractParty,
    val requester: AbstractParty,
    val txHash: SignableData,
    val status: ReissuanceLockStatus = ReissuanceLockStatus.ACTIVE,
    val timeWindow: TimeWindow,
    override val participants: List<AbstractParty> = listOf(issuer, requester)
): ContractState {

    @CordaSerializable
    enum class ReissuanceLockStatus {
        ACTIVE,
        INACTIVE
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ReissuanceLock
        if (txHash != other.txHash) return false
        if (issuer != other.issuer) return false
        if (requester != other.requester) return false
        if (timeWindow != other.timeWindow) return false
        if (status != other.status) return false
        if (participants != other.participants) return false
        return true
    }

    override fun hashCode(): Int {
        var result = issuer.hashCode()
        result = 31 * result + requester.hashCode()
        result = 31 * result + txHash.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + timeWindow.hashCode()
        result = 31 * result + participants.hashCode()
        return result
    }

    fun getCompositeKey() : PublicKey {
        return CompositeKey.Builder()
            .addKeys(participants.map { it.owningKey })
            .build(1)
    }
}