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
        val dummyState = createSimpleDummyStateAndRef()
        val reissuanceLockAndHashPair = prepareReissuanceLockState(SimpleDummyStateContract.contractId, listOf(dummyState))
        val reissuanceLock = reissuanceLockAndHashPair.first
        val txHash = reissuanceLockAndHashPair.second

        issuerNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReissuanceLockContract.contractId, reissuanceLockLabel,
                    contractState=reissuanceLock, encumbrance = 1)
                output(SimpleDummyStateContract.contractId, reissuedStateLabel,
                    contractState=dummyState.state.data, encumbrance = 0)
            }

            transaction {
                input(reissuanceLockLabel)
                input(reissuedStateLabel)
                command(listOf(aliceParty.owningKey, issuerParty.owningKey), ReissuanceLockContract.Commands.Delete())
                command(listOf(aliceParty.owningKey, issuerParty.owningKey), SimpleDummyStateContract.Commands.Delete())
                attachment(txHash)
                verifies()
            }
        }
    }

    @Test
    fun `Re-issuance lock and many re-issued state are deleted`() {
        val tokens = listOf(createTokenStateAndRef(), createTokenStateAndRef())
        val tokenIndices = tokens.indices.toList()
        val reissuanceLockAndHashPair = prepareReissuanceLockState(FungibleTokenContract.contractId, tokens)
        val reissuanceLock = reissuanceLockAndHashPair.first
        val txHash = reissuanceLockAndHashPair.second

        issuerNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                tokens.forEachIndexed { idx, token ->
                    output(FungibleTokenContract.contractId, reissuedStateLabel(idx),
                        contractState=token.state.data, encumbrance = idx+1)
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
                command(listOf(aliceParty.owningKey, issuerParty.owningKey), RedeemTokenCommand(issuedTokenType,
                    tokenIndices, listOf()))
                attachment(txHash)
                verifies()
            }
        }
    }

    @Test
    fun `Re-issued state can't deleted without re-issuance lock`() {
        val dummyState = createSimpleDummyStateAndRef()
        val reissuanceLockAndHashPair = prepareReissuanceLockState(SimpleDummyStateContract.contractId, listOf(dummyState))
        val reissuanceLock = reissuanceLockAndHashPair.first

        issuerNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReissuanceLockContract.contractId, reissuanceLockLabel,
                    contractState=reissuanceLock, encumbrance = 1)
                output(SimpleDummyStateContract.contractId, reissuedStateLabel,
                    contractState=dummyState.state.data, encumbrance = 0)
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
        val dummyState = createSimpleDummyStateAndRef()
        val reissuanceLockAndHashPair = prepareReissuanceLockState(SimpleDummyStateContract.contractId, listOf
            (dummyState), ReissuanceLock.ReissuanceLockStatus2.INACTIVE)
        val reissuanceLock = reissuanceLockAndHashPair.first

        issuerNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReissuanceLockContract.contractId, reissuanceLockLabel,
                    contractState=reissuanceLock, encumbrance = 1)
                output(SimpleDummyStateContract.contractId, reissuedStateLabel,
                    contractState=dummyState.state.data, encumbrance = 0)
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
        val dummyState = createSimpleDummyStateAndRef()
        val reissuanceLockAndHashPair = prepareReissuanceLockState(SimpleDummyStateContract.contractId, listOf(dummyState))
        val reissuanceLock = reissuanceLockAndHashPair.first

        issuerNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReissuanceLockContract.contractId, reissuanceLockLabel,
                    contractState=reissuanceLock, encumbrance = 1)
                output(SimpleDummyStateContract.contractId, reissuedStateLabel,
                    contractState=dummyState.state.data, encumbrance = 0)
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
