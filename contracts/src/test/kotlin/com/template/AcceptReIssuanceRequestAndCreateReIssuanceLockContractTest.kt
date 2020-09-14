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
        val dummyReIssuanceRequest = createDummyReIssuanceRequest(listOf(createDummyRef()))
        issuerNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReIssuanceRequestContract.contractId, dummyReIssuanceRequest)
            }
            transaction {
                input(ReIssuanceRequestContract.contractId, dummyReIssuanceRequest)
                output(ReIssuanceLockContract.contractId, reIssuanceLockLabel,
                    contractState=createDummyReIssuanceLock(), encumbrance = 1)
                output(DummyContract.PROGRAM_ID, reIssuedStateLabel,
                    contractState=createDummyState(), encumbrance = 0)
                command(listOf(issuerParty.owningKey), ReIssuanceRequestContract.Commands.Accept())
                command(listOf(issuerParty.owningKey), ReIssuanceLockContract.Commands.Create())
                command(listOf(issuerParty.owningKey), SimpleStateContract.Commands.Create())
                verifies()
            }
        }
    }

    @Test
    fun `Re-issuance can't produce unencumbered states`() {
        val dummyReIssuanceRequest = createDummyReIssuanceRequest(listOf(createDummyRef()))
        issuerNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReIssuanceRequestContract.contractId, dummyReIssuanceRequest)
            }
            transaction {
                input(ReIssuanceRequestContract.contractId, dummyReIssuanceRequest)
                output(ReIssuanceLockContract.contractId, reIssuanceLockLabel,
                    contractState=createDummyReIssuanceLock())
                output(DummyContract.PROGRAM_ID, reIssuedStateLabel,
                    contractState=createDummyState())
                command(listOf(issuerParty.owningKey), ReIssuanceRequestContract.Commands.Accept())
                command(listOf(issuerParty.owningKey), ReIssuanceLockContract.Commands.Create())
                command(listOf(issuerParty.owningKey), SimpleStateContract.Commands.Create())
                fails()
            }
        }
    }

    @Test
    fun `States can't be re-issued without creating a re-issuance lock`() {
        val dummyReIssuanceRequest = createDummyReIssuanceRequest(listOf(createDummyRef()))
        issuerNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReIssuanceRequestContract.contractId, dummyReIssuanceRequest)
            }
            transaction {
                input(ReIssuanceRequestContract.contractId, dummyReIssuanceRequest)
                output(DummyContract.PROGRAM_ID, reIssuedStateLabel,
                    contractState=createDummyState(), encumbrance = 0)
                command(listOf(issuerParty.owningKey), ReIssuanceRequestContract.Commands.Accept())
                command(listOf(issuerParty.owningKey), ReIssuanceLockContract.Commands.Create())
                command(listOf(issuerParty.owningKey), SimpleStateContract.Commands.Create())
                fails()
            }
        }
    }

    @Test
    fun `Re-issuance can't happen without a request`() {
        val dummyReIssuanceRequest = createDummyReIssuanceRequest(listOf(createDummyRef()))
        issuerNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReIssuanceRequestContract.contractId, dummyReIssuanceRequest)
            }
            transaction {
                output(ReIssuanceLockContract.contractId, reIssuanceLockLabel,
                    contractState=createDummyReIssuanceLock(), encumbrance = 1)
                output(DummyContract.PROGRAM_ID, reIssuedStateLabel,
                    contractState=createDummyState(), encumbrance = 0)
                command(listOf(issuerParty.owningKey), ReIssuanceRequestContract.Commands.Accept())
                command(listOf(issuerParty.owningKey), ReIssuanceLockContract.Commands.Create())
                command(listOf(issuerParty.owningKey), SimpleStateContract.Commands.Create())
                fails()
            }
        }
    }
}
