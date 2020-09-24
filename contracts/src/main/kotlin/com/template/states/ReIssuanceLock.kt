package com.template.states

import com.template.contracts.ReIssuanceLockContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.AbstractParty
import net.corda.core.serialization.CordaSerializable

@BelongsToContract(ReIssuanceLockContract::class)
data class ReIssuanceLock<T>(
    val issuer: AbstractParty,
    val requester: AbstractParty,
    val originalStates: List<StateAndRef<T>>,
    val status: ReIssuanceLockStatus = ReIssuanceLockStatus.ACTIVE,
    val issuerIsRequiredExitTransactionSigner: Boolean = true
    ): ContractState where T: ContractState {

    @CordaSerializable
    enum class ReIssuanceLockStatus {
        ACTIVE,
        INACTIVE
    }

    override val participants: List<AbstractParty>
        get() = listOf(issuer, requester)

}
