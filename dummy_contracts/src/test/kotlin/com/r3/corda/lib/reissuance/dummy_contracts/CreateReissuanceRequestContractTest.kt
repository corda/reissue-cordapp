package com.r3.corda.lib.reissuance.dummy_contracts

import com.r3.corda.lib.reissuance.contracts.ReissuanceRequestContract
import net.corda.testing.node.ledger
import org.junit.Test

class CreateReissuanceRequestContractTest: AbstractContractTest() {

    @Test
    fun `Re-issuance request is created`() {
        aliceNode.services.ledger(notary = notaryParty) {
            transaction {
                output(
                    ReissuanceRequestContract.contractId, createSimpleDummyStateReissuanceRequest(
                    listOf(createDummyRef())))
                command(listOf(aliceParty.owningKey), ReissuanceRequestContract.Commands.Create())
                verifies()
            }
        }
    }

    @Test
    fun `Re-issuance request is created for many states`() {
        aliceNode.services.ledger(notary = notaryParty) {
            transaction {
                output(ReissuanceRequestContract.contractId, createTokensReissuanceRequest(
                    listOf(createDummyRef(), createDummyRef())))
                command(listOf(aliceParty.owningKey), ReissuanceRequestContract.Commands.Create())
                verifies()
            }
        }
    }

    @Test
    fun `Re-issuance request can't be created when state reference list is empty`() {
        aliceNode.services.ledger(notary = notaryParty) {
            transaction {
                output(ReissuanceRequestContract.contractId, createSimpleDummyStateReissuanceRequest(listOf()))
                command(listOf(aliceParty.owningKey), ReissuanceRequestContract.Commands.Create())
                fails()
            }
        }
    }

    @Test
    fun `Re-issuance request can't be created when command is not signed by requester`() {
        aliceNode.services.ledger(notary = notaryParty) {
            transaction {
                output(ReissuanceRequestContract.contractId, createSimpleDummyStateReissuanceRequest(
                    listOf(createDummyRef())))
                command(listOf(issuerParty.owningKey), ReissuanceRequestContract.Commands.Create())
                fails()
            }
        }
    }

}
