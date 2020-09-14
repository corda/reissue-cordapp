package com.template

import com.template.contracts.ReIssuanceLockContract
import com.template.contracts.example.SimpleStateContract
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SignableData
import net.corda.core.crypto.SignatureMetadata
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.testing.node.ledger
import org.junit.Test

class UnlockReIssuedStateTest: AbstractContractTest() {

    @Test
    fun `Re-issued state is unlocked`() {
        val reIssuanceLockLabel = "re-issuance lock"
        val reIssuedStateLabel = "re-issued state encumbered by re-issuance lock"
        val dummyState = createDummyState()

        aliceNode.services.ledger(notary = notaryParty) {
            val deleteStateTransaction: WireTransaction = unverifiedTransaction {
                input(SimpleStateContract.contractId, dummyState)
                command(listOf(aliceParty.owningKey), SimpleStateContract.Commands.Delete())
            }

            val requesterSignatureMetadata = SignatureMetadata(aliceNode.services.myInfo.platformVersion,
                Crypto.findSignatureScheme(aliceParty.owningKey).schemeNumberID)
            val requesterSignableData = SignableData(deleteStateTransaction.id, requesterSignatureMetadata)
            val requesterSig = aliceNode.services.keyManagementService.sign(requesterSignableData, aliceParty.owningKey)

            val notarySignatureMetadata = SignatureMetadata(notaryNode.services.myInfo.platformVersion,
                Crypto.findSignatureScheme(notaryParty.owningKey).schemeNumberID)
            val notarySignableData = SignableData(deleteStateTransaction.id, notarySignatureMetadata)
            val notarySig = notaryNode.services.keyManagementService.sign(notarySignableData, notaryParty.owningKey)

            val deleteDummyStateTransaction = SignedTransaction(deleteStateTransaction, listOf(requesterSig, notarySig))
            val deletedTransactionInputStream = generateSignedTransactionByteArray(deleteDummyStateTransaction).inputStream()

            val uploadedDeletedTransactionSecureHash = aliceNode.services.attachments.importAttachment(
                deletedTransactionInputStream, aliceParty.toString(), null)
            val deletedStateRef: StateRef = deleteStateTransaction.inputs[0]

            unverifiedTransaction {
                output(ReIssuanceLockContract.contractId, reIssuanceLockLabel,
                    contractState=createDummyReIssuanceLock(stateRef = deletedStateRef), encumbrance = 1)
                output(SimpleStateContract.contractId, reIssuedStateLabel,
                    contractState=dummyState, encumbrance = 0)
            }

            transaction {
                attachment(uploadedDeletedTransactionSecureHash)
                input(reIssuanceLockLabel)
                input(reIssuedStateLabel)
                output(SimpleStateContract.contractId, dummyState)
                command(listOf(aliceParty.owningKey), ReIssuanceLockContract.Commands.Delete())
                command(listOf(aliceParty.owningKey), SimpleStateContract.Commands.Update())
                verifies()
            }
        }
    }
}
