package com.template.flows.example.stateNeedingAllParticipantsToSign

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import com.template.contracts.example.StateNeedingAllParticipantsToSignContract
import com.template.states.example.StateNeedingAllParticipantsToSign
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class UpdateStateNeedingAllParticipantsToSign(
    private val stateNeedingAllParticipantsToSignAndRef: StateAndRef<StateNeedingAllParticipantsToSign>,
    private val newOwner: Party
): FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val owner = ourIdentity
        val issuer = stateNeedingAllParticipantsToSignAndRef.state.data.issuer
        val other = stateNeedingAllParticipantsToSignAndRef.state.data.other
        val signers = setOf(owner.owningKey, newOwner.owningKey, issuer.owningKey, other.owningKey).toList()

        var stateNeedingAllParticipantsToSign = stateNeedingAllParticipantsToSignAndRef.state.data
        stateNeedingAllParticipantsToSign.owner = newOwner

        val transactionBuilder = TransactionBuilder(notary = getPreferredNotary(serviceHub))
        transactionBuilder.addInputState(stateNeedingAllParticipantsToSignAndRef)
        transactionBuilder.addOutputState(stateNeedingAllParticipantsToSign)
        transactionBuilder.addCommand(StateNeedingAllParticipantsToSignContract.Commands.Update(), signers)

        transactionBuilder.verify(serviceHub)
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)

        val sessions = listOf(newOwner, issuer, other).map{ initiateFlow(it) }
        val fullySignedTransaction = subFlow(CollectSignaturesFlow(signedTransaction, sessions))

        subFlow(
            FinalityFlow(
                transaction = fullySignedTransaction,
                sessions = sessions
            )
        )
    }
}


@InitiatedBy(UpdateStateNeedingAllParticipantsToSign::class)
class UpdateStateNeedingAllParticipantsToSignResponder(
    private val otherSession: FlowSession
) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val signTransactionFlow = object : SignTransactionFlow(otherSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
            }
        }
        val transaction = subFlow(signTransactionFlow)
        subFlow(
            ReceiveFinalityFlow(
                otherSession,
                expectedTxId = transaction.id,
                statesToRecord = StatesToRecord.ALL_VISIBLE
            )
        )
    }
}
