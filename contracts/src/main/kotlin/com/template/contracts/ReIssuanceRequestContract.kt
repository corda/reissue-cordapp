package com.template.contracts

import com.template.states.ReIssuanceLock
import com.template.states.ReIssuanceRequest
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

class ReIssuanceRequestContract<T>: Contract where T: ContractState {

    companion object {
        val contractId = this::class.java.enclosingClass.canonicalName
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        when (command.value) {
            is Commands.Create -> verifyCreateCommand(tx, command)
            is Commands.Approve -> verifyApproveCommand(tx, command)
            is Commands.Deny -> verifyDenyCommand(tx, command)
            else -> throw IllegalArgumentException("Command not supported")
        }
    }

    fun verifyCreateCommand(
        tx: LedgerTransaction,
        command: CommandWithParties<Commands>
    ) {
        requireThat {
            "No inputs are allowed" using tx.inputs.isEmpty()
            "Exactly one output is expected" using (tx.outputs.size == 1)

            "Output is of type ReIssuanceRequest" using (tx.outputs[0].data is ReIssuanceRequest)
            val reIssuanceRequest = tx.outputs[0].data as ReIssuanceRequest

            "There must be positive number of states to re-issue" using (reIssuanceRequest.stateRefsToReIssue.isNotEmpty())

            "Requester is required signer" using (command.signers.contains(reIssuanceRequest.requester.owningKey))
        }
    }

    fun verifyApproveCommand(
        tx: LedgerTransaction,
        command: CommandWithParties<Commands>
    ) {
        val reIssuanceRequestInputs = tx.inputsOfType<ReIssuanceRequest>()
        val reIssuanceRequestOutputs = tx.outputsOfType<ReIssuanceRequest>()

        val reIssuanceLockInputs = tx.inputsOfType<ReIssuanceLock<T>>()
        val reIssuanceLockOutputs = tx.outputsOfType<ReIssuanceLock<T>>()

        requireThat {
            "Exactly one input of type ReIssuanceRequest is expected" using (reIssuanceRequestInputs.size == 1)
            "No outputs of type ReIssuanceRequest are allowed" using reIssuanceRequestOutputs.isEmpty()

            val reIssuanceRequest = reIssuanceRequestInputs[0]
            "Issuer is required signer" using (command.signers.contains(reIssuanceRequest.issuer.owningKey))

            "No inputs of type ReIssuanceLock are allowed" using (reIssuanceLockInputs.isEmpty())
            "Exactly one output of type ReIssuanceLock is expected" using (reIssuanceLockOutputs.size == 1)

            // the rest of the logic is included in ReIssuanceLock contract
        }
    }

    fun verifyDenyCommand( // TODO: unit test
        tx: LedgerTransaction,
        command: CommandWithParties<Commands>
    ) {
        val reIssuanceRequestInputs = tx.inputsOfType<ReIssuanceRequest>()

        requireThat {
            "Exactly one input of type ReIssuanceRequest is expected" using (reIssuanceRequestInputs.size == 1)
            "No inputs of other than ReIssuanceRequest are allowed" using (tx.inputs.size == 1)

            "No outputs are allowed" using (tx.outputs.isEmpty())

            val reIssuanceRequest = reIssuanceRequestInputs[0]
            "Issuer is required signer" using (command.signers.contains(reIssuanceRequest.issuer.owningKey))

        }
    }

    interface Commands : CommandData {
        class Create : Commands
        class Approve : Commands
        class Deny : Commands
    }
}