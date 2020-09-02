package com.template.contracts.example

import com.template.states.example.StateNeedingAcceptance
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

class StateNeedingAcceptanceContract: Contract {

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
        val stateNeedingAcceptanceInputs = tx.inputsOfType<StateNeedingAcceptance>()
        val stateNeedingAcceptanceOutputs = tx.outputsOfType<StateNeedingAcceptance>()
        requireThat {
            "No inputs of type StateNeedingAcceptance are allowed" using stateNeedingAcceptanceInputs.isEmpty()
            "Exactly one output of type StateNeedingAcceptance is expected" using (stateNeedingAcceptanceOutputs.size == 1)
            
            val simpleState = stateNeedingAcceptanceOutputs[0]
            "Issuer is required signer" using (command.signers.contains(simpleState.issuer.owningKey))
            "Acceptor is required signer" using (command.signers.contains(simpleState.acceptor.owningKey))
        }
    }

    fun verifyUpdateCommand(
        tx: LedgerTransaction,
        command: CommandWithParties<Commands>
    ) {
        val stateNeedingAcceptanceInputs = tx.inputsOfType<StateNeedingAcceptance>()
        val stateNeedingAcceptanceOutputs = tx.outputsOfType<StateNeedingAcceptance>()
        requireThat {
            "Exactly one input of type StateNeedingAcceptance is expected" using (stateNeedingAcceptanceInputs.size == 1)
            "Exactly one output of type StateNeedingAcceptance is expected" using (stateNeedingAcceptanceOutputs.size == 1)

            val simpleStateInput = stateNeedingAcceptanceInputs[0]
            val simpleStateOutput = stateNeedingAcceptanceOutputs[0]

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
        val stateNeedingAcceptanceInputs = tx.inputsOfType<StateNeedingAcceptance>()
        val stateNeedingAcceptanceOutputs = tx.outputsOfType<StateNeedingAcceptance>()
        requireThat {
            "Exactly one input of type StateNeedingAcceptance is expected" using (stateNeedingAcceptanceInputs.size == 1)
            "No outputs of type StateNeedingAcceptance are allowed" using stateNeedingAcceptanceOutputs.isEmpty()

            val simpleState = stateNeedingAcceptanceInputs[0]
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