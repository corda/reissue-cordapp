package com.r3.corda.lib.reissuance.utils

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.AttachmentId
import net.corda.core.serialization.deserialize
import net.corda.core.transactions.SignedTransaction

@Suspendable
fun getSignedTransactionFromAttachment(serviceHub: ServiceHub, txAttachmentId: AttachmentId): SignedTransaction {
    return serviceHub.attachments.openAttachment(txAttachmentId)?.let { attachment ->
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
}