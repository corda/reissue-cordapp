package com.r3.corda.lib.reissuance.dummy_contracts

import com.r3.corda.lib.reissuance.contracts.ReissuanceLockContract
import com.r3.corda.lib.reissuance.contracts.ReissuanceRequestContract
import com.r3.corda.lib.reissuance.states.ReissuanceLock
import com.r3.corda.lib.tokens.contracts.FungibleTokenContract
import com.r3.corda.lib.tokens.contracts.commands.IssueTokenCommand
import net.corda.testing.node.ledger
import org.junit.Test

class AcceptReissuanceRequestAndCreateReissuanceLockContractTest: AbstractContractTest() {

    @Test
    fun `State is successfully re-issued`() {
        val dummyReissuanceRequest = createDummySimpleStateReissuanceRequest(listOf(createDummyRef()))
        issuerNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReissuanceRequestContract.contractId, dummyReissuanceRequest)
            }

            transaction {
                input(ReissuanceRequestContract.contractId, dummyReissuanceRequest)
                output(ReissuanceLockContract.contractId, reissuanceLockLabel,
                    contractState=createDummyReissuanceLock(listOf(createSimpleDummyStateAndRef())), encumbrance = 1)
                output(SimpleDummyStateContract.contractId, reissuedStateLabel,
                    contractState=createSimpleDummyState(), encumbrance = 0)
                command(listOf(issuerParty.owningKey), ReissuanceRequestContract.Commands.Accept())
                command(listOf(issuerParty.owningKey), ReissuanceLockContract.Commands.Create())
                command(listOf(issuerParty.owningKey), SimpleDummyStateContract.Commands.Create())
                verifies()
            }
        }
    }

    @Test
    fun `Many states are successfully re-issued`() {
        val dummyReissuanceRequest = createTokensReissuanceRequest(listOf(createDummyRef(), createDummyRef()))
        issuerNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReissuanceRequestContract.contractId, dummyReissuanceRequest)
            }
            transaction {
                input(ReissuanceRequestContract.contractId, dummyReissuanceRequest)
                output(FungibleTokenContract.contractId, reissuedStateLabel(0),
                    contractState=createToken(), encumbrance = 1)
                output(FungibleTokenContract.contractId, reissuedStateLabel(1),
                    contractState=createToken(), encumbrance = 2)
                output(ReissuanceLockContract.contractId, reissuanceLockLabel,
                    contractState=createDummyReissuanceLock(listOf(createTokenStateAndRef(), createTokenStateAndRef())), encumbrance = 0)
                command(listOf(issuerParty.owningKey), ReissuanceRequestContract.Commands.Accept())
                command(listOf(issuerParty.owningKey), ReissuanceLockContract.Commands.Create())
                command(listOf(issuerParty.owningKey), IssueTokenCommand(issuedTokenType, listOf(0, 1)))
                verifies()
            }
        }
    }

    @Test
    fun `State can't be re-issued if lock status is INACTIVE`() {
        val dummyReissuanceRequest = createDummySimpleStateReissuanceRequest(listOf(createDummyRef()))
        issuerNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReissuanceRequestContract.contractId, dummyReissuanceRequest)
            }

            transaction {
                input(ReissuanceRequestContract.contractId, dummyReissuanceRequest)
                output(ReissuanceLockContract.contractId, reissuanceLockLabel,
                    contractState=createDummyReissuanceLock(listOf(createSimpleDummyStateAndRef()),
                        ReissuanceLock.ReissuanceLockStatus.INACTIVE), encumbrance = 1)
                output(SimpleDummyStateContract.contractId, reissuedStateLabel,
                    contractState=createSimpleDummyState(), encumbrance = 0)
                command(listOf(issuerParty.owningKey), ReissuanceRequestContract.Commands.Accept())
                command(listOf(issuerParty.owningKey), ReissuanceLockContract.Commands.Create())
                command(listOf(issuerParty.owningKey), SimpleDummyStateContract.Commands.Create())
                fails()
            }
        }
    }

    @Test
    fun `Re-issuance must produce encumbered state`() {
        val dummyReissuanceRequest = createDummySimpleStateReissuanceRequest(listOf(createDummyRef()))
        issuerNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReissuanceRequestContract.contractId, dummyReissuanceRequest)
            }
            transaction {
                input(ReissuanceRequestContract.contractId, dummyReissuanceRequest)
                output(ReissuanceLockContract.contractId, reissuanceLockLabel,
                    contractState=createDummyReissuanceLock(listOf(createSimpleDummyStateAndRef())))
                output(SimpleDummyStateContract.contractId, reissuedStateLabel,
                    contractState=createSimpleDummyState())
                command(listOf(issuerParty.owningKey), ReissuanceRequestContract.Commands.Accept())
                command(listOf(issuerParty.owningKey), ReissuanceLockContract.Commands.Create())
                command(listOf(issuerParty.owningKey), SimpleDummyStateContract.Commands.Create())
                fails()
            }
        }
    }

    @Test
    fun `State can't be re-issued without creating a re-issuance lock`() {
        val dummyReissuanceRequest = createDummySimpleStateReissuanceRequest(listOf(createDummyRef()))
        issuerNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReissuanceRequestContract.contractId, dummyReissuanceRequest)
            }
            transaction {
                input(ReissuanceRequestContract.contractId, dummyReissuanceRequest)
                output(SimpleDummyStateContract.contractId, reissuedStateLabel,
                    contractState=createSimpleDummyState(), encumbrance = 0)
                command(listOf(issuerParty.owningKey), ReissuanceRequestContract.Commands.Accept())
                command(listOf(issuerParty.owningKey), ReissuanceLockContract.Commands.Create())
                command(listOf(issuerParty.owningKey), SimpleDummyStateContract.Commands.Create())
                fails()
            }
        }
    }

    @Test
    fun `Re-issuance can't happen without a request`() {
        val dummyReissuanceRequest = createDummySimpleStateReissuanceRequest(listOf(createDummyRef()))
        issuerNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReissuanceRequestContract.contractId, dummyReissuanceRequest)
            }
            transaction {
                output(ReissuanceLockContract.contractId, reissuanceLockLabel,
                    contractState=createDummyReissuanceLock(listOf(createSimpleDummyStateAndRef())), encumbrance = 1)
                output(SimpleDummyStateContract.contractId, reissuedStateLabel,
                    contractState=createSimpleDummyState(), encumbrance = 0)
                command(listOf(issuerParty.owningKey), ReissuanceRequestContract.Commands.Accept())
                command(listOf(issuerParty.owningKey), ReissuanceLockContract.Commands.Create())
                command(listOf(issuerParty.owningKey), SimpleDummyStateContract.Commands.Create())
                fails()
            }
        }
    }
}
