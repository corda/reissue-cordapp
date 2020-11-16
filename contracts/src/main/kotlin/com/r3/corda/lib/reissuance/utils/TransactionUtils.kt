package com.r3.corda.lib.reissuance.utils

import net.corda.core.crypto.SecureHash
import net.corda.core.node.ServiceHub
import net.corda.core.serialization.serialize
import net.corda.core.transactions.SignedTransaction
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

fun convertSignedTransactionToByteArray(
    signedTransaction: SignedTransaction
): ByteArray {

    val serializedSignedTransactionBytes = signedTransaction.serialize().bytes

    val baos = ByteArrayOutputStream()
    ZipOutputStream(baos).use { zos ->
        val entry = ZipEntry("SignedTransaction_${signedTransaction.id}")
        zos.putNextEntry(entry)
        zos.write(serializedSignedTransactionBytes)
        zos.closeEntry()
    }
    baos.close()

    return baos.toByteArray()
}

fun findSignedTransactionTrandsactionById(
    serviceHub: ServiceHub,
    transactionId: SecureHash
): SignedTransaction? {
    return serviceHub.validatedTransactions.track().snapshot
        .findLast { it.tx.id == transactionId }
}