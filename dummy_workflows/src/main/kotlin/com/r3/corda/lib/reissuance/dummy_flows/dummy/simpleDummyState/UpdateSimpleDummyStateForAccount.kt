package com.r3.corda.lib.reissuance.dummy_flows.dummy.simpleDummyState

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.reissuance.dummy_contracts.SimpleDummyStateContract
import com.r3.corda.lib.reissuance.dummy_states.SimpleDummyState
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.requireThat
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class UpdateSimpleDummyStateForAccount(
    private val simpleDummyStateStateAndRef: StateAndRef<SimpleDummyState>,
    private val newOwner: AbstractParty
): FlowLogic<SecureHash>() {
    @Suspendable
    override fun call(): SecureHash {
        val owner = simpleDummyStateStateAndRef.state.data.owner

        val ownerHost = serviceHub.identityService.partyFromKey(owner.owningKey)!!
        require(ownerHost == ourIdentity) { "Owner is not a valid account for the host" }

        val newOwnerHost = serviceHub.identityService.partyFromKey(newOwner.owningKey)!!

        val signers = setOf(owner.owningKey, newOwner.owningKey).toList() // old and new owner might be the same
        val localSigners = listOfNotNull(
            simpleDummyStateStateAndRef.state.data.owner.owningKey,
            if(newOwnerHost == ourIdentity && owner != newOwner) newOwner.owningKey else null
        )

        val transactionBuilder = TransactionBuilder(notary = getPreferredNotary(serviceHub))
        transactionBuilder.addInputState(simpleDummyStateStateAndRef)
        transactionBuilder.addOutputState(SimpleDummyState(newOwner))
        transactionBuilder.addCommand(SimpleDummyStateContract.Commands.Update(), signers)

        transactionBuilder.verify(serviceHub)
        var signedTransaction = serviceHub.signInitialTransaction(transactionBuilder, localSigners)

        val signersSessions = listOfNotNull(
            if(newOwnerHost != ourIdentity) initiateFlow(newOwnerHost) else null
        )

        signedTransaction = subFlow(CollectSignaturesFlow(signedTransaction, signersSessions, localSigners))

        return subFlow(
            FinalityFlow(
                transaction = signedTransaction,
                sessions = signersSessions
            )
        ).id
    }
}


@InitiatedBy(UpdateSimpleDummyStateForAccount::class)
class UpdateSimpleDummyStateForAccountResponder(
    private val otherSession: FlowSession
) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val signTransactionFlow = object : SignTransactionFlow(otherSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
            }
        }
        val transaction = subFlow(signTransactionFlow)
        subFlow(
            ReceiveFinalityFlow(
                otherSession,
                expectedTxId = transaction.id,
                statesToRecord = StatesToRecord.ALL_VISIBLE
            )
        )
    }
}
