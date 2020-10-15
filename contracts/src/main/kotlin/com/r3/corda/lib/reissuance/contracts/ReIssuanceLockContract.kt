package com.r3.corda.lib.reissuance.contracts

import com.r3.corda.lib.reissuance.states.ReIssuanceLock
import com.r3.corda.lib.reissuance.states.ReIssuanceRequest
import net.corda.core.contracts.*
import net.corda.core.contracts.Requirements.using
import net.corda.core.crypto.MerkleTree
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.componentHash
import net.corda.core.serialization.deserialize
import net.corda.core.transactions.*

class ReIssuanceLockContract<T>: Contract where T: ContractState {

    companion object {
        val contractId = this::class.java.enclosingClass.canonicalName
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        when (command.value) {
            is Commands.Create -> verifyCreateCommand(tx, command)
            is Commands.Deactivate -> verifyDeactivateCommand(tx, command)
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
            //// command constraints

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

            //// state constraints

            // verify status
            "Re-issuance lock status is ACTIVE" using(
                reIssuanceLock.status == ReIssuanceLock.ReIssuanceLockStatus.ACTIVE)

            // verify state data
            "StatesAndRef objects in ReIssuanceLock must be the same as re-issued states" using (
                reIssuanceLock.originalStates.map { it.state.data } == otherOutputs.map { it.data })

            // verify encumbrance
            reIssuanceLock.originalStates.forEach {
                "Original states can't be encumbered" using (it.state.encumbrance  == null)
            }
            otherOutputs.forEach {
                "Output other than ReIssuanceRequest and ReIssuanceLock must be encumbered" using (it.encumbrance  != null)
            }

        }
    }

    fun verifyDeactivateCommand(
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
            "Exactly one output of type ReIssuanceLock is expected" using (reIssuanceLockOutputs.size == 1)

            "At least one input other than lock is expected" using otherInputs.isNotEmpty()
            "The same number of inputs and outputs other than lock is expected" using (
                otherInputs.size == otherOutputs.size)

            val reIssuanceLockInput = reIssuanceLockInputs[0]
            val reIssuanceLockOutput = reIssuanceLockOutputs[0]

            // verify status
            "Input re-issuance lock status is ACTIVE" using(
                reIssuanceLockInput.status == ReIssuanceLock.ReIssuanceLockStatus.ACTIVE)
            "Output re-issuance lock status is INACTIVE" using(
                reIssuanceLockOutput.status == ReIssuanceLock.ReIssuanceLockStatus.INACTIVE)

            "Re-issuance lock properties hasn't change except for status" using(
                reIssuanceLockInput == reIssuanceLockOutput.copy(status = ReIssuanceLock.ReIssuanceLockStatus.ACTIVE))

            val issuerIsRequiredExitTransactionSigner = reIssuanceLockOutput.issuerIsRequiredExitTransactionSigner
            val issuer = reIssuanceLockOutput.issuer

            val attachedSignedTransactions = getAttachedLedgerTransaction(tx)

            val lockedStatesRef = reIssuanceLockInput.originalStates.map { it.ref }

            "All locked states are inputs of attached transactions" using (
                attachedSignedTransactions.flatMap { it.inputs }.containsAll(lockedStatesRef))
            "Attached transactions don't have any outputs" using (
                attachedSignedTransactions.flatMap{ it.coreTransaction.outputs }.isEmpty())

            attachedSignedTransactions.forEach { attachedSignedTransaction ->
                val attachedWireTransaction = attachedSignedTransaction.coreTransaction as? WireTransaction
                "Attached CoreTransaction ${attachedSignedTransaction.id} can be cast to WireTransaction" using(
                    attachedWireTransaction != null)
                attachedWireTransaction!!
                "Id of attached SignedTransaction ${attachedSignedTransaction.id} is equal to id of WireTransaction" using(
                    attachedSignedTransaction.id == attachedWireTransaction.id)
                "Id of attached WireTransaction ${attachedSignedTransaction.id} is equal to merkle tree hash" using(
                    attachedWireTransaction.id == attachedWireTransaction.merkleTree.hash)
                "Merkle tree of attached transaction ${attachedSignedTransaction.id} is valid" using (
                    generateWireTransactionMerkleTree(attachedWireTransaction) == attachedWireTransaction.merkleTree)
                "Notary is provided for attached transaction ${attachedSignedTransaction.id}" using(
                    attachedSignedTransaction.notary != null)
                if(issuerIsRequiredExitTransactionSigner) {
                    "Issuer is signer of attached transaction ${attachedSignedTransaction.id}" using(
                        attachedSignedTransaction.sigs.map { it.by }.contains(issuer.owningKey))
                }
                "Attached transaction ${attachedSignedTransaction.id} is notarised" using(
                    attachedSignedTransaction.sigs.map { it.by }.contains(attachedSignedTransaction.notary!!.owningKey))
            }

            // verify encumbrance
            otherInputs.forEach {
                "Inputs other than ReIssuanceLock must be encumbered" using (it.state.encumbrance != null)
            }
            otherOutputs.forEach {
                "Outputs other than ReIssuanceLock can't be encumbered" using (it.encumbrance == null)
            }
            "Input data other than ReIssuanceLock are the same as output data other than ReIssuanceLock" using (
                otherInputs.map { it.state.data }.toSet() == otherOutputs.map { it.data }.toSet())

            // verify signers
            "Requester is required signer" using (command.signers.contains(reIssuanceLockInput.requester.owningKey))
        }

    }

    fun verifyDeleteCommand(
        tx: LedgerTransaction,
        command: CommandWithParties<Commands>
    ) {
        val reIssuanceLockInputs = tx.inputs.filter { it.state.data is ReIssuanceLock<*> }
        val otherInputs = tx.inputs.filter { it.state.data !is ReIssuanceLock<*> }

        requireThat {
            "Exactly one input of type ReIssuanceLock is expected" using (reIssuanceLockInputs.size == 1)
            val reIssuanceLockInput = reIssuanceLockInputs[0].state.data as ReIssuanceLock<T>
            "Number of other inputs is equal to originalStates length" using (
                otherInputs.size == reIssuanceLockInput.originalStates.size)
            "No outputs of are allowed" using tx.outputs.isEmpty()

            // verify status
            "Input re-issuance lock status is ACTIVE" using(
                reIssuanceLockInput.status == ReIssuanceLock.ReIssuanceLockStatus.ACTIVE)

            // verify encumbrance
            "Input of type ReIssuanceLock must be encumbered" using(reIssuanceLockInputs[0].state.encumbrance != null)
            otherInputs.forEach {
                "Input ${it.ref} of type other than ReIssuanceLock must be encumbered" using (it.state.encumbrance != null)
            }

            // verify signers
            "Requester is required signer" using (command.signers.contains(reIssuanceLockInput.requester.owningKey))
            "Issuer is required signer" using (command.signers.contains(reIssuanceLockInput.issuer.owningKey))
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

    private fun generateWireTransactionMerkleTree(
        wireTransaction: WireTransaction
    ): MerkleTree {
        val availableComponentNonces: Map<Int, List<SecureHash>> by lazy {
            wireTransaction.componentGroups.map { Pair(it.groupIndex, it.components.mapIndexed {
                internalIndex, internalIt ->
                componentHash(internalIt, wireTransaction.privacySalt, it.groupIndex, internalIndex) })
            }.toMap()
        }

        val availableComponentHashes = wireTransaction.componentGroups.map {
            Pair(it.groupIndex, it.components.mapIndexed {
                internalIndex, internalIt ->
                componentHash(availableComponentNonces[it.groupIndex]!![internalIndex], internalIt) })
        }.toMap()

        val groupsMerkleRoots: Map<Int, SecureHash> by lazy {
            availableComponentHashes.map {
                Pair(it.key, MerkleTree.getMerkleTree(it.value).hash)
            }.toMap()
        }
        val groupHashes: List<SecureHash> by lazy {
            val listOfLeaves = mutableListOf<SecureHash>()
            // Even if empty and not used, we should at least send oneHashes for each known
            // or received but unknown (thus, bigger than known ordinal) component groups.
            for (i in 0..wireTransaction.componentGroups.map { it.groupIndex }.max()!!) {
                val root = groupsMerkleRoots[i] ?: SecureHash.allOnesHash
                listOfLeaves.add(root)
            }
            listOfLeaves
        }
        return MerkleTree.getMerkleTree(groupHashes)

    }

    interface Commands : CommandData {
        class Create : Commands
        class Deactivate : Commands
        class Delete: Commands
    }
}
