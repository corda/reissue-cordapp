package com.r3.corda.lib.reissuance.dummy_contracts

import com.r3.corda.lib.reissuance.contracts.ReIssuanceLockContract
import com.r3.corda.lib.reissuance.contracts.ReIssuanceRequestContract
import com.r3.corda.lib.reissuance.dummy_contracts.SimpleDummyStateContract.*
import net.corda.testing.node.ledger
import org.junit.Test

class RejectReIssuanceRequestTest: AbstractContractTest() {

    @Test
    fun `Re-issuance request is rejected`() {
        val dummyReIssuanceRequest = createDummySimpleStateReIssuanceRequest(listOf(createDummyRef()))
        issuerNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReIssuanceRequestContract.contractId, dummyReIssuanceRequest)
            }

            transaction {
                input(ReIssuanceRequestContract.contractId, dummyReIssuanceRequest)
                command(listOf(issuerParty.owningKey), ReIssuanceRequestContract.Commands.Reject())
                verifies()
            }
        }
    }

    @Test
    fun `Re-issuance request of many states is rejected`() {
        val dummyReIssuanceRequest = createTokensReIssuanceRequest(listOf(createDummyRef(), createDummyRef()))
        issuerNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReIssuanceRequestContract.contractId, dummyReIssuanceRequest)
            }
            transaction {
                input(ReIssuanceRequestContract.contractId, dummyReIssuanceRequest)
                command(listOf(issuerParty.owningKey), ReIssuanceRequestContract.Commands.Reject())
                verifies()
            }
        }
    }

    @Test
    fun `Re-issuance rejection can't produce re-issued state nor the re-issuance lock`() {
        val dummyReIssuanceRequest = createDummySimpleStateReIssuanceRequest(listOf(createDummyRef()))
        issuerNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReIssuanceRequestContract.contractId, dummyReIssuanceRequest)
            }

            transaction {
                input(ReIssuanceRequestContract.contractId, dummyReIssuanceRequest)
                output(ReIssuanceLockContract.contractId, reIssuanceLockLabel,
                    contractState=createDummyReIssuanceLock(listOf(createSimpleDummyStateAndRef())), encumbrance = 1)
                output(SimpleDummyStateContract.contractId, reIssuedStateLabel,
                    contractState=createSimpleDummyState(), encumbrance = 0)
                command(listOf(issuerParty.owningKey), ReIssuanceRequestContract.Commands.Reject())
                command(listOf(issuerParty.owningKey), ReIssuanceLockContract.Commands.Create())
                command(listOf(issuerParty.owningKey), Commands.Create())
                fails()
            }
        }
    }


}
