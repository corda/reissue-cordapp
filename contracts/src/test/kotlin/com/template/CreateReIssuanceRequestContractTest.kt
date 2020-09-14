package com.template

import com.template.contracts.ReIssuanceRequestContract
import net.corda.testing.node.ledger
import org.junit.Test

class CreateReIssuanceRequestContractTest: AbstractContractTest() {

    @Test
    fun `Re-issuance request is created`() {
        ledgerServices
            .ledger {
                transaction {
                    output(ReIssuanceRequestContract.contractId, createDummyReIssuanceRequest())
                    command(listOf(aliceParty.owningKey), ReIssuanceRequestContract.Commands.Create())
                    verifies()
                }
            }
    }

}
