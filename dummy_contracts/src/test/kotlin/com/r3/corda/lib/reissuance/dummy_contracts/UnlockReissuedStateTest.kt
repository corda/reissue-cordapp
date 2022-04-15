package com.r3.corda.lib.reissuance.dummy_contracts

import com.r3.corda.lib.reissuance.contracts.ReissuanceLockContract
import com.r3.corda.lib.reissuance.states.ReissuanceLock
import net.corda.testing.node.ledger
import org.junit.Test

class UnlockReissuedStateTest: AbstractContractTest() {

    @Test
    fun `Re-issued state is unlocked with all required signers`() {
        val dummyState = createSimpleDummyStateAndRef()

        val reissuanceLockAndHashPair = prepareReissuanceLockState(SimpleDummyStateContract.contractId, listOf(dummyState),
            nodesToSign = listOf(aliceNode, notaryNode, issuerNode))
        val reissuanceLock = reissuanceLockAndHashPair.first

        issuerNode.services.ledger(notary = notaryParty) {

            transaction {
                attachment(reissuanceLockAndHashPair.second)
                input(ReissuanceLockContract.contractId, reissuanceLock)
                output(
                    ReissuanceLockContract.contractId,
                    reissuanceLock.copy(status = ReissuanceLock.ReissuanceLockStatus2.INACTIVE))
                command(listOf(aliceParty.owningKey), ReissuanceLockContract.Commands.Deactivate())
                command(listOf(aliceParty.owningKey), SimpleDummyStateContract.Commands.Update())
                verifies()
            }
        }
    }

    @Test
    fun `Many re-issued states are unlocked`() {
        val dummyState1 = createSimpleDummyStateAndRef()
        val dummyState2 = createSimpleDummyStateAndRef()

        val reissuanceLockAndHashPair = prepareReissuanceLockState(SimpleDummyStateContract.contractId, listOf(dummyState1, dummyState2),
            nodesToSign = listOf(aliceNode, notaryNode, issuerNode))
        val reissuanceLock = reissuanceLockAndHashPair.first

        issuerNode.services.ledger(notary = notaryParty) {

            transaction {
                attachment(reissuanceLockAndHashPair.second)
                input(ReissuanceLockContract.contractId, reissuanceLock)
                output(
                    ReissuanceLockContract.contractId,
                    reissuanceLock.copy(status = ReissuanceLock.ReissuanceLockStatus2.INACTIVE))
                command(listOf(aliceParty.owningKey), ReissuanceLockContract.Commands.Deactivate())
                command(listOf(aliceParty.owningKey), SimpleDummyStateContract.Commands.Update())
                verifies()
            }
        }
    }

    @Test
    fun `Re-issued state can't be unlocked when issuer signature is required and missing`() {
        val dummyState1 = createSimpleDummyStateAndRef()
        val dummyState2 = createSimpleDummyStateAndRef()

        val reissuanceLockAndHashPair = prepareReissuanceLockState(SimpleDummyStateContract.contractId, listOf(dummyState1, dummyState2),
            nodesToSign = listOf(aliceNode, notaryNode))
        val reissuanceLock = reissuanceLockAndHashPair.first

        issuerNode.services.ledger(notary = notaryParty) {

            transaction {
                attachment(reissuanceLockAndHashPair.second)
                input(ReissuanceLockContract.contractId, reissuanceLock)
                output(
                    ReissuanceLockContract.contractId,
                    reissuanceLock.copy(status = ReissuanceLock.ReissuanceLockStatus2.INACTIVE))
                command(listOf(aliceParty.owningKey), ReissuanceLockContract.Commands.Deactivate())
                command(listOf(aliceParty.owningKey), SimpleDummyStateContract.Commands.Update())
                fails()
            }
        }
    }

    @Test
    fun `Re-issued state can't unlocked if output ReissuanceLock status is ACTIVE`() {
        val dummyState1 = createSimpleDummyStateAndRef()
        val dummyState2 = createSimpleDummyStateAndRef()

        val reissuanceLockAndHashPair = prepareReissuanceLockState(SimpleDummyStateContract.contractId, listOf(dummyState1, dummyState2),
            nodesToSign = listOf(aliceNode, notaryNode, issuerNode))
        val reissuanceLock = reissuanceLockAndHashPair.first

        issuerNode.services.ledger(notary = notaryParty) {

            transaction {
                attachment(reissuanceLockAndHashPair.second)
                input(ReissuanceLockContract.contractId, reissuanceLock)
                output(
                    ReissuanceLockContract.contractId,
                    reissuanceLock.copy(status = ReissuanceLock.ReissuanceLockStatus2.ACTIVE))
                command(listOf(aliceParty.owningKey), ReissuanceLockContract.Commands.Deactivate())
                command(listOf(aliceParty.owningKey), SimpleDummyStateContract.Commands.Update())
                fails()
            }
        }
    }

    @Test
    fun `Re-issued state can't be unlocked without attached transaction proving that the original state had been deleted`() {
        val dummyState = createSimpleDummyStateAndRef()

        val reissuanceLockAndHashPair = prepareReissuanceLockState(SimpleDummyStateContract.contractId, listOf(dummyState),
            nodesToSign = listOf(aliceNode, notaryNode, issuerNode))
        val reissuanceLock = reissuanceLockAndHashPair.first

        issuerNode.services.ledger(notary = notaryParty) {

            transaction {
                input(ReissuanceLockContract.contractId, reissuanceLock)
                output(
                    ReissuanceLockContract.contractId,
                    reissuanceLock.copy(status = ReissuanceLock.ReissuanceLockStatus2.INACTIVE))
                command(listOf(aliceParty.owningKey), ReissuanceLockContract.Commands.Deactivate())
                command(listOf(aliceParty.owningKey), SimpleDummyStateContract.Commands.Update())
                fails()
            }
        }
    }

    @Test
    fun `Re-issued state can't be unlocked when attached transaction is not notarised`() {
        val dummyState = createSimpleDummyStateAndRef()

        val reissuanceLockAndHashPair = prepareReissuanceLockState(SimpleDummyStateContract.contractId, listOf(dummyState),
            nodesToSign = listOf(aliceNode, issuerNode))
        val reissuanceLock = reissuanceLockAndHashPair.first

        issuerNode.services.ledger(notary = notaryParty) {

            transaction {
                attachment(reissuanceLockAndHashPair.second)
                input(ReissuanceLockContract.contractId, reissuanceLock)
                output(
                    ReissuanceLockContract.contractId,
                    reissuanceLock.copy(status = ReissuanceLock.ReissuanceLockStatus2.INACTIVE))
                command(listOf(aliceParty.owningKey), ReissuanceLockContract.Commands.Deactivate())
                command(listOf(aliceParty.owningKey), SimpleDummyStateContract.Commands.Update())
                fails()
            }
        }
    }

    @Test
    fun `Re-issued state can't be unlocked without re-issuance lock`() {
        val dummyState = createSimpleDummyStateAndRef()

        val reissuanceLockAndHashPair = prepareReissuanceLockState(SimpleDummyStateContract.contractId, listOf(dummyState),
            nodesToSign = listOf(aliceNode, notaryNode, issuerNode))
        val reissuanceLock = reissuanceLockAndHashPair.first

        issuerNode.services.ledger(notary = notaryParty) {

            transaction {
                attachment(reissuanceLockAndHashPair.second)
                output(
                    ReissuanceLockContract.contractId,
                    reissuanceLock.copy(status = ReissuanceLock.ReissuanceLockStatus2.INACTIVE))
                command(listOf(aliceParty.owningKey), ReissuanceLockContract.Commands.Deactivate())
                command(listOf(aliceParty.owningKey), SimpleDummyStateContract.Commands.Update())
                fails()
            }
        }
    }

}
