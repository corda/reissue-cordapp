package com.r3.corda.lib.reissuance.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.reissuance.utils.convertSignedTransactionToByteArray
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC

@StartableByRPC
class UploadTransactionAsAttachment(
    private val transactionId: SecureHash
): FlowLogic<SecureHash>()  {
    @Suspendable
    override fun call(): SecureHash {
        val signedTransaction = serviceHub.validatedTransactions.track().snapshot
            .findLast { it.tx.id == transactionId }!!
        val transactionByteArray = convertSignedTransactionToByteArray(signedTransaction)
        return serviceHub.attachments.importAttachment(transactionByteArray.inputStream(), ourIdentity.toString(), null)
    }
}
