package com.template.contracts.example

import com.template.states.example.DummyStateRequiringAcceptance
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

class DummyStateRequiringAcceptanceContract: Contract {

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
        val dummyStateRequiringAcceptanceInputs = tx.inputsOfType<DummyStateRequiringAcceptance>()
        val dummyStateRequiringAcceptanceOutputs = tx.outputsOfType<DummyStateRequiringAcceptance>()
        requireThat {
            "No inputs of type DummyStateRequiringAcceptance are allowed" using dummyStateRequiringAcceptanceInputs.isEmpty()
            "Exactly one output of type DummyStateRequiringAcceptance is expected" using (dummyStateRequiringAcceptanceOutputs.size == 1)
            
            val simpleState = dummyStateRequiringAcceptanceOutputs[0]
            "Issuer is required signer" using (command.signers.contains(simpleState.issuer.owningKey))
            "Acceptor is required signer" using (command.signers.contains(simpleState.acceptor.owningKey))
        }
    }

    fun verifyUpdateCommand(
        tx: LedgerTransaction,
        command: CommandWithParties<Commands>
    ) {
        val dummyStateRequiringAcceptanceInputs = tx.inputsOfType<DummyStateRequiringAcceptance>()
        val dummyStateRequiringAcceptanceOutputs = tx.outputsOfType<DummyStateRequiringAcceptance>()
        requireThat {
            "Exactly one input of type DummyStateRequiringAcceptance is expected" using (dummyStateRequiringAcceptanceInputs.size == 1)
            "Exactly one output of type DummyStateRequiringAcceptance is expected" using (dummyStateRequiringAcceptanceOutputs.size == 1)

            val simpleStateInput = dummyStateRequiringAcceptanceInputs[0]
            val simpleStateOutput = dummyStateRequiringAcceptanceOutputs[0]

            "Issuer remains the same" using (simpleStateInput.issuer == simpleStateOutput.issuer)
            "Acceptor remains the same" using (simpleStateInput.acceptor == simpleStateOutput.acceptor)

            "Owner is required signer" using (command.signers.contains(simpleStateInput.owner.owningKey) &&
                command.signers.contains(simpleStateOutput.owner.owningKey))
            "Acceptor is required signer" using (command.signers.contains(simpleStateInput.acceptor.owningKey))
        }
    }

    fun verifyDeleteCommand(
        tx: LedgerTransaction,
        command: CommandWithParties<Commands>
    ) {
        val dummyStateRequiringAcceptanceInputs = tx.inputsOfType<DummyStateRequiringAcceptance>()
        val dummyStateRequiringAcceptanceOutputs = tx.outputsOfType<DummyStateRequiringAcceptance>()
        requireThat {
            "Exactly one input of type DummyStateRequiringAcceptance is expected" using (dummyStateRequiringAcceptanceInputs.size == 1)
            "No outputs of type DummyStateRequiringAcceptance are allowed" using dummyStateRequiringAcceptanceOutputs.isEmpty()

            val simpleState = dummyStateRequiringAcceptanceInputs[0]
            "Owner is required signer" using (command.signers.contains(simpleState.owner.owningKey))
            "Acceptor is required signer" using (command.signers.contains(simpleState.acceptor.owningKey))
        }
    }

    interface Commands : CommandData {
        class Create : Commands
        class Update: Commands
        class Delete : Commands
    }
}