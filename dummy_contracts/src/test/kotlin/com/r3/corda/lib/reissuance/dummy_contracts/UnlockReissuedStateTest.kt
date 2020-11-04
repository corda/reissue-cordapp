package com.r3.corda.lib.reissuance.dummy_contracts

import com.r3.corda.lib.reissuance.contracts.ReissuanceLockContract
import com.r3.corda.lib.reissuance.states.ReissuanceLock
import com.r3.corda.lib.reissuance.dummy_states.SimpleDummyState
import com.r3.corda.lib.tokens.contracts.FungibleTokenContract
import com.r3.corda.lib.tokens.contracts.commands.MoveTokenCommand
import com.r3.corda.lib.tokens.contracts.commands.RedeemTokenCommand
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.internal.TestStartedNode
import net.corda.testing.node.ledger
import org.junit.Test

class UnlockReissuedStateTest: AbstractContractTest() {

    @Test
    fun `Re-issued state is unlocked without issuer signature when issuer is not required signer`() {
        val dummyState = createSimpleDummyState()

        val deleteStateWireTransaction = generateDeleteSimpleDummyStateWireTransaction(dummyState)
        val deleteStateSignedTransaction = generateSignedTransaction(deleteStateWireTransaction)
        val deletedSignedTransactionInputStream = generateSignedTransactionByteArrayInputStream(deleteStateSignedTransaction)

        val uploadedDeletedTransactionSecureHash = aliceNode.services.attachments.importAttachment(
            deletedSignedTransactionInputStream, aliceParty.toString(), null)
        val deletedStateRef: StateRef = deleteStateSignedTransaction.coreTransaction.inputs[0]

        val reissuanceLock = createDummyReissuanceLock(listOf(createSimpleDummyStateAndRef(deletedStateRef)),
            issuerIsRequiredSigner=false)

        aliceNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReissuanceLockContract.contractId, reissuanceLockLabel,
                    contractState=reissuanceLock, encumbrance = 1)
                output(SimpleDummyStateContract.contractId, reissuedStateLabel,
                    contractState=dummyState, encumbrance = 0)
            }

            transaction {
                attachment(uploadedDeletedTransactionSecureHash)
                input(reissuanceLockLabel)
                output(ReissuanceLockContract.contractId,
                    reissuanceLock.copy(status = ReissuanceLock.ReissuanceLockStatus.INACTIVE))
                input(reissuedStateLabel)
                output(SimpleDummyStateContract.contractId, dummyState)
                command(listOf(aliceParty.owningKey), ReissuanceLockContract.Commands.Deactivate())
                command(listOf(aliceParty.owningKey), SimpleDummyStateContract.Commands.Update())
                verifies()
            }
        }
    }

    @Test
    fun `Re-issued state is unlocked with issuer signature when issuer is required signer`() {
        val dummyState = createSimpleDummyState()

        val deleteStateWireTransaction = generateDeleteSimpleDummyStateWireTransaction(dummyState,
            issuerSignature = true)
        val deleteStateSignedTransaction = generateSignedTransaction(deleteStateWireTransaction, issuerSignature = true)
        val deletedSignedTransactionInputStream = generateSignedTransactionByteArrayInputStream(deleteStateSignedTransaction)

        val uploadedDeletedTransactionSecureHash = aliceNode.services.attachments.importAttachment(
            deletedSignedTransactionInputStream, aliceParty.toString(), null)
        val deletedStateRef: StateRef = deleteStateSignedTransaction.coreTransaction.inputs[0]

        val reissuanceLock = createDummyReissuanceLock(listOf(createSimpleDummyStateAndRef(deletedStateRef)),
            issuerIsRequiredSigner=true)

        aliceNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReissuanceLockContract.contractId, reissuanceLockLabel,
                    contractState=reissuanceLock, encumbrance = 1)
                output(SimpleDummyStateContract.contractId, reissuedStateLabel,
                    contractState=dummyState, encumbrance = 0)
            }

            transaction {
                attachment(uploadedDeletedTransactionSecureHash)
                input(reissuanceLockLabel)
                output(ReissuanceLockContract.contractId,
                    reissuanceLock.copy(status = ReissuanceLock.ReissuanceLockStatus.INACTIVE))
                input(reissuedStateLabel)
                output(SimpleDummyStateContract.contractId, dummyState)
                command(listOf(aliceParty.owningKey), ReissuanceLockContract.Commands.Deactivate())
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

        val reissuanceLock = createDummyReissuanceLock(tokenStateAndRefs, issuerIsRequiredSigner=false)
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
                attachment(uploadedDeletedTransactionSecureHash)
                tokenIndices.forEach {
                    input(reissuedStateLabel(it))
                }
                tokens.map {
                    output(FungibleTokenContract.contractId, it)
                }

                input(reissuanceLockLabel)
                output(ReissuanceLockContract.contractId,
                    reissuanceLock.copy(status = ReissuanceLock.ReissuanceLockStatus.INACTIVE))

                command(listOf(aliceParty.owningKey), ReissuanceLockContract.Commands.Deactivate())
                command(listOf(aliceParty.owningKey), MoveTokenCommand(issuedTokenType, tokenIndices, tokenIndices))
                verifies()
            }
        }
    }

    @Test
    fun `Re-issued state can't be unlocked when issuer signature is required and missing`() {
        val dummyState = createSimpleDummyState()

        val deleteStateWireTransaction = generateDeleteSimpleDummyStateWireTransaction(dummyState)
        val deleteStateSignedTransaction = generateSignedTransaction(deleteStateWireTransaction)
        val deletedSignedTransactionInputStream = generateSignedTransactionByteArrayInputStream(deleteStateSignedTransaction)

        val uploadedDeletedTransactionSecureHash = aliceNode.services.attachments.importAttachment(
            deletedSignedTransactionInputStream, aliceParty.toString(), null)
        val deletedStateRef: StateRef = deleteStateSignedTransaction.coreTransaction.inputs[0]

        val reissuanceLock = createDummyReissuanceLock(listOf(createSimpleDummyStateAndRef(deletedStateRef)),
            issuerIsRequiredSigner=true)

        aliceNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReissuanceLockContract.contractId, reissuanceLockLabel,
                    contractState=reissuanceLock, encumbrance = 1)
                output(SimpleDummyStateContract.contractId, reissuedStateLabel,
                    contractState=dummyState, encumbrance = 0)
            }

            transaction {
                attachment(uploadedDeletedTransactionSecureHash)
                input(reissuanceLockLabel)
                output(ReissuanceLockContract.contractId,
                    reissuanceLock.copy(status = ReissuanceLock.ReissuanceLockStatus.INACTIVE))
                input(reissuedStateLabel)
                output(SimpleDummyStateContract.contractId, dummyState)
                command(listOf(aliceParty.owningKey), ReissuanceLockContract.Commands.Deactivate())
                command(listOf(aliceParty.owningKey), SimpleDummyStateContract.Commands.Update())
                fails()
            }
        }
    }

    @Test
    fun `Re-issued state can't unlocked if output ReissuanceLock status is ACTIVE`() {
        val dummyState = createSimpleDummyState()

        val deleteStateWireTransaction = generateDeleteSimpleDummyStateWireTransaction(dummyState)
        val deleteStateSignedTransaction = generateSignedTransaction(deleteStateWireTransaction)
        val deletedSignedTransactionInputStream = generateSignedTransactionByteArrayInputStream(deleteStateSignedTransaction)

        val uploadedDeletedTransactionSecureHash = aliceNode.services.attachments.importAttachment(
            deletedSignedTransactionInputStream, aliceParty.toString(), null)
        val deletedStateRef: StateRef = deleteStateSignedTransaction.coreTransaction.inputs[0]

        val reissuanceLock = createDummyReissuanceLock(listOf(createSimpleDummyStateAndRef(deletedStateRef)),
            issuerIsRequiredSigner=false)

        aliceNode.services.ledger(notary = notaryParty) {
            unverifiedTransaction {
                output(ReissuanceLockContract.contractId, reissuanceLockLabel,
                    contractState=reissuanceLock, encumbrance = 1)
                output(SimpleDummyStateContract.contractId, reissuedStateLabel,
                    contractState=dummyState, encumbrance = 0)
            }

            transaction {
                attachment(uploadedDeletedTransactionSecureHash)
                input(reissuanceLockLabel)
                output(ReissuanceLockContract.contractId,
                    reissuanceLock.copy(status = ReissuanceLock.ReissuanceLockStatus.ACTIVE))
                input(reissuedStateLabel)
                output(SimpleDummyStateContract.contractId, dummyState)
                command(listOf(aliceParty.owningKey), ReissuanceLockContract.Commands.Deactivate())
                command(listOf(aliceParty.owningKey), SimpleDummyStateContract.Commands.Update())
                fails()
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
                output(ReissuanceLockContract.contractId, reissuanceLockLabel,
                    contractState=createDummyReissuanceLock(listOf(createSimpleDummyStateAndRef(deletedStateRef)),
                        issuerIsRequiredSigner=false),
                    encumbrance = 1)
                output(SimpleDummyStateContract.contractId, reissuedStateLabel,
                    contractState=dummyState, encumbrance = 0)
            }

            transaction {
                input(reissuanceLockLabel)
                input(reissuedStateLabel)
                output(SimpleDummyStateContract.contractId, dummyState)
                command(listOf(aliceParty.owningKey), ReissuanceLockContract.Commands.Deactivate())
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
                output(ReissuanceLockContract.contractId, reissuanceLockLabel,
                    contractState=createDummyReissuanceLock(listOf(createSimpleDummyStateAndRef(deletedStateRef)),
                        issuerIsRequiredSigner=false),
                    encumbrance = 1)
                output(SimpleDummyStateContract.contractId, reissuedStateLabel,
                    contractState=dummyState, encumbrance = 0)
            }

            transaction {
                attachment(uploadedDeletedTransactionSecureHash)
                input(reissuanceLockLabel)
                input(reissuedStateLabel)
                output(SimpleDummyStateContract.contractId, dummyState)
                command(listOf(aliceParty.owningKey), ReissuanceLockContract.Commands.Deactivate())
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
                output(ReissuanceLockContract.contractId, reissuanceLockLabel,
                    contractState=createDummyReissuanceLock(listOf(createSimpleDummyStateAndRef(deletedStateRef)),
                        issuerIsRequiredSigner=false),
                    encumbrance = 1)
                output(SimpleDummyStateContract.contractId, reissuedStateLabel,
                    contractState=dummyState, encumbrance = 0)
            }

            transaction {
                attachment(uploadedDeletedTransactionSecureHash)
                input(reissuanceLockLabel)
                input(reissuedStateLabel)
                output(SimpleDummyStateContract.contractId, dummyState)
                command(listOf(aliceParty.owningKey), ReissuanceLockContract.Commands.Deactivate())
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
                output(ReissuanceLockContract.contractId, reissuanceLockLabel,
                    contractState=createDummyReissuanceLock(listOf(createSimpleDummyStateAndRef(deletedStateRef)),
                        issuerIsRequiredSigner=false),
                    encumbrance = 1)
                output(SimpleDummyStateContract.contractId, reissuedStateLabel,
                    contractState=dummyState, encumbrance = 0)
            }

            transaction {
                attachment(uploadedDeletedTransactionSecureHash)
                input(reissuanceLockLabel)
                input(reissuedStateLabel)
                output(SimpleDummyStateContract.contractId, dummyState)
                command(listOf(aliceParty.owningKey), ReissuanceLockContract.Commands.Deactivate())
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
                output(ReissuanceLockContract.contractId, reissuanceLockLabel,
                    contractState=createDummyReissuanceLock(listOf(createSimpleDummyStateAndRef(deletedStateRef)),
                        issuerIsRequiredSigner=false),
                    encumbrance = 1)
                output(SimpleDummyStateContract.contractId, reissuedStateLabel,
                    contractState=dummyState, encumbrance = 0)
            }

            transaction {
                attachment(uploadedDeletedTransactionSecureHash)
                input(reissuedStateLabel)
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
                output(ReissuanceLockContract.contractId, reissuanceLockLabel,
                    contractState=createDummyReissuanceLock(listOf(createSimpleDummyStateAndRef(deletedStateRef)),
                        issuerIsRequiredSigner=false),
                    encumbrance = 1)
                output(SimpleDummyStateContract.contractId, reissuedStateLabel,
                    contractState=dummyState, encumbrance = 0)
            }

            transaction {
                attachment(uploadedDeletedTransactionSecureHash)
                input(reissuanceLockLabel)
                output(SimpleDummyStateContract.contractId, dummyState)
                command(listOf(aliceParty.owningKey), ReissuanceLockContract.Commands.Deactivate())
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
                output(ReissuanceLockContract.contractId, reissuanceLockLabel,
                    contractState=createDummyReissuanceLock(listOf(createSimpleDummyStateAndRef(deletedStateRef)),
                        issuerIsRequiredSigner=false),
                    encumbrance = 1)
                output(SimpleDummyStateContract.contractId, reissuedStateLabel,
                    contractState=dummyState, encumbrance = 0)
            }

            transaction {
                attachment(uploadedDeletedTransactionSecureHash)
                input(reissuanceLockLabel)
                input(reissuedStateLabel)
                output(SimpleDummyStateContract.contractId, "", contractState=dummyState, encumbrance = 0)
                command(listOf(aliceParty.owningKey), ReissuanceLockContract.Commands.Deactivate())
                command(listOf(aliceParty.owningKey), SimpleDummyStateContract.Commands.Update())
                fails()
            }
        }
    }

    private fun generateDeleteSimpleDummyStateWireTransaction(
        simpleDummyState: SimpleDummyState,
        generateOutput: Boolean = false,
        requesterSignature: Boolean = true,
        issuerSignature: Boolean = false
    ): WireTransaction {
        val commandSigners = listOfNotNull(
            if(requesterSignature) aliceParty.owningKey else null,
            if(issuerSignature) issuerParty.owningKey else null
        )
        var nullableDeleteStateTransaction: WireTransaction? = null
        aliceNode.services.ledger(notary = notaryParty) {
            nullableDeleteStateTransaction = unverifiedTransaction {
                input(SimpleDummyStateContract.contractId, simpleDummyState)
                command(commandSigners, SimpleDummyStateContract.Commands.Delete())
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
        issuerSignature: Boolean = false,
        notarySignature: Boolean = true
    ): SignedTransaction {
        var sigs = mutableListOf<TransactionSignature>()
        if(requesterSignature) sigs.add(generateTransactionSignature(aliceNode, deleteStateWireTransaction.id))
        if(issuerSignature) sigs.add(generateTransactionSignature(issuerNode, deleteStateWireTransaction.id))
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
