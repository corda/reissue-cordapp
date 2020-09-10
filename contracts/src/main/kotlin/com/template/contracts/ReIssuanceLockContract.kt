package com.template.contracts

import com.template.states.ReIssuanceLock
import com.template.states.ReIssuanceRequest
import net.corda.core.contracts.*
import net.corda.core.contracts.Requirements.using
import net.corda.core.internal.castIfPossible
import net.corda.core.serialization.deserialize
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.transactions.SignedTransaction

class ReIssuanceLockContract<T>: Contract where T: ContractState { // TODO: contract validation

    companion object {
        val contractId = this::class.java.enclosingClass.canonicalName
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        when (command.value) {
            is Commands.Create -> verifyCreateCommand(tx, command)
            is Commands.Delete -> verifyDeleteCommand(tx, command)
            else -> throw IllegalArgumentException("Command not supported")
        }
    }

    fun verifyCreateCommand(
        tx: LedgerTransaction,
        command: CommandWithParties<Commands>
    ) {
        val reIssuanceRequestInputs = tx.inputsOfType<ReIssuanceRequest>()
        val reIssuanceRequestOutputs = tx.outputsOfType<ReIssuanceRequest>()

        val reIssuanceLockInputs = tx.inputsOfType<ReIssuanceLock<T>>()
        val reIssuanceLockOutputs = tx.outputsOfType<ReIssuanceLock<T>>()

        val otherInputs = tx.inputs.filter { it.state.data !is ReIssuanceLock<*> && it.state.data !is ReIssuanceRequest }
        val otherOutputs = tx.outputs.filter { it.data !is ReIssuanceLock<*>  && it.data !is ReIssuanceRequest }

        requireThat {
//            // verify number of inputs and outputs of a given type
//            "Exactly one input of type ReIssuanceRequest is expected" using (reIssuanceRequestInputs.size == 1)
//            "No outputs of type ReIssuanceRequest are allowed" using reIssuanceRequestOutputs.isEmpty()
//
//            "No inputs of type ReIssuanceLock are allowed" using (reIssuanceLockInputs.isEmpty())
//            "Exactly one output of type ReIssuanceLock is expected" using (reIssuanceLockOutputs.size == 1)
//
//            "No inputs other than ReIssuanceRequest and ReIssuanceLock are expected" using otherInputs.isEmpty()
//            "At least one output other than ReIssuanceRequest and ReIssuanceLock is expected" using otherOutputs.isNotEmpty() // redundant
//
//            val reIssuanceRequest = reIssuanceRequestInputs[0]
//            val reIssuanceLock = reIssuanceLockOutputs[0]
//
//            // verify requester & issuer
//            "Requester is the same in both ReIssuanceRequest and ReIssuanceLock" using (
//                reIssuanceRequest.requester == reIssuanceLock.requester)
//            "Issuer is the same in both ReIssuanceRequest and ReIssuanceLock" using (
//                reIssuanceRequest.issuer == reIssuanceLock.issuer)
//
//            // verify participants
//            "Participants in re-issuance lock must contain all participants from states to be re-issued" using (
//                reIssuanceLock.participants.containsAll(reIssuanceLock.lockedStates[0].state.data.participants))
//
//            // verify requested states to be re-issued
////            "Re-issued states must be exactly the same as requested states to be re-issued (make sure equals method is implemented for state to be re-issued)" using (
////                reIssuanceRequest.statesToReIssue.map { it.state.data }.toSet() == otherOutputs.map { it.data }.toSet()) // compare only states
////            "StatesAndRef objects in ReIssuanceLock must be the same as requested states to be re-issued (make sure equals method is implemented for state to be re-issued)" using (
////                reIssuanceLock.lockedStates == reIssuanceRequest.statesToReIssue) // compare StateAndRef objects
//
//            // verify encumbrance
//            otherOutputs.forEach {
//                "Output other than ReIssuanceRequest and ReIssuanceLock must be encumbered" using (it.encumbrance  != null)
//            }
//
//            // verify signers
//            "Issuer is required signer" using (command.signers.contains(reIssuanceRequest.issuer.owningKey))
        }
    }

    fun verifyDeleteCommand(
        tx: LedgerTransaction,
        command: CommandWithParties<Commands>
    ) {
        val reIssuanceLockInputs = tx.inputsOfType<ReIssuanceLock<T>>()
        val reIssuanceLockOutputs = tx.outputsOfType<ReIssuanceLock<T>>()

        val otherInputs = tx.inputs.filter { it.state.data !is ReIssuanceLock<*> }
        val otherOutputs = tx.outputs.filter { it.data !is ReIssuanceLock<*> }

        requireThat {
            // verify number of inputs and outputs of a given type
            "Exactly one input of type ReIssuanceLock is expected" using (reIssuanceLockInputs.size == 1)
            "No outputs of type ReIssuanceLock are allowed" using reIssuanceLockOutputs.isEmpty()

            "At least one input other than lock is expected" using otherInputs.isNotEmpty()
            "The same number of inputs and outputs other than lock is expected" using (otherInputs.size == otherOutputs.size)

            val reIssuanceLock = reIssuanceLockInputs[0]
            val attachedSignedTransaction = getAttachedLedgerTransaction(tx)

            val lockedStatesRef = reIssuanceLock.lockedStates.map { it.ref }

            "All locked states are inputs of attached transaction" using (
                attachedSignedTransaction.inputs.containsAll(lockedStatesRef))
            "Attached transaction doesn't have any outputs" using (
                attachedSignedTransaction.coreTransaction.outputs.isEmpty())
            "Notary is provided for attached transaction" using(attachedSignedTransaction.notary != null)
            "Attached transaction is notarised" using(attachedSignedTransaction.sigs.map { it.by }.contains(
                attachedSignedTransaction.notary!!.owningKey))

            // verify encumbrance
            otherInputs.forEach {
                "Input other than ReIssuanceLock must be encumbered" using (it.state.encumbrance != null)
            }
            otherOutputs.forEach {
                "Output other than ReIssuanceLock can't be encumbered" using (it.encumbrance == null)
            }

            // verify signers
            "Requester is required signer" using (command.signers.contains(reIssuanceLock.requester.owningKey))
        }

    }

    private fun getAttachedLedgerTransaction(tx: LedgerTransaction): SignedTransaction {
        // Constraints on the included attachments.
        val nonContractAttachments = tx.attachments.filter { it !is ContractAttachment }
        "The transaction should have a single non-contract attachment" using (nonContractAttachments.size == 1)
        val attachment = nonContractAttachments.single()

        val attachmentJar = attachment.openAsJAR()
        while (attachmentJar.nextEntry.name != "SignedTransaction") {
            // Calling `attachmentJar.nextEntry` causes us to scroll through the JAR.
        }

        val transactionBytes = attachmentJar.readBytes()
        return transactionBytes.deserialize<SignedTransaction>()
    }

    interface Commands : CommandData {
        class Create : Commands
        class Delete : Commands
    }
}
