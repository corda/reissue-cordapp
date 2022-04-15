package com.r3.corda.lib.reissuance.dummy_contracts

import com.r3.corda.lib.reissuance.contracts.ReissuanceLockContract
import com.r3.corda.lib.reissuance.contracts.ReissuanceRequestContract
import com.r3.corda.lib.reissuance.states.ReissuanceLock
import com.r3.corda.lib.tokens.contracts.FungibleTokenContract
import com.r3.corda.lib.tokens.contracts.commands.IssueTokenCommand
import net.corda.core.contracts.TimeWindow
import net.corda.core.transactions.WireTransaction
import net.corda.testing.node.ledger
import org.junit.Test
import java.time.Instant

class AcceptReissuanceRequestAndCreateReissuanceLockContractTest: AbstractContractTest() {

    @Test
    fun `SimpleDummyState is successfully re-issued`() {
        val ref = createDummyRef()
        val stateAndRef = createSimpleDummyStateAndRef(ref)

        var tx: WireTransaction? = null
        aliceNode.services.ledger(notary = notaryParty) {
            tx = unverifiedTransaction {
                input(SimpleDummyStateContract.contractId, stateAndRef.state.data)
            }
        }

        val dummyReissuanceRequest = createSimpleDummyStateReissuanceRequest(tx!!.inputs)
        val uploadedSignedTransactionSecureHash = issuerNode.services.attachments.importAttachment(
            generateSignedTransactionByteArrayInputStream(tx!!, listOf(aliceNode)), aliceParty.toString(), null)

        issuerNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReissuanceRequestContract.contractId, dummyReissuanceRequest)
            }

            transaction {
                input(ReissuanceRequestContract.contractId, dummyReissuanceRequest)
                output(
                    ReissuanceLockContract.contractId, reissuanceLockLabel,
                    contractState = createDummyReissuanceLock2(
                        signableData = createSignableData(tx!!.id, issuerParty.owningKey),
                        requiredSigners = listOf(issuerParty, aliceParty),
                        timeWindow = TimeWindow.untilOnly(Instant.now().plusSeconds(5))
                    ),
                    encumbrance = 1)
                attachment(uploadedSignedTransactionSecureHash)
                output(SimpleDummyStateContract.contractId, reissuedStateLabel, contractState = createSimpleDummyState(), encumbrance = 0)
                command(listOf(issuerParty.owningKey), ReissuanceRequestContract.Commands.Accept())
                command(listOf(issuerParty.owningKey, aliceParty.owningKey), ReissuanceLockContract.Commands.Create())
                command(listOf(issuerParty.owningKey), SimpleDummyStateContract.Commands.Create())
                timeWindow(TimeWindow.untilOnly(Instant.now().plusSeconds(5)))
                verifies()
            }
        }
    }

    @Test
    fun `Many states are successfully re-issued`() {

        val stateAndRef1 = createTokenStateAndRef()
        val stateAndRef2 = createTokenStateAndRef()
        val stateAndRef3 = createTokenStateAndRef()

        var tx: WireTransaction? = null
        aliceNode.services.ledger(notary = notaryParty) {
            tx = unverifiedTransaction {
                input(FungibleTokenContract.contractId, stateAndRef1.state.data)
                input(FungibleTokenContract.contractId, stateAndRef2.state.data)
                input(FungibleTokenContract.contractId, stateAndRef3.state.data)
            }
        }

        val dummyReissuanceRequest = createTokensReissuanceRequest(tx!!.inputs)
        val uploadedSignedTransactionSecureHash = issuerNode.services.attachments.importAttachment(
            generateSignedTransactionByteArrayInputStream(tx!!, listOf(aliceNode)), aliceParty.toString(), null)

        issuerNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReissuanceRequestContract.contractId, dummyReissuanceRequest)
            }

            transaction {
                input(ReissuanceRequestContract.contractId, dummyReissuanceRequest)
                attachment(uploadedSignedTransactionSecureHash)
                output(FungibleTokenContract.contractId, reissuedStateLabel(0), contractState = createToken(), encumbrance = 1)
                output(FungibleTokenContract.contractId, reissuedStateLabel(1), contractState = createToken(), encumbrance = 2)
                output(FungibleTokenContract.contractId, reissuedStateLabel(2), contractState = createToken(), encumbrance = 3)
                output(
                    ReissuanceLockContract.contractId, reissuanceLockLabel,
                    contractState = createDummyReissuanceLock2(
                        signableData = createSignableData(tx!!.id, issuerParty.owningKey),
                        requiredSigners = listOf(issuerParty, aliceParty),
                        timeWindow = TimeWindow.untilOnly(Instant.now().plusSeconds(5))
                    ),
                    encumbrance = 0
                )
                command(listOf(issuerParty.owningKey), ReissuanceRequestContract.Commands.Accept())
                command(listOf(issuerParty.owningKey, aliceParty.owningKey), ReissuanceLockContract.Commands.Create())
                command(listOf(issuerParty.owningKey), IssueTokenCommand(issuedTokenType, listOf(0, 1, 2)))
                timeWindow(TimeWindow.untilOnly(Instant.now().plusSeconds(5)))
                verifies()
            }
        }
    }

    @Test
    fun `State can't be re-issued if lock status is INACTIVE`() {
        val ref = createDummyRef()
        val stateAndRef = createSimpleDummyStateAndRef(ref)

        var tx: WireTransaction? = null
        aliceNode.services.ledger(notary = notaryParty) {
            tx = unverifiedTransaction {
                input(SimpleDummyStateContract.contractId, stateAndRef.state.data)
            }
        }

        val dummyReissuanceRequest = createSimpleDummyStateReissuanceRequest(tx!!.inputs)
        val uploadedWireTransactionSecureHash = issuerNode.services.attachments.importAttachment(
            generateWireTransactionByteArrayInputStream(wireTransaction = tx!!), aliceParty.toString(), null)

        issuerNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReissuanceRequestContract.contractId, dummyReissuanceRequest)
            }

            transaction {
                input(ReissuanceRequestContract.contractId, dummyReissuanceRequest)
                output(
                    ReissuanceLockContract.contractId, reissuanceLockLabel,
                    contractState = createDummyReissuanceLock2(
                        signableData = createSignableData(tx!!.id, issuerParty.owningKey),
                        requiredSigners = listOf(issuerParty, aliceParty),
                        timeWindow = TimeWindow.untilOnly(Instant.now().plusSeconds(5)),
                        status = ReissuanceLock.ReissuanceLockStatus2.INACTIVE
                    ),
                    encumbrance = 1)
                attachment(uploadedWireTransactionSecureHash)
                output(SimpleDummyStateContract.contractId, reissuedStateLabel, contractState = createSimpleDummyState(), encumbrance = 0)
                command(listOf(issuerParty.owningKey), ReissuanceRequestContract.Commands.Accept())
                command(listOf(issuerParty.owningKey, aliceParty.owningKey), ReissuanceLockContract.Commands.Create())
                command(listOf(issuerParty.owningKey), SimpleDummyStateContract.Commands.Create())
                timeWindow(TimeWindow.untilOnly(Instant.now().plusSeconds(5)))
                fails()
            }
        }
    }

    @Test
    fun `Re-issuance must produce encumbered state`() {
        val ref = createDummyRef()
        val stateAndRef = createSimpleDummyStateAndRef(ref)

        var tx: WireTransaction? = null
        aliceNode.services.ledger(notary = notaryParty) {
            tx = unverifiedTransaction {
                input(SimpleDummyStateContract.contractId, stateAndRef.state.data)
            }
        }

        val dummyReissuanceRequest = createSimpleDummyStateReissuanceRequest(tx!!.inputs)
        val uploadedWireTransactionSecureHash = issuerNode.services.attachments.importAttachment(
            generateWireTransactionByteArrayInputStream(wireTransaction = tx!!), aliceParty.toString(), null)

        issuerNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReissuanceRequestContract.contractId, dummyReissuanceRequest)
            }

            transaction {
                input(ReissuanceRequestContract.contractId, dummyReissuanceRequest)
                output(
                    ReissuanceLockContract.contractId, reissuanceLockLabel,
                    contractState = createDummyReissuanceLock2(
                        signableData = createSignableData(tx!!.id, issuerParty.owningKey),
                        requiredSigners = listOf(issuerParty, aliceParty),
                        timeWindow = TimeWindow.untilOnly(Instant.now().plusSeconds(5))
                    ))
                attachment(uploadedWireTransactionSecureHash)
                output(SimpleDummyStateContract.contractId, reissuedStateLabel, contractState = createSimpleDummyState())
                command(listOf(issuerParty.owningKey), ReissuanceRequestContract.Commands.Accept())
                command(listOf(issuerParty.owningKey, aliceParty.owningKey), ReissuanceLockContract.Commands.Create())
                command(listOf(issuerParty.owningKey), SimpleDummyStateContract.Commands.Create())
                timeWindow(TimeWindow.untilOnly(Instant.now().plusSeconds(5)))
                fails()
            }
        }
    }

    @Test
    fun `State can't be re-issued without creating a re-issuance lock`() {
        val ref = createDummyRef()
        val stateAndRef = createSimpleDummyStateAndRef(ref)

        var tx: WireTransaction? = null
        aliceNode.services.ledger(notary = notaryParty) {
            tx = unverifiedTransaction {
                input(SimpleDummyStateContract.contractId, stateAndRef.state.data)
            }
        }

        val dummyReissuanceRequest = createSimpleDummyStateReissuanceRequest(tx!!.inputs)
        val uploadedWireTransactionSecureHash = issuerNode.services.attachments.importAttachment(
            generateWireTransactionByteArrayInputStream(wireTransaction = tx!!), aliceParty.toString(), null)

        issuerNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReissuanceRequestContract.contractId, dummyReissuanceRequest)
            }

            transaction {
                input(ReissuanceRequestContract.contractId, dummyReissuanceRequest)
                attachment(uploadedWireTransactionSecureHash)
                output(SimpleDummyStateContract.contractId, reissuedStateLabel, contractState =
                createSimpleDummyState())
                command(listOf(issuerParty.owningKey), ReissuanceRequestContract.Commands.Accept())
                command(listOf(issuerParty.owningKey, aliceParty.owningKey), ReissuanceLockContract.Commands.Create())
                command(listOf(issuerParty.owningKey), SimpleDummyStateContract.Commands.Create())
                timeWindow(TimeWindow.untilOnly(Instant.now().plusSeconds(5)))
                fails()
            }
        }
    }

    @Test
    fun `Re-issuance can't happen without a request`() {
        val ref = createDummyRef()
        val stateAndRef = createSimpleDummyStateAndRef(ref)

        var tx: WireTransaction? = null
        aliceNode.services.ledger(notary = notaryParty) {
            tx = unverifiedTransaction {
                input(SimpleDummyStateContract.contractId, stateAndRef.state.data)
            }
        }

        val uploadedWireTransactionSecureHash = issuerNode.services.attachments.importAttachment(
            generateWireTransactionByteArrayInputStream(wireTransaction = tx!!), aliceParty.toString(), null)

        issuerNode.services.ledger(notary = notaryParty) {

            transaction {
                output(
                    ReissuanceLockContract.contractId, reissuanceLockLabel,
                    contractState = createDummyReissuanceLock2(
                        signableData = createSignableData(tx!!.id, issuerParty.owningKey),
                        requiredSigners = listOf(issuerParty, aliceParty),
                        timeWindow = TimeWindow.untilOnly(Instant.now().plusSeconds(5))
                    ),
                    encumbrance = 1)
                attachment(uploadedWireTransactionSecureHash)
                output(SimpleDummyStateContract.contractId, reissuedStateLabel, contractState = createSimpleDummyState(), encumbrance = 0)
                command(listOf(issuerParty.owningKey), ReissuanceRequestContract.Commands.Accept())
                command(listOf(issuerParty.owningKey, aliceParty.owningKey), ReissuanceLockContract.Commands.Create())
                command(listOf(issuerParty.owningKey), SimpleDummyStateContract.Commands.Create())
                timeWindow(TimeWindow.untilOnly(Instant.now().plusSeconds(5)))
                fails()
            }
        }
    }
}
