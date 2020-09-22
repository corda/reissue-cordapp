package com.template.contracts

import com.r3.corda.lib.tokens.contracts.FungibleTokenContract
import com.r3.corda.lib.tokens.contracts.commands.MoveTokenCommand
import com.r3.corda.lib.tokens.contracts.commands.RedeemTokenCommand
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.template.contracts.example.SimpleDummyStateContract
import com.template.states.ReIssuanceLock
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

        val deleteStateWireTransaction = generateDeleteSimpleDummyStateWireTransaction(dummyState)
        val deleteStateSignedTransaction = generateSignedTransaction(deleteStateWireTransaction)
        val deletedSignedTransactionInputStream = generateSignedTransactionByteArrayInputStream(deleteStateSignedTransaction)

        val uploadedDeletedTransactionSecureHash = aliceNode.services.attachments.importAttachment(
            deletedSignedTransactionInputStream, aliceParty.toString(), null)
        val deletedStateRef: StateRef = deleteStateSignedTransaction.coreTransaction.inputs[0]

        val reIssuanceLock = createDummyReIssuanceLock(listOf(createSimpleDummyStateAndRef(deletedStateRef)))

        aliceNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReIssuanceLockContract.contractId, reIssuanceLockLabel,
                    contractState=reIssuanceLock, encumbrance = 1)
                output(SimpleDummyStateContract.contractId, reIssuedStateLabel,
                    contractState=dummyState, encumbrance = 0)
            }

            transaction {
                attachment(uploadedDeletedTransactionSecureHash)
                input(reIssuanceLockLabel)
                output(ReIssuanceLockContract.contractId,
                    reIssuanceLock.copy(status = ReIssuanceLock.ReIssuanceLockStatus.INACTIVE))
                input(reIssuedStateLabel)
                output(SimpleDummyStateContract.contractId, dummyState)
                command(listOf(aliceParty.owningKey), ReIssuanceLockContract.Commands.Use())
                command(listOf(aliceParty.owningKey), SimpleDummyStateContract.Commands.Update())
                verifies()
            }
        }
    }

    @Test
    fun `Many re-issued states are unlocked`() {
        val tokens = listOf(createToken(), createToken())
        val tokenIndices = tokens.indices.toList()

        val deleteStateWireTransaction = generateDeleteTokensWireTransaction(tokens)
        val deleteStateSignedTransaction = generateSignedTransaction(deleteStateWireTransaction)
        val deletedSignedTransactionInputStream = generateSignedTransactionByteArrayInputStream(deleteStateSignedTransaction)

        val uploadedDeletedTransactionSecureHash = aliceNode.services.attachments.importAttachment(
            deletedSignedTransactionInputStream, aliceParty.toString(), null)
        val deletedStateRefs: List<StateRef> = deleteStateSignedTransaction.coreTransaction.inputs
        val tokenStateAndRefs = deletedStateRefs.map { createTokenStateAndRef(it) }

        val reIssuanceLock = createDummyReIssuanceLock(tokenStateAndRefs)
        aliceNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReIssuanceLockContract.contractId, reIssuanceLockLabel,
                    contractState=reIssuanceLock, encumbrance = 1)
                tokens.forEachIndexed { idx, token ->
                    output(FungibleTokenContract.contractId, reIssuedStateLabel(idx),
                        contractState=token, encumbrance = 0)
                }
            }

            transaction {
                attachment(uploadedDeletedTransactionSecureHash)
                tokenIndices.forEach {
                    input(reIssuedStateLabel(it))
                }
                tokens.map {
                    output(FungibleTokenContract.contractId, it)
                }

                input(reIssuanceLockLabel)
                output(ReIssuanceLockContract.contractId,
                    reIssuanceLock.copy(status = ReIssuanceLock.ReIssuanceLockStatus.INACTIVE))

                command(listOf(aliceParty.owningKey), ReIssuanceLockContract.Commands.Use())
                command(listOf(aliceParty.owningKey), MoveTokenCommand(issuedTokenType, tokenIndices, tokenIndices))
                verifies()
            }
        }
    }

    @Test
    fun `Re-issued state can't be unlocked without attached transaction proving that the original state had been deleted`() {
        val dummyState = createSimpleDummyState()

        val deleteStateWireTransaction = generateDeleteSimpleDummyStateWireTransaction(dummyState)
        val deleteStateSignedTransaction = generateSignedTransaction(deleteStateWireTransaction)

        val deletedStateRef: StateRef = deleteStateSignedTransaction.coreTransaction.inputs[0]

        aliceNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReIssuanceLockContract.contractId, reIssuanceLockLabel,
                    contractState=createDummyReIssuanceLock(listOf(createSimpleDummyStateAndRef(deletedStateRef))),
                    encumbrance = 1)
                output(SimpleDummyStateContract.contractId, reIssuedStateLabel,
                    contractState=dummyState, encumbrance = 0)
            }

            transaction {
                input(reIssuanceLockLabel)
                input(reIssuedStateLabel)
                output(SimpleDummyStateContract.contractId, dummyState)
                command(listOf(aliceParty.owningKey), ReIssuanceLockContract.Commands.Use())
                command(listOf(aliceParty.owningKey), SimpleDummyStateContract.Commands.Update())
                fails()
            }
        }
    }

    @Test
    fun `Re-issued state can't be unlocked when attached transaction is not notarised`() {
        val dummyState = createSimpleDummyState()

        val deleteStateWireTransaction = generateDeleteSimpleDummyStateWireTransaction(dummyState)
        val deleteStateSignedTransaction = generateSignedTransaction(deleteStateWireTransaction,
            notarySignature = false)
        val deletedSignedTransactionInputStream = generateSignedTransactionByteArrayInputStream(deleteStateSignedTransaction)

        val uploadedDeletedTransactionSecureHash = aliceNode.services.attachments.importAttachment(
            deletedSignedTransactionInputStream, aliceParty.toString(), null)
        val deletedStateRef: StateRef = deleteStateSignedTransaction.coreTransaction.inputs[0]

        aliceNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReIssuanceLockContract.contractId, reIssuanceLockLabel,
                    contractState=createDummyReIssuanceLock(listOf(createSimpleDummyStateAndRef(deletedStateRef))),
                    encumbrance = 1)
                output(SimpleDummyStateContract.contractId, reIssuedStateLabel,
                    contractState=dummyState, encumbrance = 0)
            }

            transaction {
                attachment(uploadedDeletedTransactionSecureHash)
                input(reIssuanceLockLabel)
                input(reIssuedStateLabel)
                output(SimpleDummyStateContract.contractId, dummyState)
                command(listOf(aliceParty.owningKey), ReIssuanceLockContract.Commands.Use())
                command(listOf(aliceParty.owningKey), SimpleDummyStateContract.Commands.Update())
                fails()
            }
        }
    }

    @Test
    fun `Re-issued can't be unlocked when original state ref and state ref in re-issuance lock don't match`() {
        val dummyState = createSimpleDummyState()

        val deleteStateWireTransaction = generateDeleteSimpleDummyStateWireTransaction(dummyState)
        val deleteStateSignedTransaction = generateSignedTransaction(deleteStateWireTransaction)
        val deletedSignedTransactionInputStream = generateSignedTransactionByteArrayInputStream(deleteStateSignedTransaction)

        val uploadedDeletedTransactionSecureHash = aliceNode.services.attachments.importAttachment(
            deletedSignedTransactionInputStream, aliceParty.toString(), null)
        val deletedStateRef: StateRef = createDummyRef() // invalid ref

        aliceNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReIssuanceLockContract.contractId, reIssuanceLockLabel,
                    contractState=createDummyReIssuanceLock(listOf(createSimpleDummyStateAndRef(deletedStateRef))),
                    encumbrance = 1)
                output(SimpleDummyStateContract.contractId, reIssuedStateLabel,
                    contractState=dummyState, encumbrance = 0)
            }

            transaction {
                attachment(uploadedDeletedTransactionSecureHash)
                input(reIssuanceLockLabel)
                input(reIssuedStateLabel)
                output(SimpleDummyStateContract.contractId, dummyState)
                command(listOf(aliceParty.owningKey), ReIssuanceLockContract.Commands.Use())
                command(listOf(aliceParty.owningKey), SimpleDummyStateContract.Commands.Update())
                fails()
            }
        }
    }

    @Test
    fun `Re-issued state can't be unlocked when attached transaction produces some output`() {
        val dummyState = createSimpleDummyState()

        val deleteStateWireTransaction = generateDeleteSimpleDummyStateWireTransaction(dummyState, generateOutput = true)
        val deleteStateSignedTransaction = generateSignedTransaction(deleteStateWireTransaction)
        val deletedSignedTransactionInputStream = generateSignedTransactionByteArrayInputStream(deleteStateSignedTransaction)

        val uploadedDeletedTransactionSecureHash = aliceNode.services.attachments.importAttachment(
            deletedSignedTransactionInputStream, aliceParty.toString(), null)
        val deletedStateRef: StateRef = deleteStateSignedTransaction.coreTransaction.inputs[0]

        aliceNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReIssuanceLockContract.contractId, reIssuanceLockLabel,
                    contractState=createDummyReIssuanceLock(listOf(createSimpleDummyStateAndRef(deletedStateRef))),
                    encumbrance = 1)
                output(SimpleDummyStateContract.contractId, reIssuedStateLabel,
                    contractState=dummyState, encumbrance = 0)
            }

            transaction {
                attachment(uploadedDeletedTransactionSecureHash)
                input(reIssuanceLockLabel)
                input(reIssuedStateLabel)
                output(SimpleDummyStateContract.contractId, dummyState)
                command(listOf(aliceParty.owningKey), ReIssuanceLockContract.Commands.Use())
                command(listOf(aliceParty.owningKey), SimpleDummyStateContract.Commands.Update())
                fails()
            }
        }
    }

    @Test
    fun `Re-issued state can't be unlocked without re-issuance lock`() {
        val dummyState = createSimpleDummyState()

        val deleteStateWireTransaction = generateDeleteSimpleDummyStateWireTransaction(dummyState)
        val deleteStateSignedTransaction = generateSignedTransaction(deleteStateWireTransaction)
        val deletedSignedTransactionInputStream = generateSignedTransactionByteArrayInputStream(deleteStateSignedTransaction)

        val uploadedDeletedTransactionSecureHash = aliceNode.services.attachments.importAttachment(
            deletedSignedTransactionInputStream, aliceParty.toString(), null)
        val deletedStateRef: StateRef = deleteStateSignedTransaction.coreTransaction.inputs[0]

        aliceNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReIssuanceLockContract.contractId, reIssuanceLockLabel,
                    contractState=createDummyReIssuanceLock(listOf(createSimpleDummyStateAndRef(deletedStateRef))),
                    encumbrance = 1)
                output(SimpleDummyStateContract.contractId, reIssuedStateLabel,
                    contractState=dummyState, encumbrance = 0)
            }

            transaction {
                attachment(uploadedDeletedTransactionSecureHash)
                input(reIssuedStateLabel)
                output(SimpleDummyStateContract.contractId, dummyState)
                command(listOf(aliceParty.owningKey), SimpleDummyStateContract.Commands.Update())
                fails()
            }
        }
    }

    @Test
    fun `Re-issued state can't be unlocked without consuming the encumbered state`() {
        val dummyState = createSimpleDummyState()

        val deleteStateWireTransaction = generateDeleteSimpleDummyStateWireTransaction(dummyState)
        val deleteStateSignedTransaction = generateSignedTransaction(deleteStateWireTransaction)
        val deletedSignedTransactionInputStream = generateSignedTransactionByteArrayInputStream(deleteStateSignedTransaction)

        val uploadedDeletedTransactionSecureHash = aliceNode.services.attachments.importAttachment(
            deletedSignedTransactionInputStream, aliceParty.toString(), null)
        val deletedStateRef: StateRef = deleteStateSignedTransaction.coreTransaction.inputs[0]

        aliceNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReIssuanceLockContract.contractId, reIssuanceLockLabel,
                    contractState=createDummyReIssuanceLock(listOf(createSimpleDummyStateAndRef(deletedStateRef))),
                    encumbrance = 1)
                output(SimpleDummyStateContract.contractId, reIssuedStateLabel,
                    contractState=dummyState, encumbrance = 0)
            }

            transaction {
                attachment(uploadedDeletedTransactionSecureHash)
                input(reIssuanceLockLabel)
                output(SimpleDummyStateContract.contractId, dummyState)
                command(listOf(aliceParty.owningKey), ReIssuanceLockContract.Commands.Use())
                command(listOf(aliceParty.owningKey), SimpleDummyStateContract.Commands.Update())
                fails()
            }
        }
    }

    @Test
    fun `Re-issued state must be unencumbered`() {
        val dummyState = createSimpleDummyState()

        val deleteStateWireTransaction = generateDeleteSimpleDummyStateWireTransaction(dummyState)
        val deleteStateSignedTransaction = generateSignedTransaction(deleteStateWireTransaction)
        val deletedSignedTransactionInputStream = generateSignedTransactionByteArrayInputStream(deleteStateSignedTransaction)

        val uploadedDeletedTransactionSecureHash = aliceNode.services.attachments.importAttachment(
            deletedSignedTransactionInputStream, aliceParty.toString(), null)
        val deletedStateRef: StateRef = deleteStateSignedTransaction.coreTransaction.inputs[0]

        aliceNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReIssuanceLockContract.contractId, reIssuanceLockLabel,
                    contractState=createDummyReIssuanceLock(listOf(createSimpleDummyStateAndRef(deletedStateRef))),
                    encumbrance = 1)
                output(SimpleDummyStateContract.contractId, reIssuedStateLabel,
                    contractState=dummyState, encumbrance = 0)
            }

            transaction {
                attachment(uploadedDeletedTransactionSecureHash)
                input(reIssuanceLockLabel)
                input(reIssuedStateLabel)
                output(SimpleDummyStateContract.contractId, "", contractState=dummyState, encumbrance = 0)
                command(listOf(aliceParty.owningKey), ReIssuanceLockContract.Commands.Use())
                command(listOf(aliceParty.owningKey), SimpleDummyStateContract.Commands.Update())
                fails()
            }
        }
    }

    private fun generateDeleteSimpleDummyStateWireTransaction(
        simpleDummyState: SimpleDummyState,
        generateOutput: Boolean = false
    ): WireTransaction {
        var nullableDeleteStateTransaction: WireTransaction? = null
        aliceNode.services.ledger(notary = notaryParty) {
            nullableDeleteStateTransaction = unverifiedTransaction {
                input(SimpleDummyStateContract.contractId, simpleDummyState)
                command(listOf(aliceParty.owningKey), SimpleDummyStateContract.Commands.Delete())
                if(generateOutput) output(SimpleDummyStateContract.contractId, simpleDummyState)
            }
        }
        return nullableDeleteStateTransaction!!
    }

    private fun generateDeleteTokensWireTransaction(
        tokens: List<FungibleToken>
    ): WireTransaction {
        var nullableDeleteStateTransaction: WireTransaction? = null
        aliceNode.services.ledger(notary = notaryParty) {
            nullableDeleteStateTransaction = unverifiedTransaction {
                tokens.forEach {
                    input(FungibleTokenContract.contractId, it)
                }
                command(listOf(aliceParty.owningKey), RedeemTokenCommand(issuedTokenType, tokens.indices.toList(), listOf()))
            }
        }
        return nullableDeleteStateTransaction!!
    }

    private fun generateSignedTransaction(
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
