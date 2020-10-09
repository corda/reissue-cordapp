package com.r3.corda.lib.reissuance.dummy_flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord

@InitiatingFlow
@StartableByRPC
class ShareTransactionWithIssuer(
    private val issuer: Party,
    private val transactionId: SecureHash
): FlowLogic<Unit>() {

    @Suspendable
    override fun call() {
        val signedTransaction = serviceHub.validatedTransactions.getTransaction(transactionId)!!
        val sendToSession = initiateFlow(issuer)
        subFlow(SendTransactionFlow(sendToSession, signedTransaction))
    }
}

@InitiatedBy(ShareTransactionWithIssuer::class)
class SendSignedTransactionResponder(val otherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(ReceiveTransactionFlow(
            otherSideSession = otherSession,
            statesToRecord = StatesToRecord.ALL_VISIBLE
        ))
    }
}
