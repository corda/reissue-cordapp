package com.r3.corda.lib.reissuance.schemas

import net.corda.core.contracts.StateRef
import net.corda.core.schemas.MappedSchema
import java.io.Serializable
import javax.persistence.*


object PersistentReissuedStateSchema

object PersistentReissuedStateSchemaV1 : MappedSchema(
    schemaFamily = PersistentReissuedStateSchema.javaClass,
    version = 1,
    mappedTypes = listOf(PersistentReissuedState::class.java)
)

enum class ReissuanceDirection {
    SENT,
    RECEIVED
}

@Embeddable
data class PersistentReissuedStateId(
    @Column(name = "state_ref_hash", nullable = false)
    var stateRefHash: String,

    @Column(name = "state_ref_index", nullable = false)
    var stateRefIndex: Int,

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false)
    var direction: ReissuanceDirection
) : Serializable

@Entity(name = "PersistentReissuedState")
@Table(name = "reissued_state")
data class PersistentReissuedState(

    @EmbeddedId
    var txhashAndIndex: PersistentReissuedStateId

) : Serializable {
    constructor(stateRef: StateRef, direction: ReissuanceDirection) :
            this(
                PersistentReissuedStateId(
                    stateRef.txhash.toHexString(),
                    stateRef.index,
                    direction
            )
    )
}