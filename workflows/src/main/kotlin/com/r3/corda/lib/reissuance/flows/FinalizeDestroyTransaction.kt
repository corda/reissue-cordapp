package com.r3.corda.lib.reissuance.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.ourIdentity
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.serialization.deserialize
import net.corda.core.transactions.SignedTransaction

@InitiatingFlow
@StartableByRPC
class FinalizeDestroyTransaction(
    private val txAttachmentId: SecureHash
): FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val tx = serviceHub.attachments.openAttachment(txAttachmentId)?.let { attachment ->
            attachment.openAsJAR().use {
                var nextEntry = it.nextEntry
                while (nextEntry != null && !nextEntry.name.startsWith("SignedTransaction")) {
                    // Calling `attachmentJar.nextEntry` causes us to scroll through the JAR.
                    nextEntry = it.nextEntry
                }
                if(nextEntry != null) {
                    it.readBytes().deserialize<SignedTransaction>()
                } else throw IllegalArgumentException("Transaction with id $txAttachmentId not found")
            }
        } ?: throw IllegalArgumentException("Transaction with id $txAttachmentId not found")

        val signers = tx.requiredSigningKeys.filter { it != serviceHub.ourIdentity.owningKey }.mapNotNull {
            serviceHub.identityService.partyFromKey(it)
        }

        val signersSessions = subFlow(GenerateRequiredFlowSessions(signers))

        val sameHostSigners = tx.requiredSigningKeys.filter { pk ->
            !tx.sigs.map { it.by }.contains(pk)
        }.filter { pk ->
            serviceHub.identityService.partyFromKey(pk) == serviceHub.ourIdentity
        }

        var signedTx = tx
        sameHostSigners.forEach {
            signedTx = serviceHub.addSignature(signedTx, it)
        }

        val fullySignedTransaction = subFlow(CollectSignaturesFlow(signedTx, signersSessions.filter { it.counterparty != tx.notary }))

        return subFlow(FinalityFlow(
            fullySignedTransaction, signersSessions
        ))
    }
}

@InitiatedBy(FinalizeDestroyTransaction::class)
class FinalizeDestroyTransactionResponder(
    private val otherSession: FlowSession
) : FlowLogic<SignedTransaction>()  {
    @Suspendable
    override fun call(): SignedTransaction {

        subFlow(object : SignTransactionFlow(otherSession) {
            override fun checkTransaction(stx: SignedTransaction) { }
        })
        return subFlow(
            ReceiveFinalityFlow(
                otherSession
            )
        )
    }
}