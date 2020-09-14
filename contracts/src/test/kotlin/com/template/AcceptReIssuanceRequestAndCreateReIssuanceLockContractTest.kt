package com.template

import com.template.contracts.ReIssuanceLockContract
import com.template.contracts.ReIssuanceRequestContract
import com.template.contracts.example.SimpleStateContract
import net.corda.testing.contracts.DummyContract
import net.corda.testing.node.ledger
import org.junit.Test

class AcceptReIssuanceRequestAndCreateReIssuanceLockContractTest: AbstractContractTest() {

    @Test
    fun `Re-issuance request is accepted and re-issuance lock is created`() {
        val dummyReIssuanceRequest = createDummyReIssuanceRequest()
        issuerNode.services.ledger {
                unverifiedTransaction {
                    output(ReIssuanceRequestContract.contractId, dummyReIssuanceRequest)
                }
                transaction {
                    input(ReIssuanceRequestContract.contractId, dummyReIssuanceRequest)
                    output(ReIssuanceLockContract.contractId, "re-issuance lock",
                        contractState=createDummyReIssuanceLock(), encumbrance = 1)
                    output(DummyContract.PROGRAM_ID, "re-issued state encumbered by re-issuance lock",
                        contractState=createDummyState(), encumbrance = 0)
                    command(listOf(issuerParty.owningKey), ReIssuanceRequestContract.Commands.Accept())
                    command(listOf(issuerParty.owningKey), ReIssuanceLockContract.Commands.Create())
                    command(listOf(issuerParty.owningKey), SimpleStateContract.Commands.Create())
                    verifies()
                }
            }
    }
}
