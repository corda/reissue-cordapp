package com.r3.corda.lib.reissuance.dummy_contracts

import com.r3.corda.lib.reissuance.contracts.ReissuanceLockContract
import com.r3.corda.lib.reissuance.states.ReissuanceLock
import com.r3.corda.lib.tokens.contracts.FungibleTokenContract
import com.r3.corda.lib.tokens.contracts.commands.RedeemTokenCommand
import net.corda.testing.node.ledger
import org.junit.Test

class DeleteLockAndReissuedStatesTest: AbstractContractTest() {

    @Test
    fun `Re-issuance lock and re-issued state are deleted`() {
        val dummyState = createSimpleDummyState()
        val reissuanceLock = createDummyReissuanceLock(listOf(createSimpleDummyStateAndRef()))

        aliceNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReissuanceLockContract.contractId, reissuanceLockLabel,
                    contractState=reissuanceLock, encumbrance = 1)
                output(SimpleDummyStateContract.contractId, reissuedStateLabel,
                    contractState=dummyState, encumbrance = 0)
            }

            transaction {
                input(reissuanceLockLabel)
                input(reissuedStateLabel)
                command(listOf(aliceParty.owningKey, issuerParty.owningKey), ReissuanceLockContract.Commands.Delete())
                command(listOf(aliceParty.owningKey, issuerParty.owningKey), SimpleDummyStateContract.Commands.Delete())
                verifies()
            }
        }
    }

    @Test
    fun `Re-issuance lock and many re-issued state are deleted`() {
        val tokens = listOf(createToken(), createToken())
        val tokenIndices = tokens.indices.toList()
        val reissuanceLock = createDummyReissuanceLock(listOf(createTokenStateAndRef(), createTokenStateAndRef()))

        aliceNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                tokens.forEachIndexed { idx, token ->
                    output(FungibleTokenContract.contractId, reissuedStateLabel(idx),
                        contractState=token, encumbrance = idx+1)
                }
                output(ReissuanceLockContract.contractId, reissuanceLockLabel,
                    contractState=reissuanceLock, encumbrance = 0)
            }

            transaction {
                tokens.forEachIndexed { idx, _ ->
                    input(reissuedStateLabel(idx))
                }
                input(reissuanceLockLabel)

                command(listOf(aliceParty.owningKey, issuerParty.owningKey), ReissuanceLockContract.Commands.Delete())
                command(listOf(aliceParty.owningKey, issuerParty.owningKey), RedeemTokenCommand(issuedTokenType, tokenIndices, listOf()))
                verifies()
            }
        }
    }

    @Test
    fun `Rre-issued state can't deleted without re-issuance lock`() {
        val dummyState = createSimpleDummyState()
        val reissuanceLock = createDummyReissuanceLock(listOf(createSimpleDummyStateAndRef()))

        aliceNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReissuanceLockContract.contractId, reissuanceLockLabel,
                    contractState=reissuanceLock, encumbrance = 1)
                output(SimpleDummyStateContract.contractId, reissuedStateLabel,
                    contractState=dummyState, encumbrance = 0)
            }

            transaction {
                input(reissuedStateLabel)
                command(listOf(aliceParty.owningKey, issuerParty.owningKey), SimpleDummyStateContract.Commands.Delete())
                fails()
            }
        }
    }

    @Test
    fun `Re-issuance lock can't be deleted without re-issued state`() {
        val dummyState = createSimpleDummyState()
        val reissuanceLock = createDummyReissuanceLock(listOf(createSimpleDummyStateAndRef()))

        aliceNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReissuanceLockContract.contractId, reissuanceLockLabel,
                    contractState=reissuanceLock, encumbrance = 1)
                output(SimpleDummyStateContract.contractId, reissuedStateLabel,
                    contractState=dummyState, encumbrance = 0)
            }

            transaction {
                input(reissuanceLockLabel)
                command(listOf(aliceParty.owningKey, issuerParty.owningKey), ReissuanceLockContract.Commands.Delete())
                fails()
            }
        }
    }

    @Test
    fun `Re-issuance lock can't be deleted if it's state is INACTIVE`() {
        val dummyState = createSimpleDummyState()
        val reissuanceLock = createDummyReissuanceLock(listOf(createSimpleDummyStateAndRef()),
            ReissuanceLock.ReissuanceLockStatus.INACTIVE)

        aliceNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReissuanceLockContract.contractId, reissuanceLockLabel,
                    contractState=reissuanceLock, encumbrance = 1)
                output(SimpleDummyStateContract.contractId, reissuedStateLabel,
                    contractState=dummyState, encumbrance = 0)
            }

            transaction {
                input(reissuanceLockLabel)
                input(reissuedStateLabel)
                command(listOf(aliceParty.owningKey, issuerParty.owningKey), ReissuanceLockContract.Commands.Delete())
                command(listOf(aliceParty.owningKey, issuerParty.owningKey), SimpleDummyStateContract.Commands.Delete())
                fails()
            }
        }
    }

}
