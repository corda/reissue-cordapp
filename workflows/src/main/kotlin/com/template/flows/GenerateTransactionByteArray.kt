package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.serialization.serialize
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@StartableByRPC
class GenerateTransactionByteArray(
    private val ledgerTransactionId: SecureHash
): FlowLogic<ByteArray>()  {
    @Suspendable
    override fun call(): ByteArray {
        val signedTransaction = serviceHub.validatedTransactions.track().snapshot
            .findLast { it.tx.id == ledgerTransactionId }!!
        val serializedLedgerTransactionBytes = signedTransaction.serialize().bytes

        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            val entry = ZipEntry("SignedTransaction")
            zos.putNextEntry(entry)
            zos.write(serializedLedgerTransactionBytes)
            zos.closeEntry()
        }
        baos.close()

        return baos.toByteArray()
    }
}