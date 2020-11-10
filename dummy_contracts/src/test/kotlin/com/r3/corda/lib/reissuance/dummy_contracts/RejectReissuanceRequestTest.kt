package com.r3.corda.lib.reissuance.dummy_contracts

import com.r3.corda.lib.reissuance.contracts.ReissuanceLockContract
import com.r3.corda.lib.reissuance.contracts.ReissuanceRequestContract
import com.r3.corda.lib.reissuance.dummy_contracts.SimpleDummyStateContract.*
import net.corda.testing.node.ledger
import org.junit.Test

class RejectReissuanceRequestTest: AbstractContractTest() {

    @Test
    fun `Re-issuance request is rejected`() {
        val dummyReissuanceRequest = createSimpleDummyStateReissuanceRequest(listOf(createDummyRef()))
        issuerNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReissuanceRequestContract.contractId, dummyReissuanceRequest)
            }

            transaction {
                input(ReissuanceRequestContract.contractId, dummyReissuanceRequest)
                command(listOf(issuerParty.owningKey), ReissuanceRequestContract.Commands.Reject())
                verifies()
            }
        }
    }

    @Test
    fun `Re-issuance request of many states is rejected`() {
        val dummyReissuanceRequest = createTokensReissuanceRequest(listOf(createDummyRef(), createDummyRef()))
        issuerNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReissuanceRequestContract.contractId, dummyReissuanceRequest)
            }
            transaction {
                input(ReissuanceRequestContract.contractId, dummyReissuanceRequest)
                command(listOf(issuerParty.owningKey), ReissuanceRequestContract.Commands.Reject())
                verifies()
            }
        }
    }

    @Test
    fun `Re-issuance rejection can't produce re-issued state nor the re-issuance lock`() {
        val dummyReissuanceRequest = createSimpleDummyStateReissuanceRequest(listOf(createDummyRef()))
        issuerNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReissuanceRequestContract.contractId, dummyReissuanceRequest)
            }

            transaction {
                input(ReissuanceRequestContract.contractId, dummyReissuanceRequest)
                output(ReissuanceLockContract.contractId, reissuanceLockLabel,
                    contractState=createDummyReissuanceLock(listOf(createSimpleDummyStateAndRef()),
                        listOf(issuerParty)), encumbrance = 1)
                output(SimpleDummyStateContract.contractId, reissuedStateLabel,
                    contractState=createSimpleDummyState(), encumbrance = 0)
                command(listOf(issuerParty.owningKey), ReissuanceRequestContract.Commands.Reject())
                command(listOf(issuerParty.owningKey), ReissuanceLockContract.Commands.Create())
                command(listOf(issuerParty.owningKey), Commands.Create())
                fails()
            }
        }
    }


}
