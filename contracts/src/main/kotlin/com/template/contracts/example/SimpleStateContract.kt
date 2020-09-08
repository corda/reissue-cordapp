package com.template.contracts.example

import com.template.states.example.SimpleState
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

class SimpleStateContract: Contract {

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
        val simpleStateInputs = tx.inputsOfType<SimpleState>()
        val simpleStateOutputs = tx.outputsOfType<SimpleState>()
        requireThat {
            "No simpleStateInputs are allowed" using simpleStateInputs.isEmpty()
            "Exactly one output is expected" using (simpleStateOutputs.size == 1)
        }
    }

    fun verifyUpdateCommand(
        tx: LedgerTransaction,
        command: CommandWithParties<Commands>
    ) {
        val simpleStateInputs = tx.inputsOfType<SimpleState>()
        val simpleStateOutputs = tx.outputsOfType<SimpleState>()
        requireThat {
            "Exactly one input is expected" using (simpleStateInputs.size == 1)
            "Exactly one output is expected" using (simpleStateOutputs.size == 1)
            val inputSimpleState = simpleStateInputs[0]
            val outputSimpleState = simpleStateOutputs[0]
            "Owner in input state is required signer" using command.signers.contains(inputSimpleState.owner.owningKey)
            "Owner in output state is required signer" using command.signers.contains(outputSimpleState.owner.owningKey)
        }
    }

    fun verifyDeleteCommand(
        tx: LedgerTransaction,
        command: CommandWithParties<Commands>
    ) {
        val simpleStateInputs = tx.inputsOfType<SimpleState>()
        val simpleStateOutputs = tx.outputsOfType<SimpleState>()
        requireThat {
            "Exactly one input is expected" using (simpleStateInputs.size == 1)
            "No simpleStateOutputs are allowed" using simpleStateOutputs.isEmpty()
            val inputSimpleState = simpleStateInputs[0]
            "Owner in input state is required signer" using command.signers.contains(inputSimpleState.owner.owningKey)
        }
    }

    interface Commands : CommandData {
        class Create : Commands
        class Update: Commands
        class Delete : Commands
    }
}