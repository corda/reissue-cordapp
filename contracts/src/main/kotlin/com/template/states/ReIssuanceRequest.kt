package com.template.states

import com.template.contracts.ReIssuanceRequestContract
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable

@BelongsToContract(ReIssuanceRequestContract::class)
@CordaSerializable
class ReIssuanceRequest<T>(
    val issuer: AbstractParty,
    val requester: AbstractParty,
    val statesToReIssue: List<StateAndRef<T>>,
    val issuanceCommand: CommandData,
    val issuanceSigners: List<AbstractParty>
): ContractState where T: ContractState {

    override val participants: List<AbstractParty>
        get() = listOf(issuer, requester)

}
