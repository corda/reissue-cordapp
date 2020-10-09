package com.r3.corda.lib.reissuance.dummy_contracts

import com.r3.corda.lib.reissuance.contracts.ReIssuanceRequestContract
import net.corda.testing.node.ledger
import org.junit.Test

class CreateReIssuanceRequestContractTest: AbstractContractTest() {

    @Test
    fun `Re-issuance request is created`() {
        aliceNode.services.ledger(notary = notaryParty) {
            transaction {
                output(ReIssuanceRequestContract.contractId, createDummySimpleStateReIssuanceRequest(listOf(createDummyRef())))
                command(listOf(aliceParty.owningKey), ReIssuanceRequestContract.Commands.Create())
                verifies()
            }
        }
    }

    @Test
    fun `Re-issuance request is created for many states`() {
        aliceNode.services.ledger(notary = notaryParty) {
            transaction {
                output(ReIssuanceRequestContract.contractId, createTokensReIssuanceRequest(
                    listOf(createDummyRef(), createDummyRef())))
                command(listOf(aliceParty.owningKey), ReIssuanceRequestContract.Commands.Create())
                verifies()
            }
        }
    }

    @Test
    fun `Re-issuance request can't be created when state reference list is empty`() {
        aliceNode.services.ledger(notary = notaryParty) {
            transaction {
                output(ReIssuanceRequestContract.contractId, createDummySimpleStateReIssuanceRequest(listOf()))
                command(listOf(aliceParty.owningKey), ReIssuanceRequestContract.Commands.Create())
                fails()
            }
        }
    }

    @Test
    fun `Re-issuance request can't be created when command is not signed by requester`() {
        aliceNode.services.ledger(notary = notaryParty) {
            transaction {
                output(ReIssuanceRequestContract.contractId, createDummySimpleStateReIssuanceRequest(listOf(createDummyRef())))
                command(listOf(issuerParty.owningKey), ReIssuanceRequestContract.Commands.Create())
                fails()
            }
        }
    }

}
