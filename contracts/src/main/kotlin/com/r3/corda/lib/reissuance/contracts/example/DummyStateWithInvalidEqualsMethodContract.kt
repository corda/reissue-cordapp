package com.r3.corda.lib.reissuance.contracts.example

import com.r3.corda.lib.reissuance.states.example.DummyStateWithInvalidEqualsMethod
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

class DummyStateWithInvalidEqualsMethodContract: Contract {

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
        val dummyStateWithInvalidEqualsMethodInputs = tx.inputsOfType<DummyStateWithInvalidEqualsMethod>()
        val dummyStateWithInvalidEqualsMethodOutputs = tx.outputsOfType<DummyStateWithInvalidEqualsMethod>()
        requireThat {
            "No inputs of type DummyStateWithInvalidEqualsMethod are allowed" using dummyStateWithInvalidEqualsMethodInputs.isEmpty()
            "Exactly one output is expected" using (dummyStateWithInvalidEqualsMethodOutputs.size == 1)
        }
    }

    fun verifyUpdateCommand(
        tx: LedgerTransaction,
        command: CommandWithParties<Commands>
    ) {
        val dummyStateWithInvalidEqualsMethodInputs = tx.inputsOfType<DummyStateWithInvalidEqualsMethod>()
        val dummyStateWithInvalidEqualsMethodOutputs = tx.outputsOfType<DummyStateWithInvalidEqualsMethod>()
        requireThat {
            "Exactly one input of type DummyStateWithInvalidEqualsMethod is expected" using (dummyStateWithInvalidEqualsMethodInputs.size == 1)
            "Exactly one output of type DummyStateWithInvalidEqualsMethod is expected" using (dummyStateWithInvalidEqualsMethodOutputs.size == 1)
            val inputDummyStateWithInvalidEqualsMethod = dummyStateWithInvalidEqualsMethodInputs[0]
            val outputDummyStateWithInvalidEqualsMethod = dummyStateWithInvalidEqualsMethodOutputs[0]
            "Owner in input state is required signer" using command.signers.contains(inputDummyStateWithInvalidEqualsMethod.owner.owningKey)
            "Owner in output state is required signer" using command.signers.contains(outputDummyStateWithInvalidEqualsMethod.owner.owningKey)
        }
    }

    fun verifyDeleteCommand(
        tx: LedgerTransaction,
        command: CommandWithParties<Commands>
    ) {
        val dummyStateWithInvalidEqualsMethodInputs = tx.inputsOfType<DummyStateWithInvalidEqualsMethod>()
        val dummyStateWithInvalidEqualsMethodOutputs = tx.outputsOfType<DummyStateWithInvalidEqualsMethod>()
        requireThat {
            "Exactly one input is expected" using (dummyStateWithInvalidEqualsMethodInputs.size == 1)
            "No dummyStateWithInvalidEqualsMethodOutputs are allowed" using dummyStateWithInvalidEqualsMethodOutputs.isEmpty()
            val inputDummyStateWithInvalidEqualsMethod = dummyStateWithInvalidEqualsMethodInputs[0]
            "Owner in input state is required signer" using command.signers.contains(inputDummyStateWithInvalidEqualsMethod.owner.owningKey)
        }
    }

    interface Commands : CommandData {
        class Create : Commands
        class Update: Commands
        class Delete : Commands
    }
}