package com.r3.corda.lib.reissuance.services

import com.r3.corda.lib.reissuance.schemas.PersistentReissuedState
import com.r3.corda.lib.reissuance.schemas.ReissuanceDirection
import net.corda.core.contracts.StateRef
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken

@CordaService
class ReissuedStatesService(private val appServiceHub : AppServiceHub) : SingletonSerializeAsToken() {

    fun hasStateRef(stateRef: StateRef, direction: ReissuanceDirection): Boolean {
        val result = appServiceHub.withEntityManager {
            find(PersistentReissuedState::class.java, PersistentReissuedState(stateRef, direction).txhashAndIndex)
        }

        return result != null
    }

    fun storeStateRef(stateRef: StateRef, direction: ReissuanceDirection) {
        val entity = PersistentReissuedState(stateRef, direction)

        appServiceHub.withEntityManager {
            persist(entity)
        }
    }
}
