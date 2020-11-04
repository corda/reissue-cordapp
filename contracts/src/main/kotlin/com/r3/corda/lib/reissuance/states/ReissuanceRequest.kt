package com.r3.corda.lib.reissuance.states

import com.r3.corda.lib.reissuance.contracts.ReissuanceRequestContract
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.serialization.CordaSerializable

@BelongsToContract(ReissuanceRequestContract::class)
@CordaSerializable
data class ReissuanceRequest(
    val issuer: AbstractParty,
    val requester: AbstractParty,
    val stateRefsToReissue: List<StateRef>,
    val assetIssuanceCommand: CommandData,
    val assetIssuanceSigners: List<AbstractParty>
): ContractState {

    override val participants: List<AbstractParty>
        get() = listOf(issuer, requester)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ReissuanceRequest

        if (issuer != other.issuer) return false
        if (requester != other.requester) return false
        if (stateRefsToReissue != other.stateRefsToReissue) return false
        if (assetIssuanceCommand != other.assetIssuanceCommand) return false
        if (assetIssuanceSigners != other.assetIssuanceSigners) return false

        return true
    }

    override fun hashCode(): Int {
        var result = issuer.hashCode()
        result = 31 * result + requester.hashCode()
        result = 31 * result + stateRefsToReissue.hashCode()
        result = 31 * result + assetIssuanceCommand.hashCode()
        result = 31 * result + assetIssuanceSigners.hashCode()
        return result
    }

}
