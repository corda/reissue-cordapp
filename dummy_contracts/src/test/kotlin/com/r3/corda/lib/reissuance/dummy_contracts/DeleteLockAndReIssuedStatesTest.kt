package com.r3.corda.lib.reissuance.dummy_contracts

import com.r3.corda.lib.reissuance.contracts.ReIssuanceLockContract
import com.r3.corda.lib.reissuance.states.ReIssuanceLock
import com.r3.corda.lib.tokens.contracts.FungibleTokenContract
import com.r3.corda.lib.tokens.contracts.commands.RedeemTokenCommand
import net.corda.testing.node.ledger
import org.junit.Test

class DeleteLockAndReIssuedStatesTest: AbstractContractTest() {

    @Test
    fun `Re-issuance lock and re-issued state are deleted`() {
        val dummyState = createSimpleDummyState()
        val reIssuanceLock = createDummyReIssuanceLock(listOf(createSimpleDummyStateAndRef()))

        aliceNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReIssuanceLockContract.contractId, reIssuanceLockLabel,
                    contractState=reIssuanceLock, encumbrance = 1)
                output(SimpleDummyStateContract.contractId, reIssuedStateLabel,
                    contractState=dummyState, encumbrance = 0)
            }

            transaction {
                input(reIssuanceLockLabel)
                input(reIssuedStateLabel)
                command(listOf(aliceParty.owningKey, issuerParty.owningKey), ReIssuanceLockContract.Commands.Delete())
                command(listOf(aliceParty.owningKey, issuerParty.owningKey), SimpleDummyStateContract.Commands.Delete())
                verifies()
            }
        }
    }

    @Test
    fun `Re-issuance lock and many re-issued state are deleted`() {
        val tokens = listOf(createToken(), createToken())
        val tokenIndices = tokens.indices.toList()
        val reIssuanceLock = createDummyReIssuanceLock(listOf(createTokenStateAndRef(), createTokenStateAndRef()))

        aliceNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                tokens.forEachIndexed { idx, token ->
                    output(FungibleTokenContract.contractId, reIssuedStateLabel(idx),
                        contractState=token, encumbrance = idx+1)
                }
                output(ReIssuanceLockContract.contractId, reIssuanceLockLabel,
                    contractState=reIssuanceLock, encumbrance = 0)
            }

            transaction {
                tokens.forEachIndexed { idx, _ ->
                    input(reIssuedStateLabel(idx))
                }
                input(reIssuanceLockLabel)

                command(listOf(aliceParty.owningKey, issuerParty.owningKey), ReIssuanceLockContract.Commands.Delete())
                command(listOf(aliceParty.owningKey, issuerParty.owningKey), RedeemTokenCommand(issuedTokenType, tokenIndices, listOf()))
                verifies()
            }
        }
    }

    @Test
    fun `Rre-issued state can't deleted without re-issuance lock`() {
        val dummyState = createSimpleDummyState()
        val reIssuanceLock = createDummyReIssuanceLock(listOf(createSimpleDummyStateAndRef()))

        aliceNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReIssuanceLockContract.contractId, reIssuanceLockLabel,
                    contractState=reIssuanceLock, encumbrance = 1)
                output(SimpleDummyStateContract.contractId, reIssuedStateLabel,
                    contractState=dummyState, encumbrance = 0)
            }

            transaction {
                input(reIssuedStateLabel)
                command(listOf(aliceParty.owningKey, issuerParty.owningKey), SimpleDummyStateContract.Commands.Delete())
                fails()
            }
        }
    }

    @Test
    fun `Re-issuance lock can't be deleted without re-issued state`() {
        val dummyState = createSimpleDummyState()
        val reIssuanceLock = createDummyReIssuanceLock(listOf(createSimpleDummyStateAndRef()))

        aliceNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReIssuanceLockContract.contractId, reIssuanceLockLabel,
                    contractState=reIssuanceLock, encumbrance = 1)
                output(SimpleDummyStateContract.contractId, reIssuedStateLabel,
                    contractState=dummyState, encumbrance = 0)
            }

            transaction {
                input(reIssuanceLockLabel)
                command(listOf(aliceParty.owningKey, issuerParty.owningKey), ReIssuanceLockContract.Commands.Delete())
                fails()
            }
        }
    }

    @Test
    fun `Re-issuance lock can't be deleted if it's state is INACTIVE`() {
        val dummyState = createSimpleDummyState()
        val reIssuanceLock = createDummyReIssuanceLock(listOf(createSimpleDummyStateAndRef()),
            ReIssuanceLock.ReIssuanceLockStatus.INACTIVE)

        aliceNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReIssuanceLockContract.contractId, reIssuanceLockLabel,
                    contractState=reIssuanceLock, encumbrance = 1)
                output(SimpleDummyStateContract.contractId, reIssuedStateLabel,
                    contractState=dummyState, encumbrance = 0)
            }

            transaction {
                input(reIssuanceLockLabel)
                input(reIssuedStateLabel)
                command(listOf(aliceParty.owningKey, issuerParty.owningKey), ReIssuanceLockContract.Commands.Delete())
                command(listOf(aliceParty.owningKey, issuerParty.owningKey), SimpleDummyStateContract.Commands.Delete())
                fails()
            }
        }
    }

}
