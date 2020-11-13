package com.r3.corda.lib.reissuance.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import com.r3.corda.lib.reissuance.contracts.ReissuanceLockContract
import com.r3.corda.lib.reissuance.states.ReissuanceLock
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap

@InitiatingFlow
@StartableByRPC
class DeleteReissuedStatesAndLock<T>(
    private val reissuanceLockStateAndRef: StateAndRef<ReissuanceLock<T>>,
    private val reissuedStateAndRefs: List<StateAndRef<T>>,
    private val assetExitCommand: CommandData,
    private val assetExitSigners: List<AbstractParty> = listOf(reissuanceLockStateAndRef.state.data.requester,
        reissuanceLockStateAndRef.state.data.issuer)
): FlowLogic<SecureHash>() where T: ContractState {
    @Suspendable
    override fun call(): SecureHash {
        val notary = getPreferredNotary(serviceHub)

        val reissuanceLock = reissuanceLockStateAndRef.state.data
        val requester = reissuanceLock.requester
        val issuer = reissuanceLock.issuer
        val lockSigners = listOf(requester, issuer)
        val lockSignersKeys = lockSigners.map { it.owningKey }
        val reissuedStatesSignersKeys = assetExitSigners.map { it.owningKey }

        val transactionBuilder = TransactionBuilder(notary)

        reissuedStateAndRefs.forEach { reissuedStateAndRef ->
            transactionBuilder.addInputState(reissuedStateAndRef)
        }
        transactionBuilder.addCommand(assetExitCommand, reissuedStatesSignersKeys)

        transactionBuilder.addInputState(reissuanceLockStateAndRef)
        transactionBuilder.addCommand(ReissuanceLockContract.Commands.Delete(), lockSignersKeys)

        val signers = (lockSigners + assetExitSigners).distinct()

        val localSigners = signers.filter { serviceHub.identityService.partyFromKey(it.owningKey)!! == ourIdentity }
        val localSignersKeys = localSigners.map { it.owningKey }

        transactionBuilder.verify(serviceHub)
        var signedTransaction = serviceHub.signInitialTransaction(transactionBuilder, localSignersKeys)

        // as some of the participants might be signers and some might not, we are sending them a flag which informs
        // them if they are expected to sign the transaction or not
        val otherParticipants = reissuanceLock.participants.filter { !signers.contains(it) }

        val signersSessions = subFlow(GenerateRequiredFlowSessions(signers))
        val otherParticipantsSessions = subFlow(GenerateRequiredFlowSessions(otherParticipants))
        subFlow(SendSignerFlags(signersSessions, otherParticipantsSessions))

        if(signersSessions.isNotEmpty()) {
            signedTransaction = subFlow(CollectSignaturesFlow(signedTransaction, signersSessions, localSignersKeys))
        }

        return subFlow(
            FinalityFlow(
                transaction = signedTransaction,
                sessions = signersSessions + otherParticipantsSessions
            )
        ).id
    }

}

abstract class DeleteReissuedStatesAndLockResponder(
    private val otherSession: FlowSession
) : FlowLogic<SignedTransaction>() {

    abstract fun checkSignedTransaction(stx: SignedTransaction)

    @Suspendable
    override fun call(): SignedTransaction {
        val needsToSignTransaction = otherSession.receive<Boolean>().unwrap { it }
        // only sign if instructed to do so
        if (needsToSignTransaction) {
            subFlow(object : SignTransactionFlow(otherSession) {
                override fun checkTransaction(stx: SignedTransaction) {
                    checkSignedTransaction(stx)
                }
            })
        }
        // always save the transaction
        return subFlow(ReceiveFinalityFlow(otherSideSession = otherSession))
    }
}
