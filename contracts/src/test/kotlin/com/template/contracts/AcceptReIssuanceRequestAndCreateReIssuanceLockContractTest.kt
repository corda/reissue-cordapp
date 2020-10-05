package com.template.contracts

import com.r3.corda.lib.tokens.contracts.FungibleTokenContract
import com.r3.corda.lib.tokens.contracts.commands.IssueTokenCommand
import com.template.contracts.example.SimpleDummyStateContract
import com.template.states.ReIssuanceLock
import net.corda.testing.node.ledger
import org.junit.Test

class AcceptReIssuanceRequestAndCreateReIssuanceLockContractTest: AbstractContractTest() {

    @Test
    fun `State is successfully re-issued`() {
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
                command(listOf(issuerParty.owningKey), ReIssuanceRequestContract.Commands.Accept())
                command(listOf(issuerParty.owningKey), ReIssuanceLockContract.Commands.Create())
                command(listOf(issuerParty.owningKey), SimpleDummyStateContract.Commands.Create())
                verifies()
            }
        }
    }

    @Test
    fun `Many states are successfully re-issued`() {
        val dummyReIssuanceRequest = createTokensReIssuanceRequest(listOf(createDummyRef(), createDummyRef()))
        issuerNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReIssuanceRequestContract.contractId, dummyReIssuanceRequest)
            }
            transaction {
                input(ReIssuanceRequestContract.contractId, dummyReIssuanceRequest)
                output(FungibleTokenContract.contractId, reIssuedStateLabel(0),
                    contractState=createToken(), encumbrance = 1)
                output(FungibleTokenContract.contractId, reIssuedStateLabel(1),
                    contractState=createToken(), encumbrance = 2)
                output(ReIssuanceLockContract.contractId, reIssuanceLockLabel,
                    contractState=createDummyReIssuanceLock(listOf(createTokenStateAndRef(), createTokenStateAndRef())), encumbrance = 0)
                command(listOf(issuerParty.owningKey), ReIssuanceRequestContract.Commands.Accept())
                command(listOf(issuerParty.owningKey), ReIssuanceLockContract.Commands.Create())
                command(listOf(issuerParty.owningKey), IssueTokenCommand(issuedTokenType, listOf(0, 1)))
                verifies()
            }
        }
    }

    @Test
    fun `State can't be re-issued if lock status is INACTIVE`() {
        val dummyReIssuanceRequest = createDummySimpleStateReIssuanceRequest(listOf(createDummyRef()))
        issuerNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReIssuanceRequestContract.contractId, dummyReIssuanceRequest)
            }

            transaction {
                input(ReIssuanceRequestContract.contractId, dummyReIssuanceRequest)
                output(ReIssuanceLockContract.contractId, reIssuanceLockLabel,
                    contractState=createDummyReIssuanceLock(listOf(createSimpleDummyStateAndRef()),
                        ReIssuanceLock.ReIssuanceLockStatus.INACTIVE), encumbrance = 1)
                output(SimpleDummyStateContract.contractId, reIssuedStateLabel,
                    contractState=createSimpleDummyState(), encumbrance = 0)
                command(listOf(issuerParty.owningKey), ReIssuanceRequestContract.Commands.Accept())
                command(listOf(issuerParty.owningKey), ReIssuanceLockContract.Commands.Create())
                command(listOf(issuerParty.owningKey), SimpleDummyStateContract.Commands.Create())
                fails()
            }
        }
    }

    @Test
    fun `Re-issuance must produce encumbered state`() {
        val dummyReIssuanceRequest = createDummySimpleStateReIssuanceRequest(listOf(createDummyRef()))
        issuerNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReIssuanceRequestContract.contractId, dummyReIssuanceRequest)
            }
            transaction {
                input(ReIssuanceRequestContract.contractId, dummyReIssuanceRequest)
                output(ReIssuanceLockContract.contractId, reIssuanceLockLabel,
                    contractState=createDummyReIssuanceLock(listOf(createSimpleDummyStateAndRef())))
                output(SimpleDummyStateContract.contractId, reIssuedStateLabel,
                    contractState=createSimpleDummyState())
                command(listOf(issuerParty.owningKey), ReIssuanceRequestContract.Commands.Accept())
                command(listOf(issuerParty.owningKey), ReIssuanceLockContract.Commands.Create())
                command(listOf(issuerParty.owningKey), SimpleDummyStateContract.Commands.Create())
                fails()
            }
        }
    }

    @Test
    fun `State can't be re-issued without creating a re-issuance lock`() {
        val dummyReIssuanceRequest = createDummySimpleStateReIssuanceRequest(listOf(createDummyRef()))
        issuerNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReIssuanceRequestContract.contractId, dummyReIssuanceRequest)
            }
            transaction {
                input(ReIssuanceRequestContract.contractId, dummyReIssuanceRequest)
                output(SimpleDummyStateContract.contractId, reIssuedStateLabel,
                    contractState=createSimpleDummyState(), encumbrance = 0)
                command(listOf(issuerParty.owningKey), ReIssuanceRequestContract.Commands.Accept())
                command(listOf(issuerParty.owningKey), ReIssuanceLockContract.Commands.Create())
                command(listOf(issuerParty.owningKey), SimpleDummyStateContract.Commands.Create())
                fails()
            }
        }
    }

    @Test
    fun `Re-issuance can't happen without a request`() {
        val dummyReIssuanceRequest = createDummySimpleStateReIssuanceRequest(listOf(createDummyRef()))
        issuerNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReIssuanceRequestContract.contractId, dummyReIssuanceRequest)
            }
            transaction {
                output(ReIssuanceLockContract.contractId, reIssuanceLockLabel,
                    contractState=createDummyReIssuanceLock(listOf(createSimpleDummyStateAndRef())), encumbrance = 1)
                output(SimpleDummyStateContract.contractId, reIssuedStateLabel,
                    contractState=createSimpleDummyState(), encumbrance = 0)
                command(listOf(issuerParty.owningKey), ReIssuanceRequestContract.Commands.Accept())
                command(listOf(issuerParty.owningKey), ReIssuanceLockContract.Commands.Create())
                command(listOf(issuerParty.owningKey), SimpleDummyStateContract.Commands.Create())
                fails()
            }
        }
    }
}
