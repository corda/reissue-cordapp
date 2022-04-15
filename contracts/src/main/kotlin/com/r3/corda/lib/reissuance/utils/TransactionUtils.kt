package com.r3.corda.lib.reissuance.utils

import net.corda.core.serialization.serialize
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.WireTransaction
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

fun convertWireTransactionToByteArray(
    wireTransaction: WireTransaction
): ByteArray {

    val serializedWireTransactionBytes = wireTransaction.serialize().bytes

    val baos = ByteArrayOutputStream()
    ZipOutputStream(baos).use { zos ->
        val entry = ZipEntry("WireTransaction${wireTransaction.id}")
        zos.putNextEntry(entry)
        zos.write(serializedWireTransactionBytes)
        zos.closeEntry()
    }
    baos.close()

    return baos.toByteArray()
}
