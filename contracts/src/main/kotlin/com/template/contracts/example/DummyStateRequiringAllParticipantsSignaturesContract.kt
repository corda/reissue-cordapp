package com.template.contracts.example

import com.template.states.example.DummyStateRequiringAllParticipantsSignatures
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
        val stateNeedingAllParticipantsToSignInputs = tx.inputsOfType<DummyStateRequiringAllParticipantsSignatures>()
        val stateNeedingAllParticipantsToSignOutputs = tx.outputsOfType<DummyStateRequiringAllParticipantsSignatures>()
        requireThat {
            "No stateNeedingAllParticipantsToSignInputs are allowed" using stateNeedingAllParticipantsToSignInputs.isEmpty()
            "Exactly one output is expected" using (stateNeedingAllParticipantsToSignOutputs.size == 1)

            val stateNeedingAllParticipantsToSign = stateNeedingAllParticipantsToSignOutputs[0]

            "Owner is required signer" using (
                command.signers.contains(stateNeedingAllParticipantsToSign.owner.owningKey))
            "Issuer is required signer" using (
                command.signers.contains(stateNeedingAllParticipantsToSign.issuer.owningKey))
            "Other is required signer" using (
                command.signers.contains(stateNeedingAllParticipantsToSign.other.owningKey))
        }
    }

    fun verifyUpdateCommand(
        tx: LedgerTransaction,
        command: CommandWithParties<Commands>
    ) {
        val stateNeedingAllParticipantsToSignInputs = tx.inputsOfType<DummyStateRequiringAllParticipantsSignatures>()
        val stateNeedingAllParticipantsToSignOutputs = tx.outputsOfType<DummyStateRequiringAllParticipantsSignatures>()
        requireThat {
            "Exactly one input is expected" using (stateNeedingAllParticipantsToSignInputs.size == 1)
            "Exactly one output is expected" using (stateNeedingAllParticipantsToSignOutputs.size == 1)

            val stateNeedingAllParticipantsToSignInput = stateNeedingAllParticipantsToSignInputs[0]
            val stateNeedingAllParticipantsToSignOutput = stateNeedingAllParticipantsToSignOutputs[0]

            "Owner is required signer" using (
                command.signers.contains(stateNeedingAllParticipantsToSignInput.owner.owningKey)
                    && command.signers.contains(stateNeedingAllParticipantsToSignOutput.owner.owningKey))
            "Issuer is required signer" using (
                command.signers.contains(stateNeedingAllParticipantsToSignInput.issuer.owningKey)
                    && command.signers.contains(stateNeedingAllParticipantsToSignOutput.issuer.owningKey))
            "Other is required signer" using (
                command.signers.contains(stateNeedingAllParticipantsToSignInput.other.owningKey)
                    && command.signers.contains(stateNeedingAllParticipantsToSignOutput.other.owningKey))
        }
    }

    fun verifyDeleteCommand(
        tx: LedgerTransaction,
        command: CommandWithParties<Commands>
    ) {
        val stateNeedingAllParticipantsToSignInputs = tx.inputsOfType<DummyStateRequiringAllParticipantsSignatures>()
        val stateNeedingAllParticipantsToSignOutputs = tx.outputsOfType<DummyStateRequiringAllParticipantsSignatures>()
        requireThat {
            "Exactly one input is expected" using (stateNeedingAllParticipantsToSignInputs.size == 1)
            "No stateNeedingAllParticipantsToSignOutputs are allowed" using stateNeedingAllParticipantsToSignOutputs.isEmpty()

            val stateNeedingAllParticipantsToSign = stateNeedingAllParticipantsToSignInputs[0]

            "Owner is required signer" using (
                command.signers.contains(stateNeedingAllParticipantsToSign.owner.owningKey))
            "Issuer is required signer" using (
                command.signers.contains(stateNeedingAllParticipantsToSign.issuer.owningKey))
            "Other is required signer" using (
                command.signers.contains(stateNeedingAllParticipantsToSign.other.owningKey))
        }
    }

    interface Commands : CommandData {
        class Create : Commands
        class Update: Commands
        class Delete : Commands
    }
}