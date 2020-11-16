package com.r3.corda.lib.reissuance.dummy_contracts

import com.r3.corda.lib.reissuance.dummy_states.SimpleDummyState
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

class SimpleDummyStateContract: Contract {

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
        val simpleDummyStateInputs = tx.inputsOfType<SimpleDummyState>()
        val simpleDummyStateOutputs = tx.outputsOfType<SimpleDummyState>()
        requireThat {
            "No inputs of type SimpleDummyState are allowed" using simpleDummyStateInputs.isEmpty()
            "Exactly one output is expected" using (simpleDummyStateOutputs.size == 1)
        }
    }

    fun verifyUpdateCommand(
        tx: LedgerTransaction,
        command: CommandWithParties<Commands>
    ) {
        val simpleDummyStateInputs = tx.inputsOfType<SimpleDummyState>()
        val simpleDummyStateOutputs = tx.outputsOfType<SimpleDummyState>()
        requireThat {
            "Exactly one input of type SimpleDummyState is expected" using (simpleDummyStateInputs.size == 1)
            "Exactly one output of type SimpleDummyState is expected" using (simpleDummyStateOutputs.size == 1)
        }
    }

    fun verifyDeleteCommand(
        tx: LedgerTransaction,
        command: CommandWithParties<Commands>
    ) {
        val simpleDummyStateInputs = tx.inputsOfType<SimpleDummyState>()
        val simpleDummyStateOutputs = tx.outputsOfType<SimpleDummyState>()
        requireThat {
            "Exactly one input is expected" using (simpleDummyStateInputs.size == 1)
            "No simpleDummyStateOutputs are allowed" using simpleDummyStateOutputs.isEmpty()
            val inputSimpleDummyState = simpleDummyStateInputs[0]
            "Owner in input state is required signer" using command.signers.contains(inputSimpleDummyState.owner.owningKey)
        }
    }

    interface Commands : CommandData {
        class Create : Commands {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                return true
            }

            override fun hashCode(): Int {
                return javaClass.hashCode()
            }
        }

        class Update: Commands {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                return true
            }

            override fun hashCode(): Int {
                return javaClass.hashCode()
            }
        }

        class Delete : Commands {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                return true
            }

            override fun hashCode(): Int {
                return javaClass.hashCode()
            }
        }
    }
}