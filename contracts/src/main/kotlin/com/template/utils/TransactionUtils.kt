package com.template.utils

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
        val entry = ZipEntry("SignedTransaction")
        zos.putNextEntry(entry)
        zos.write(serializedSignedTransactionBytes)
        zos.closeEntry()
    }
    baos.close()

    return baos.toByteArray()
}