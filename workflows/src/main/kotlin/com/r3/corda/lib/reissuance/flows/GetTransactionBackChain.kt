package com.r3.corda.lib.reissuance.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.FlowException
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

    private fun getTransactionBackChain(
        transactionId: SecureHash,
        visitedTransactions: MutableSet<SecureHash>,
        transactionsToVisit: MutableSet<SecureHash>
    ): Set<SecureHash> {
        val signedTransaction = serviceHub.validatedTransactions.getTransaction(transactionId)
            ?: throw BackChainException("Cannot find transaction with id $transactionId")

        transactionsToVisit.remove(transactionId)
        visitedTransactions.add(transactionId)

        transactionsToVisit.addAll(signedTransaction.inputs.map { it.txhash })

        if(transactionsToVisit.isEmpty())
            return visitedTransactions
        return getTransactionBackChain(transactionsToVisit.elementAt(0), visitedTransactions,
            transactionsToVisit)
    }

    class BackChainException(
        message: String, cause: Throwable? = null
    ) : FlowException(message, cause)

}
