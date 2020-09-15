package com.template

import com.template.contracts.ReIssuanceLockContract
import com.template.contracts.example.SimpleDummyStateContract
import com.template.states.example.SimpleDummyState
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.internal.TestStartedNode
import net.corda.testing.node.ledger
import org.junit.Test

class UnlockReIssuedStateTest: AbstractContractTest() {

    @Test
    fun `Re-issued state is unlocked`() {
        val dummyState = createSimpleDummyState()

        val deleteStateWireTransaction = generateDeleteDummyStateWireTransaction(dummyState)
        val deleteStateSignedTransaction = generateDeleteDummyStateSignedTransaction(deleteStateWireTransaction)
        val deletedSignedTransactionInputStream = generateSignedTransactionByteArray(deleteStateSignedTransaction)
            .inputStream()

        val uploadedDeletedTransactionSecureHash = aliceNode.services.attachments.importAttachment(
            deletedSignedTransactionInputStream, aliceParty.toString(), null)
        val deletedStateRef: StateRef = deleteStateSignedTransaction.coreTransaction.inputs[0]

        aliceNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReIssuanceLockContract.contractId, reIssuanceLockLabel,
                    contractState=createDummyReIssuanceLock(listOf(createSimpleDummyStateAndRef(deletedStateRef))),
                    encumbrance = 1)
                output(SimpleDummyStateContract.contractId, reIssuedState1Label,
                    contractState=dummyState, encumbrance = 0)
            }

            transaction {
                attachment(uploadedDeletedTransactionSecureHash)
                input(reIssuanceLockLabel)
                input(reIssuedState1Label)
                output(SimpleDummyStateContract.contractId, dummyState)
                command(listOf(aliceParty.owningKey), ReIssuanceLockContract.Commands.Delete())
                command(listOf(aliceParty.owningKey), SimpleDummyStateContract.Commands.Update())
                verifies()
            }
        }
    }

    @Test
    fun `Re-issued state can't be unlocked without attached transaction proving that the original state had been deleted`() {
        val dummyState = createSimpleDummyState()

        val deleteStateWireTransaction = generateDeleteDummyStateWireTransaction(dummyState)
        val deleteStateSignedTransaction = generateDeleteDummyStateSignedTransaction(deleteStateWireTransaction)

        val deletedStateRef: StateRef = deleteStateSignedTransaction.coreTransaction.inputs[0]

        aliceNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReIssuanceLockContract.contractId, reIssuanceLockLabel,
                    contractState=createDummyReIssuanceLock(listOf(createSimpleDummyStateAndRef(deletedStateRef))),
                    encumbrance = 1)
                output(SimpleDummyStateContract.contractId, reIssuedState1Label,
                    contractState=dummyState, encumbrance = 0)
            }

            transaction {
                input(reIssuanceLockLabel)
                input(reIssuedState1Label)
                output(SimpleDummyStateContract.contractId, dummyState)
                command(listOf(aliceParty.owningKey), ReIssuanceLockContract.Commands.Delete())
                command(listOf(aliceParty.owningKey), SimpleDummyStateContract.Commands.Update())
                fails()
            }
        }
    }

    @Test
    fun `Re-issued state can't be unlocked when attached transaction is not notarised`() {
        val dummyState = createSimpleDummyState()

        val deleteStateWireTransaction = generateDeleteDummyStateWireTransaction(dummyState)
        val deleteStateSignedTransaction = generateDeleteDummyStateSignedTransaction(deleteStateWireTransaction,
            notarySignature = false)
        val deletedSignedTransactionInputStream = generateSignedTransactionByteArray(deleteStateSignedTransaction)
            .inputStream()

        val uploadedDeletedTransactionSecureHash = aliceNode.services.attachments.importAttachment(
            deletedSignedTransactionInputStream, aliceParty.toString(), null)
        val deletedStateRef: StateRef = deleteStateSignedTransaction.coreTransaction.inputs[0]

        aliceNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReIssuanceLockContract.contractId, reIssuanceLockLabel,
                    contractState=createDummyReIssuanceLock(listOf(createSimpleDummyStateAndRef(deletedStateRef))),
                    encumbrance = 1)
                output(SimpleDummyStateContract.contractId, reIssuedState1Label,
                    contractState=dummyState, encumbrance = 0)
            }

            transaction {
                attachment(uploadedDeletedTransactionSecureHash)
                input(reIssuanceLockLabel)
                input(reIssuedState1Label)
                output(SimpleDummyStateContract.contractId, dummyState)
                command(listOf(aliceParty.owningKey), ReIssuanceLockContract.Commands.Delete())
                command(listOf(aliceParty.owningKey), SimpleDummyStateContract.Commands.Update())
                fails()
            }
        }
    }

    @Test
    fun `Re-issued can't be unlocked when original state ref and state ref in re-issuance lock don't match`() {
        val dummyState = createSimpleDummyState()

        val deleteStateWireTransaction = generateDeleteDummyStateWireTransaction(dummyState)
        val deleteStateSignedTransaction = generateDeleteDummyStateSignedTransaction(deleteStateWireTransaction)
        val deletedSignedTransactionInputStream = generateSignedTransactionByteArray(deleteStateSignedTransaction)
            .inputStream()

        val uploadedDeletedTransactionSecureHash = aliceNode.services.attachments.importAttachment(
            deletedSignedTransactionInputStream, aliceParty.toString(), null)
        val deletedStateRef: StateRef = createDummyRef() // invalid ref

        aliceNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReIssuanceLockContract.contractId, reIssuanceLockLabel,
                    contractState=createDummyReIssuanceLock(listOf(createSimpleDummyStateAndRef(deletedStateRef))),
                    encumbrance = 1)
                output(SimpleDummyStateContract.contractId, reIssuedState1Label,
                    contractState=dummyState, encumbrance = 0)
            }

            transaction {
                attachment(uploadedDeletedTransactionSecureHash)
                input(reIssuanceLockLabel)
                input(reIssuedState1Label)
                output(SimpleDummyStateContract.contractId, dummyState)
                command(listOf(aliceParty.owningKey), ReIssuanceLockContract.Commands.Delete())
                command(listOf(aliceParty.owningKey), SimpleDummyStateContract.Commands.Update())
                fails()
            }
        }
    }

    @Test
    fun `Re-issued state can't be unlocked when attached transaction produces some output`() {
        val dummyState = createSimpleDummyState()

        val deleteStateWireTransaction = generateDeleteDummyStateWireTransaction(dummyState, generateOutput = true)
        val deleteStateSignedTransaction = generateDeleteDummyStateSignedTransaction(deleteStateWireTransaction)
        val deletedSignedTransactionInputStream = generateSignedTransactionByteArray(deleteStateSignedTransaction)
            .inputStream()

        val uploadedDeletedTransactionSecureHash = aliceNode.services.attachments.importAttachment(
            deletedSignedTransactionInputStream, aliceParty.toString(), null)
        val deletedStateRef: StateRef = deleteStateSignedTransaction.coreTransaction.inputs[0]

        aliceNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReIssuanceLockContract.contractId, reIssuanceLockLabel,
                    contractState=createDummyReIssuanceLock(listOf(createSimpleDummyStateAndRef(deletedStateRef))),
                    encumbrance = 1)
                output(SimpleDummyStateContract.contractId, reIssuedState1Label,
                    contractState=dummyState, encumbrance = 0)
            }

            transaction {
                attachment(uploadedDeletedTransactionSecureHash)
                input(reIssuanceLockLabel)
                input(reIssuedState1Label)
                output(SimpleDummyStateContract.contractId, dummyState)
                command(listOf(aliceParty.owningKey), ReIssuanceLockContract.Commands.Delete())
                command(listOf(aliceParty.owningKey), SimpleDummyStateContract.Commands.Update())
                fails()
            }
        }
    }

    @Test
    fun `Re-issued state can't be unlocked without re-issuance lock`() {
        val dummyState = createSimpleDummyState()

        val deleteStateWireTransaction = generateDeleteDummyStateWireTransaction(dummyState)
        val deleteStateSignedTransaction = generateDeleteDummyStateSignedTransaction(deleteStateWireTransaction)
        val deletedSignedTransactionInputStream = generateSignedTransactionByteArray(deleteStateSignedTransaction)
            .inputStream()

        val uploadedDeletedTransactionSecureHash = aliceNode.services.attachments.importAttachment(
            deletedSignedTransactionInputStream, aliceParty.toString(), null)
        val deletedStateRef: StateRef = deleteStateSignedTransaction.coreTransaction.inputs[0]

        aliceNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReIssuanceLockContract.contractId, reIssuanceLockLabel,
                    contractState=createDummyReIssuanceLock(listOf(createSimpleDummyStateAndRef(deletedStateRef))),
                    encumbrance = 1)
                output(SimpleDummyStateContract.contractId, reIssuedState1Label,
                    contractState=dummyState, encumbrance = 0)
            }

            transaction {
                attachment(uploadedDeletedTransactionSecureHash)
                input(reIssuedState1Label)
                output(SimpleDummyStateContract.contractId, dummyState)
                command(listOf(aliceParty.owningKey), SimpleDummyStateContract.Commands.Update())
                fails()
            }
        }
    }

    @Test
    fun `Re-issued state can't be unlocked without consuming the encumbered state`() {
        val dummyState = createSimpleDummyState()

        val deleteStateWireTransaction = generateDeleteDummyStateWireTransaction(dummyState)
        val deleteStateSignedTransaction = generateDeleteDummyStateSignedTransaction(deleteStateWireTransaction)
        val deletedSignedTransactionInputStream = generateSignedTransactionByteArray(deleteStateSignedTransaction)
            .inputStream()

        val uploadedDeletedTransactionSecureHash = aliceNode.services.attachments.importAttachment(
            deletedSignedTransactionInputStream, aliceParty.toString(), null)
        val deletedStateRef: StateRef = deleteStateSignedTransaction.coreTransaction.inputs[0]

        aliceNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReIssuanceLockContract.contractId, reIssuanceLockLabel,
                    contractState=createDummyReIssuanceLock(listOf(createSimpleDummyStateAndRef(deletedStateRef))),
                    encumbrance = 1)
                output(SimpleDummyStateContract.contractId, reIssuedState1Label,
                    contractState=dummyState, encumbrance = 0)
            }

            transaction {
                attachment(uploadedDeletedTransactionSecureHash)
                input(reIssuanceLockLabel)
                output(SimpleDummyStateContract.contractId, dummyState)
                command(listOf(aliceParty.owningKey), ReIssuanceLockContract.Commands.Delete())
                command(listOf(aliceParty.owningKey), SimpleDummyStateContract.Commands.Update())
                fails()
            }
        }
    }

    @Test
    fun `Re-issued state must be unencumbered`() {
        val dummyState = createSimpleDummyState()

        val deleteStateWireTransaction = generateDeleteDummyStateWireTransaction(dummyState)
        val deleteStateSignedTransaction = generateDeleteDummyStateSignedTransaction(deleteStateWireTransaction)
        val deletedSignedTransactionInputStream = generateSignedTransactionByteArray(deleteStateSignedTransaction)
            .inputStream()

        val uploadedDeletedTransactionSecureHash = aliceNode.services.attachments.importAttachment(
            deletedSignedTransactionInputStream, aliceParty.toString(), null)
        val deletedStateRef: StateRef = deleteStateSignedTransaction.coreTransaction.inputs[0]

        aliceNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReIssuanceLockContract.contractId, reIssuanceLockLabel,
                    contractState=createDummyReIssuanceLock(listOf(createSimpleDummyStateAndRef(deletedStateRef))),
                    encumbrance = 1)
                output(SimpleDummyStateContract.contractId, reIssuedState1Label,
                    contractState=dummyState, encumbrance = 0)
            }

            transaction {
                attachment(uploadedDeletedTransactionSecureHash)
                input(reIssuanceLockLabel)
                input(reIssuedState1Label)
                output(SimpleDummyStateContract.contractId, "", contractState=dummyState, encumbrance = 0)
                command(listOf(aliceParty.owningKey), ReIssuanceLockContract.Commands.Delete())
                command(listOf(aliceParty.owningKey), SimpleDummyStateContract.Commands.Update())
                fails()
            }
        }
    }

    private fun generateDeleteDummyStateWireTransaction(
        dummyState: SimpleDummyState,
        generateOutput: Boolean = false
    ): WireTransaction {
        var nullableDeleteStateTransaction: WireTransaction? = null
        aliceNode.services.ledger(notary = notaryParty) {
            nullableDeleteStateTransaction = unverifiedTransaction {
                input(SimpleDummyStateContract.contractId, dummyState)
                command(listOf(aliceParty.owningKey), SimpleDummyStateContract.Commands.Delete())
                if(generateOutput) output(SimpleDummyStateContract.contractId, dummyState)
            }
        }
        require(nullableDeleteStateTransaction != null) { "deleteStateTransaction hasn't been initialized" }
        return nullableDeleteStateTransaction!!
    }

    private fun generateDeleteDummyStateSignedTransaction(
        deleteStateWireTransaction: WireTransaction,
        requesterSignature: Boolean = true,
        notarySignature: Boolean = true
    ): SignedTransaction {
        var sigs = mutableListOf<TransactionSignature>()
        if(requesterSignature) sigs.add(generateTransactionSignature(aliceNode, deleteStateWireTransaction.id))
        if(notarySignature) sigs.add(generateTransactionSignature(notaryNode, deleteStateWireTransaction.id))
        return SignedTransaction(deleteStateWireTransaction, sigs)
    }

    private fun generateTransactionSignature(node: TestStartedNode, txId: SecureHash): TransactionSignature {
        val signatureMetadata = SignatureMetadata(aliceNode.services.myInfo.platformVersion,
            Crypto.findSignatureScheme(aliceParty.owningKey).schemeNumberID)
        val signableData = SignableData(txId, signatureMetadata)
        return node.services.keyManagementService.sign(signableData, node.info.singleIdentity().owningKey)
    }
}
