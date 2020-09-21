package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.utils.convertSignedTransactionToByteArray
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC

@StartableByRPC
class GenerateTransactionByteArray(
    private val transactionId: SecureHash
): FlowLogic<ByteArray>()  {
    @Suspendable
    override fun call(): ByteArray {
        val signedTransaction = serviceHub.validatedTransactions.track().snapshot
            .findLast { it.tx.id == transactionId }!!
        return convertSignedTransactionToByteArray(signedTransaction)
    }
}
