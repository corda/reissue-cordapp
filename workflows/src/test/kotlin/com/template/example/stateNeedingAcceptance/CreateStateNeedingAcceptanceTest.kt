package com.template.example.stateNeedingAcceptance

import com.template.AbstractFlowTest
import com.template.flows.example.TmpFlow
import com.template.flows.example.stateNeedingAcceptance.CreateStateNeedingAcceptance
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.getOrThrow
import net.corda.testing.node.internal.startFlow
import org.junit.Test

class CreateStateNeedingAcceptanceTest: AbstractFlowTest() {

    @Test
    fun `Create state needing acceptance`() {
        createStateNeedingAcceptance(aliceParty)
    }

}
