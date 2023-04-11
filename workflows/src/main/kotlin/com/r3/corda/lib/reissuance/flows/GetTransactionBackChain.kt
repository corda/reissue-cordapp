package com.r3.corda.lib.reissuance.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC

@StartableByRPC
class GetTransactionBackChain(
    private val transactionId: SecureHash
): FlowLogic<Set<SecureHash>>() {

    @Suspendable
    override fun call(): Set<SecureHash> {
        val visitedTransactions = mutableSetOf<SecureHash>()
        val transactionsToVisitQueue = mutableSetOf<SecureHash>(transactionId)
        return getTransactionBackChain(transactionId, visitedTransactions, transactionsToVisitQueue)
    }

    private tailrec fun getTransactionBackChain(
        transactionId: SecureHash,
        visitedTransactions: MutableSet<SecureHash>,
        transactionsToVisit: MutableSet<SecureHash>
    ): Set<SecureHash> {
        val inputs = serviceHub.validatedTransactions.getTransaction(transactionId)?.inputs ?: run {
            serviceHub.validatedTransactions.getEncryptedTransaction(transactionId)?.let { encryptedTx ->
                    serviceHub.encryptedTransactionService.decryptInputAndRefsForNode(encryptedTx).inputs.map { it.ref }
            }
        } ?: throw BackChainException("Cannot find transaction with id $transactionId")

        transactionsToVisit.remove(transactionId)
        visitedTransactions.add(transactionId)

        transactionsToVisit.addAll(inputs.map { it.txhash })

        return if(transactionsToVisit.isEmpty())
            visitedTransactions
        else getTransactionBackChain(transactionsToVisit.elementAt(0), visitedTransactions,
            transactionsToVisit)
    }

}
