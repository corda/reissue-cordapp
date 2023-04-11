package com.r3.corda.lib.reissuance.states

import net.corda.core.contracts.ContractState

/**
 * The ReissuableState interface is an optional interface that states can implement if they
 * need to change any of their properties during reissuance.
 *
 * Note: if reissued states are identical to the states being destroyed, then this interface
 * is not required.
 *
 * For example, a token that has a counter which increments each time it is used, but needs
 * to be reset upon reissuance.
 *
 * The methods that this interface supports are intended to:
 * - Allow reissuance to create a new state to issue from an existing state [createReissuance]
 * - Allow reissuance to test that a reissued state is equal to an existing state for the
 *   purposes of reissuance [isEqualForReissuance].
 *
 */
interface ReissuableState<T : ContractState> {

    /**
     * Create a reissuable version of a state. This allows the developer to adjust fields which
     * will not be the same before and after reissuance.
     */
    fun createReissuance() : T

    /**
     * Compare to another state of the same type, to evaluate whether for reissuance purposes
     * the state is the same. This allows a developer to ignore fields which they do not expect
     * to be the same before and after reissuance.
     *
     * @param otherState
     */
    fun isEqualForReissuance(otherState : T) : Boolean
}