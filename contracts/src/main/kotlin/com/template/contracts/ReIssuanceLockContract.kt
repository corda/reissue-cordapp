package com.template.contracts

import com.template.states.ReIssuanceLock
import com.template.states.ReIssuanceRequest
import net.corda.core.contracts.*
import net.corda.core.contracts.Requirements.using
import net.corda.core.crypto.Base58
import net.corda.core.crypto.SecureHash
import net.corda.core.serialization.deserialize
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.transactions.SignedTransaction

class ReIssuanceLockContract<T>: Contract where T: ContractState {

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
            // verify number of inputs and outputs of a given type
            "Exactly one input of type ReIssuanceRequest is expected" using (reIssuanceRequestInputs.size == 1)
            "No outputs of type ReIssuanceRequest are allowed" using reIssuanceRequestOutputs.isEmpty()

            "No inputs of type ReIssuanceLock are allowed" using (reIssuanceLockInputs.isEmpty())
            "Exactly one output of type ReIssuanceLock is expected" using (reIssuanceLockOutputs.size == 1)

            "No inputs other than ReIssuanceRequest and ReIssuanceLock are expected" using otherInputs.isEmpty()
            "At least one output other than ReIssuanceRequest and ReIssuanceLock is expected" using otherOutputs.isNotEmpty() // redundant

            val reIssuanceRequest = reIssuanceRequestInputs[0]
            val reIssuanceLock = reIssuanceLockOutputs[0]

            // verify requester & issuer
            "Requester is the same in both ReIssuanceRequest and ReIssuanceLock" using (
                reIssuanceRequest.requester == reIssuanceLock.requester)
            "Issuer is the same in both ReIssuanceRequest and ReIssuanceLock" using (
                reIssuanceRequest.issuer == reIssuanceLock.issuer)

            // verify participants
            "Participants in re-issuance lock must contain all participants from states to be re-issued" using (
                reIssuanceLock.participants.containsAll(reIssuanceLock.originalStates[0].state.data.participants))

            // verify state data
            "StatesAndRef objects in ReIssuanceLock must be the same as re-issued states" using (
                reIssuanceLock.originalStates.map { it.state.data } == otherOutputs.map { it.data })

            // verify encumbrance
            reIssuanceLock.originalStates.forEach {
                "States referenced in lock object must be unencumbered" using (it.state.encumbrance  == null)
            }
            otherOutputs.forEach {
                "Output other than ReIssuanceRequest and ReIssuanceLock must be encumbered" using (it.encumbrance  != null)
            }

            val firstReIssuedState = reIssuanceLock.originalStates[0]
            (1 until reIssuanceRequest.stateRefsToReIssue.size).forEach {
                val reIssuedState = reIssuanceLock.originalStates[it]

                // participants for all re-issued states must be the same
                "Participants in state to be re-issued ${reIssuedState.ref} must be the same as participants in the first state to be re-issued ${reIssuedState.ref}" using (
                    reIssuedState.state.data.participants.equals(firstReIssuedState.state.data.participants))

                // all re-issued states must be of the same type
                "State to be re-issued ${reIssuedState.ref} must be of the same type as the first state to be re-issued ${reIssuedState.ref}" using (
                    reIssuedState.state.data::class.java == firstReIssuedState.state.data::class.java)
            }

            // verify signers
            "Issuer is required signer" using (command.signers.contains(reIssuanceRequest.issuer.owningKey))
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
            "The same number of inputs and outputs other than lock is expected" using (
                otherInputs.size == otherOutputs.size)

            val reIssuanceLock = reIssuanceLockInputs[0]
            val attachedSignedTransactions = getAttachedLedgerTransaction(tx)

            val lockedStatesRef = reIssuanceLock.originalStates.map { it.ref }

            "All locked states are inputs of attached transactions" using (
                attachedSignedTransactions.flatMap { it.inputs }.containsAll(lockedStatesRef))
            "Attached transactions don't have any outputs" using (
                attachedSignedTransactions.flatMap{ it.coreTransaction.outputs }.isEmpty())

            attachedSignedTransactions.forEach { attachedSignedTransaction ->
                "Notary is provided for attached transaction ${attachedSignedTransaction.id}" using(
                    attachedSignedTransaction.notary != null)
                "Attached transaction ${attachedSignedTransaction.id} is notarised" using(
                    attachedSignedTransaction.sigs.map { it.by }.contains(attachedSignedTransaction.notary!!.owningKey))
            }


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

    private fun getAttachedLedgerTransaction(tx: LedgerTransaction): List<SignedTransaction> {
        // Constraints on the included attachments.
        val nonContractAttachments = tx.attachments.filter { it !is ContractAttachment }
        "The transaction should have at least one non-contract attachment" using (nonContractAttachments.isNotEmpty())

        var attachedSignedTransactions = mutableListOf<SignedTransaction>()
        nonContractAttachments.forEach { attachment ->
            val attachmentJar = attachment.openAsJAR()
            var nextEntry = attachmentJar.nextEntry
            while (nextEntry != null && !nextEntry.name.startsWith("SignedTransaction")) {
                // Calling `attachmentJar.nextEntry` causes us to scroll through the JAR.
                nextEntry = attachmentJar.nextEntry
            }

            if(nextEntry != null) {
                val transactionBytes = attachmentJar.readBytes()
                attachedSignedTransactions.add(transactionBytes.deserialize<SignedTransaction>())
            }

        }

        return attachedSignedTransactions
    }

    interface Commands : CommandData {
        class Create : Commands
        class Delete : Commands
    }
}
