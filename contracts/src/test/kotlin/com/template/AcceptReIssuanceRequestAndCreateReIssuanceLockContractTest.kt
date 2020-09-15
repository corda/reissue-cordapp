package com.template

import com.r3.corda.lib.tokens.contracts.FungibleTokenContract
import com.r3.corda.lib.tokens.contracts.commands.IssueTokenCommand
import com.template.contracts.ReIssuanceLockContract
import com.template.contracts.ReIssuanceRequestContract
import com.template.contracts.example.SimpleDummyStateContract
import com.template.states.example.SimpleDummyState
import net.corda.core.contracts.StateAndRef
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
                output(SimpleDummyStateContract.contractId, reIssuedState1Label,
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
                output(FungibleTokenContract.contractId, reIssuedState1Label,
                    contractState=createToken(), encumbrance = 1)
                output(FungibleTokenContract.contractId, reIssuedState2Label,
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
                output(SimpleDummyStateContract.contractId, reIssuedState1Label,
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
                output(SimpleDummyStateContract.contractId, reIssuedState1Label,
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
                output(SimpleDummyStateContract.contractId, reIssuedState1Label,
                    contractState=createSimpleDummyState(), encumbrance = 0)
                command(listOf(issuerParty.owningKey), ReIssuanceRequestContract.Commands.Accept())
                command(listOf(issuerParty.owningKey), ReIssuanceLockContract.Commands.Create())
                command(listOf(issuerParty.owningKey), SimpleDummyStateContract.Commands.Create())
                fails()
            }
        }
    }
}
