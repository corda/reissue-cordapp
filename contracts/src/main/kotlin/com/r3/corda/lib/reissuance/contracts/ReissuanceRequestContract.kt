package com.r3.corda.lib.reissuance.contracts

import com.r3.corda.lib.reissuance.states.ReissuanceLock
import com.r3.corda.lib.reissuance.states.ReissuanceRequest
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

class ReissuanceRequestContract<T>: Contract where T: ContractState {

    companion object {
        val contractId = this::class.java.enclosingClass.canonicalName
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        when (command.value) {
            is Commands.Create -> verifyCreateCommand(tx, command)
            is Commands.Accept -> verifyAcceptCommand(tx, command)
            is Commands.Reject -> verifyRejectCommand(tx, command)
            else -> throw IllegalArgumentException("Command not supported")
        }
    }

    fun verifyCreateCommand(
        tx: LedgerTransaction,
        command: CommandWithParties<Commands>
    ) {
        requireThat {
            // command constraints
            "No inputs are allowed" using tx.inputs.isEmpty()
            "Exactly one output is expected" using (tx.outputs.size == 1)
            "Output is of type ReissuanceRequest" using (tx.outputs[0].data is ReissuanceRequest)
            val reissuanceRequest = tx.outputs[0].data as ReissuanceRequest

            "Requester is required signer" using (command.signers.contains(reissuanceRequest.requester.owningKey))

            // universal constraints
            "Issuer and requester must be different parties" using(
                reissuanceRequest.issuer != reissuanceRequest.requester)

            // state constraints
            "There must be positive number of states to re-issue" using (
                reissuanceRequest.stateRefsToReissue.isNotEmpty())
            "Asset issuance signers must contain issuer" using(
                reissuanceRequest.assetIssuanceSigners.contains(reissuanceRequest.issuer))
        }
    }

    fun verifyAcceptCommand(
        tx: LedgerTransaction,
        command: CommandWithParties<Commands>
    ) {
        val reissuanceRequestInputs = tx.inputsOfType<ReissuanceRequest>()
        val reissuanceRequestOutputs = tx.outputsOfType<ReissuanceRequest>()

        val reissuanceLockInputs = tx.inputsOfType<ReissuanceLock<T>>()
        val reissuanceLockOutputs = tx.outputsOfType<ReissuanceLock<T>>()

        requireThat {
            // command constraints
            "Exactly one input of type ReissuanceRequest is expected" using (reissuanceRequestInputs.size == 1)
            "No outputs of type ReissuanceRequest are allowed" using reissuanceRequestOutputs.isEmpty()

            val reissuanceRequest = reissuanceRequestInputs[0]
            "Issuer is required signer" using (command.signers.contains(reissuanceRequest.issuer.owningKey))

            "No inputs of type ReissuanceLock are allowed" using (reissuanceLockInputs.isEmpty())
            "Exactly one output of type ReissuanceLock is expected" using (reissuanceLockOutputs.size == 1)

            // the rest of the logic is included in ReissuanceLock contract

            // universal constraints
            "Issuer and requester must be different parties" using(
                reissuanceRequest.issuer != reissuanceRequest.requester)
        }
    }

    fun verifyRejectCommand(
        tx: LedgerTransaction,
        command: CommandWithParties<Commands>
    ) {
        val reissuanceRequestInputs = tx.inputsOfType<ReissuanceRequest>()

        requireThat {
            // command constraints
            "Exactly one input of type ReissuanceRequest is expected" using (reissuanceRequestInputs.size == 1)
            "No inputs of other than ReissuanceRequest are allowed" using (tx.inputs.size == 1)
            "No outputs are allowed" using (tx.outputs.isEmpty())

            val reissuanceRequest = reissuanceRequestInputs[0]
            "Issuer is required signer" using (command.signers.contains(reissuanceRequest.issuer.owningKey))

            // universal constraints
            "Issuer and requester must be different parties" using(
                reissuanceRequest.issuer != reissuanceRequest.requester)
        }
    }

    interface Commands : CommandData {
        class Create : Commands
        class Accept : Commands
        class Reject : Commands
    }
}