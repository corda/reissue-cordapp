package com.template.contracts

import com.template.states.ReIssuanceLock
import com.template.states.ReIssuanceRequest
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

class ReIssuanceRequestContract<T>: Contract where T: ContractState { // TODO: contract validation

    companion object {
        val contractId = this::class.java.enclosingClass.canonicalName
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        when (command.value) {
            is Commands.Create -> verifyCreateCommand(tx, command)
            is Commands.Accept -> verifyAcceptCommand(tx, command)
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

            "Output is of type ReIssuanceRequest" using (tx.outputs[0].data is ReIssuanceRequest<*>)
            val reIssuanceRequest = tx.outputs[0].data as ReIssuanceRequest<T>

            "There must be positive number of states to re-issue" using (reIssuanceRequest.statesToReIssue.isNotEmpty())

            reIssuanceRequest.statesToReIssue.forEach {
                "State to be re-issued ${it.ref} can't be encumbered" using (it.state.encumbrance == null)
            }

            val firstStateToReIssue = reIssuanceRequest.statesToReIssue[0]
            (1 until reIssuanceRequest.statesToReIssue.size).forEach {
                val stateToReIssue = reIssuanceRequest.statesToReIssue[it]

                // participants for all states to be re-issued must be the same
                "Participants in state to be re-issued ${stateToReIssue.ref} must be the same as participants in the first state to be re-issued ${firstStateToReIssue.ref}" using (
                    stateToReIssue.state.data.participants.equals(firstStateToReIssue.state.data.participants))

                // all states to be re-issued must be of the same type
                "State to be re-issued ${stateToReIssue.ref} must be of the same type as the first state to be re-issued ${firstStateToReIssue.ref}" using (
                    stateToReIssue.state.data::class.java == firstStateToReIssue.state.data::class.java)
            }

            // do we want the following?
//            "Participants in re-issuance request must contain all participants from states to be re-issued" using (
//                reIssuanceRequest.participants.containsAll(firstStateToReIssue.state.data.participants))
//
//            "Requester is one of participants" using (reIssuanceRequest.participants.contains(reIssuanceRequest.requester))
//            "Issuer is one of participants" using (reIssuanceRequest.participants.contains(reIssuanceRequest.issuer))

            "There is exactly one signer" using (command.signers.size == 1)
            "Requester is required signer" using (command.signers.contains(reIssuanceRequest.requester.owningKey))
        }
    }

    fun verifyAcceptCommand(
        tx: LedgerTransaction,
        command: CommandWithParties<Commands>
    ) {
        val reIssuanceRequestInputs = tx.inputsOfType<ReIssuanceRequest<T>>()
        val reIssuanceRequestOutputs = tx.outputsOfType<ReIssuanceRequest<T>>()

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

    interface Commands : CommandData {
        class Create : Commands
        class Accept : Commands
    }
}