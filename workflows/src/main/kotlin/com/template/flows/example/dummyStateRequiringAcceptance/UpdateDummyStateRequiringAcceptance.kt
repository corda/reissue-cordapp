package com.template.flows.example.dummyStateRequiringAcceptance

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import com.template.contracts.example.DummyStateRequiringAcceptanceContract
import com.template.states.example.DummyStateRequiringAcceptance
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap

@InitiatingFlow
@StartableByRPC
class UpdateDummyStateRequiringAcceptance(
    private val stateNeedingAcceptanceStateAndRef: StateAndRef<DummyStateRequiringAcceptance>,
    private val newOwner: Party
): FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val owner = ourIdentity
        val issuer = stateNeedingAcceptanceStateAndRef.state.data.issuer
        val acceptor = stateNeedingAcceptanceStateAndRef.state.data.acceptor

        val signers = setOf(owner.owningKey, newOwner.owningKey, acceptor.owningKey).toList()

        var newStateNeedingAcceptance = stateNeedingAcceptanceStateAndRef.state.data
        newStateNeedingAcceptance.owner = newOwner

        val transactionBuilder = TransactionBuilder(notary = getPreferredNotary(serviceHub))
        transactionBuilder.addInputState(stateNeedingAcceptanceStateAndRef)
        transactionBuilder.addOutputState(newStateNeedingAcceptance)
        transactionBuilder.addCommand(DummyStateRequiringAcceptanceContract.Commands.Update(), signers)

        transactionBuilder.verify(serviceHub)
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)

        val signersSessions = listOf(initiateFlow(acceptor), initiateFlow(newOwner))
        signersSessions.forEach {
            it.send(true)
        }

        val otherParticipantsSession = listOf(initiateFlow(issuer))
        otherParticipantsSession.forEach {
            it.send(false)
        }

        val fullySignedTransaction = subFlow(CollectSignaturesFlow(signedTransaction, signersSessions))

        subFlow(
            FinalityFlow(
                transaction = fullySignedTransaction,
                sessions = signersSessions + otherParticipantsSession
            )
        )
    }
}


@InitiatedBy(UpdateDummyStateRequiringAcceptance::class)
class UpdateDummyStateRequiringAcceptanceResponder(
    private val otherSession: FlowSession
) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val needsToSignTransaction = otherSession.receive<Boolean>().unwrap { it }
        // only sign if instructed to do so
        if (needsToSignTransaction) {
            subFlow(object : SignTransactionFlow(otherSession) {
                override fun checkTransaction(stx: SignedTransaction) { }
            })
        }
        // always save the transaction
        return subFlow(ReceiveFinalityFlow(otherSideSession = otherSession))
    }
}
