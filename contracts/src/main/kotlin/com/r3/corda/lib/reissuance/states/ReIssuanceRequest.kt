package com.r3.corda.lib.reissuance.states

import com.r3.corda.lib.reissuance.contracts.ReIssuanceRequestContract
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.serialization.CordaSerializable

@BelongsToContract(ReIssuanceRequestContract::class)
@CordaSerializable
data class ReIssuanceRequest(
    val issuer: AbstractParty,
    val requester: AbstractParty,
    val stateRefsToReIssue: List<StateRef>,
    val assetIssuanceCommand: CommandData,
    val assetIssuanceSigners: List<AbstractParty>
): ContractState {

    override val participants: List<AbstractParty>
        get() = listOf(issuer, requester)

}
