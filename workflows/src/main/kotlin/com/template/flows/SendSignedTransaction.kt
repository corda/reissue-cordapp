package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction

@InitiatingFlow
@StartableByRPC
class SendSignedTransaction(
    val sendTo: AbstractParty,
    val signedTransaction: SignedTransaction
) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {
        val sendToHost = serviceHub.identityService.partyFromKey(sendTo.owningKey)!!
        val sendToSession = initiateFlow(sendToHost)
        subFlow(SendTransactionFlow(sendToSession, signedTransaction))
    }

}

@InitiatedBy(SendSignedTransaction::class)
class ReceiveSignedTransaction(val otherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(ReceiveTransactionFlow(
            otherSideSession = otherSession,
            statesToRecord = StatesToRecord.ALL_VISIBLE
        ))
    }
}
