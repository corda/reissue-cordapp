package com.r3.corda.lib.reissuance.contracts.example

import com.r3.corda.lib.reissuance.states.example.DummyStateRequiringAllParticipantsSignatures
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

class DummyStateRequiringAllParticipantsSignaturesContract: Contract {

    companion object {
        val contractId = this::class.java.enclosingClass.canonicalName
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        when (command.value) {
            is Commands.Create -> verifyCreateCommand(tx, command)
            is Commands.Update -> verifyUpdateCommand(tx, command)
            is Commands.Delete -> verifyDeleteCommand(tx, command)
            else -> throw IllegalArgumentException("Command not supported")
        }
    }

    fun verifyCreateCommand(
        tx: LedgerTransaction,
        command: CommandWithParties<Commands>
    ) {
        val dummyStateRequiringAllParticipantsSignaturesInputs = tx.inputsOfType<DummyStateRequiringAllParticipantsSignatures>()
        val dummyStateRequiringAllParticipantsSignaturesOutputs = tx.outputsOfType<DummyStateRequiringAllParticipantsSignatures>()
        requireThat {
            "No inputs of type StateRequiringAllParticipantsSignatures are allowed" using
                dummyStateRequiringAllParticipantsSignaturesInputs.isEmpty()
            "Exactly one output is expected" using (dummyStateRequiringAllParticipantsSignaturesOutputs.size == 1)

            val dummyStateRequiringAllParticipantsSignatures = dummyStateRequiringAllParticipantsSignaturesOutputs[0]

            "Owner is required signer" using (
                command.signers.contains(dummyStateRequiringAllParticipantsSignatures.owner.owningKey))
            "Issuer is required signer" using (
                command.signers.contains(dummyStateRequiringAllParticipantsSignatures.issuer.owningKey))
            "Other is required signer" using (
                command.signers.contains(dummyStateRequiringAllParticipantsSignatures.other.owningKey))
        }
    }

    fun verifyUpdateCommand(
        tx: LedgerTransaction,
        command: CommandWithParties<Commands>
    ) {
        val dummyStateRequiringAllParticipantsSignaturesInputs = tx.inputsOfType<DummyStateRequiringAllParticipantsSignatures>()
        val dummyStateRequiringAllParticipantsSignaturesOutputs = tx.outputsOfType<DummyStateRequiringAllParticipantsSignatures>()
        requireThat {
            "Exactly one input of type DummyStateRequiringAllParticipantsSignatures is expected" using (
                dummyStateRequiringAllParticipantsSignaturesInputs.size == 1)
            "Exactly one output of type DummyStateRequiringAllParticipantsSignatures is expected" using (
                dummyStateRequiringAllParticipantsSignaturesOutputs.size == 1)

            val dummyStateRequiringAllParticipantsSignaturesInput = dummyStateRequiringAllParticipantsSignaturesInputs[0]
            val dummyStateRequiringAllParticipantsSignaturesOutput = dummyStateRequiringAllParticipantsSignaturesOutputs[0]

            "Owner is required signer" using (
                command.signers.contains(dummyStateRequiringAllParticipantsSignaturesInput.owner.owningKey)
                    && command.signers.contains(dummyStateRequiringAllParticipantsSignaturesOutput.owner.owningKey))
            "Issuer is required signer" using (
                command.signers.contains(dummyStateRequiringAllParticipantsSignaturesInput.issuer.owningKey)
                    && command.signers.contains(dummyStateRequiringAllParticipantsSignaturesOutput.issuer.owningKey))
            "Other is required signer" using (
                command.signers.contains(dummyStateRequiringAllParticipantsSignaturesInput.other.owningKey)
                    && command.signers.contains(dummyStateRequiringAllParticipantsSignaturesOutput.other.owningKey))
        }
    }

    fun verifyDeleteCommand(
        tx: LedgerTransaction,
        command: CommandWithParties<Commands>
    ) {
        val dummyStateRequiringAllParticipantsSignaturesInputs = tx.inputsOfType<DummyStateRequiringAllParticipantsSignatures>()
        val dummyStateRequiringAllParticipantsSignaturesOutputs = tx.outputsOfType<DummyStateRequiringAllParticipantsSignatures>()
        requireThat {
            "Exactly one input of type DummyStateRequiringAllParticipantsSignatures is expected" using (dummyStateRequiringAllParticipantsSignaturesInputs.size == 1)
            "No DummyStateRequiringAllParticipantsSignatures are allowed" using dummyStateRequiringAllParticipantsSignaturesOutputs.isEmpty()

            val dummyStateRequiringAllParticipantsSignatures = dummyStateRequiringAllParticipantsSignaturesInputs[0]

            "Owner is required signer" using (
                command.signers.contains(dummyStateRequiringAllParticipantsSignatures.owner.owningKey))
            "Issuer is required signer" using (
                command.signers.contains(dummyStateRequiringAllParticipantsSignatures.issuer.owningKey))
            "Other is required signer" using (
                command.signers.contains(dummyStateRequiringAllParticipantsSignatures.other.owningKey))
        }
    }

    interface Commands : CommandData {
        class Create : Commands
        class Update: Commands
        class Delete : Commands
    }
}