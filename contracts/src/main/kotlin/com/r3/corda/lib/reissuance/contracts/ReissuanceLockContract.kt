package com.r3.corda.lib.reissuance.contracts

import com.r3.corda.lib.reissuance.states.ReissuanceLock
import com.r3.corda.lib.reissuance.states.ReissuanceRequest
import net.corda.core.contracts.*
import net.corda.core.contracts.Requirements.using
import net.corda.core.crypto.MerkleTree
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.componentHash
import net.corda.core.serialization.deserialize
import net.corda.core.transactions.*

class ReissuanceLockContract<T>: Contract where T: ContractState {

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
        val reissuanceRequestInputs = tx.inputsOfType<ReissuanceRequest>()
        val reissuanceRequestOutputs = tx.outputsOfType<ReissuanceRequest>()

        val reissuanceLockInputs = tx.inputsOfType<ReissuanceLock<T>>()
        val reissuanceLockOutputs = tx.outputsOfType<ReissuanceLock<T>>()

        val otherInputs = tx.inputs.filter { it.state.data !is ReissuanceLock<*> && it.state.data !is ReissuanceRequest }
        val otherOutputs = tx.outputs.filter { it.data !is ReissuanceLock<*>  && it.data !is ReissuanceRequest }

        requireThat {
            //// command constraints

            // verify number of inputs and outputs of a given type
            "Exactly one input of type ReissuanceRequest is expected" using (reissuanceRequestInputs.size == 1)
            "No outputs of type ReissuanceRequest are allowed" using reissuanceRequestOutputs.isEmpty()

            "No inputs of type ReissuanceLock are allowed" using (reissuanceLockInputs.isEmpty())
            "Exactly one output of type ReissuanceLock is expected" using (reissuanceLockOutputs.size == 1)

            "No inputs other than ReissuanceRequest and ReissuanceLock are expected" using otherInputs.isEmpty()
            "At least one output other than ReissuanceRequest and ReissuanceLock is expected" using otherOutputs.isNotEmpty() // redundant

            val reissuanceRequest = reissuanceRequestInputs[0]
            val reissuanceLock = reissuanceLockOutputs[0]

            // verify requester & issuer
            "Requester is the same in both ReissuanceRequest and ReissuanceLock" using (
                reissuanceRequest.requester == reissuanceLock.requester)
            "Issuer is the same in both ReissuanceRequest and ReissuanceLock" using (
                reissuanceRequest.issuer == reissuanceLock.issuer)

            val firstReissuedState = reissuanceLock.originalStates[0]
            (1 until reissuanceRequest.stateRefsToReissue.size).forEach {
                val reissuedState = reissuanceLock.originalStates[it]

                // participants for all re-issued states must be the same
                "Participants in state to be re-issued ${reissuedState.ref} must be the same as participants in the first state to be re-issued ${reissuedState.ref}" using (
                    reissuedState.state.data.participants.equals(firstReissuedState.state.data.participants))

                // all re-issued states must be of the same type
                "State to be re-issued ${reissuedState.ref} must be of the same type as the first state to be re-issued ${reissuedState.ref}" using (
                    reissuedState.state.data::class.java == firstReissuedState.state.data::class.java)
            }

            // verify signers
            "Issuer is required signer" using (command.signers.contains(reissuanceRequest.issuer.owningKey))

            //// state constraints

            // verify status
            "Re-issuance lock status is ACTIVE" using(
                reissuanceLock.status == ReissuanceLock.ReissuanceLockStatus.ACTIVE)

            // verify state data
            "StatesAndRef objects in ReissuanceLock must be the same as re-issued states" using (
                reissuanceLock.originalStates.map { it.state.data } == otherOutputs.map { it.data })

            // verify encumbrance
            reissuanceLock.originalStates.forEach {
                "Original states can't be encumbered" using (it.state.encumbrance  == null)
            }
            otherOutputs.forEach {
                "Output other than ReissuanceRequest and ReissuanceLock must be encumbered" using (it.encumbrance  != null)
            }

        }
    }

    fun verifyDeactivateCommand(
        tx: LedgerTransaction,
        command: CommandWithParties<Commands>
    ) {
        val reissuanceLockInputs = tx.inputsOfType<ReissuanceLock<T>>()
        val reissuanceLockOutputs = tx.outputsOfType<ReissuanceLock<T>>()

        val otherInputs = tx.inputs.filter { it.state.data !is ReissuanceLock<*> }
        val otherOutputs = tx.outputs.filter { it.data !is ReissuanceLock<*> }

        requireThat {
            // verify number of inputs and outputs of a given type
            "Exactly one input of type ReissuanceLock is expected" using (reissuanceLockInputs.size == 1)
            "Exactly one output of type ReissuanceLock is expected" using (reissuanceLockOutputs.size == 1)

            "At least one input other than lock is expected" using otherInputs.isNotEmpty()
            "The same number of inputs and outputs other than lock is expected" using (
                otherInputs.size == otherOutputs.size)

            val reissuanceLockInput = reissuanceLockInputs[0]
            val reissuanceLockOutput = reissuanceLockOutputs[0]

            // verify status
            "Input re-issuance lock status is ACTIVE" using(
                reissuanceLockInput.status == ReissuanceLock.ReissuanceLockStatus.ACTIVE)
            "Output re-issuance lock status is INACTIVE" using(
                reissuanceLockOutput.status == ReissuanceLock.ReissuanceLockStatus.INACTIVE)

            "Re-issuance lock properties hasn't change except for status" using(
                reissuanceLockInput == reissuanceLockOutput.copy(status = ReissuanceLock.ReissuanceLockStatus.ACTIVE))

            val requiredExitCommandSigners = reissuanceLockOutput.extraAssetExitCommandSigners
            val requester = reissuanceLockOutput.requester

            val attachedSignedTransactions = getAttachedLedgerTransaction(tx)

            val lockedStatesRef = reissuanceLockInput.originalStates.map { it.ref }

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
                "Notary of the attached transaction ${attachedSignedTransaction.id} is the same as the notary of the " +
                    "transaction being verified" using (attachedSignedTransaction.notary!!.owningKey == tx.notary!!.owningKey)
                attachedSignedTransaction.sigs.forEach {
                    "Signature $it of transaction ${attachedSignedTransaction.id} is valid" using (
                        it.verify(attachedSignedTransaction.id))
                }
                "Requester is a signer of attached transaction ${attachedSignedTransaction.id}" using(
                    attachedSignedTransaction.sigs.map { it.by }.contains(requester!!.owningKey))
                "Attached transaction ${attachedSignedTransaction.id} is notarised" using(
                    attachedSignedTransaction.sigs.map { it.by }.contains(attachedSignedTransaction.notary!!.owningKey))
                requiredExitCommandSigners.forEach { requiredSigner ->
                    "Attached transaction ${attachedSignedTransaction.id} is signed with ${requiredSigner.owningKey}" using(
                        attachedSignedTransaction.sigs.map { it.by }.contains(requiredSigner.owningKey))
                }

            }

            // verify encumbrance
            otherInputs.forEach {
                "Inputs other than ReissuanceLock must be encumbered" using (it.state.encumbrance != null)
            }
            otherOutputs.forEach {
                "Outputs other than ReissuanceLock can't be encumbered" using (it.encumbrance == null)
            }
            "Input data other than ReissuanceLock are the same as output data other than ReissuanceLock" using (
                otherInputs.map { it.state.data }.toSet() == otherOutputs.map { it.data }.toSet())

            // verify signers
            "Requester is required signer" using (command.signers.contains(reissuanceLockInput.requester.owningKey))
        }

    }

    fun verifyDeleteCommand(
        tx: LedgerTransaction,
        command: CommandWithParties<Commands>
    ) {
        val reissuanceLockInputs = tx.inputs.filter { it.state.data is ReissuanceLock<*> }
        val otherInputs = tx.inputs.filter { it.state.data !is ReissuanceLock<*> }

        requireThat {
            "Exactly one input of type ReissuanceLock is expected" using (reissuanceLockInputs.size == 1)
            val reissuanceLockInput = reissuanceLockInputs[0].state.data as ReissuanceLock<T>
            "Number of other inputs is equal to originalStates length" using (
                otherInputs.size == reissuanceLockInput.originalStates.size)
            "No outputs are allowed" using tx.outputs.isEmpty()

            // verify status
            "Input re-issuance lock status is ACTIVE" using(
                reissuanceLockInput.status == ReissuanceLock.ReissuanceLockStatus.ACTIVE)

            // verify encumbrance
            "Input of type ReissuanceLock must be encumbered" using(reissuanceLockInputs[0].state.encumbrance != null)
            otherInputs.forEach {
                "Input ${it.ref} of type other than ReissuanceLock must be encumbered" using (it.state.encumbrance != null)
            }

            // verify signers
            "Requester is required signer" using (command.signers.contains(reissuanceLockInput.requester.owningKey))
            "Issuer is required signer" using (command.signers.contains(reissuanceLockInput.issuer.owningKey))
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
