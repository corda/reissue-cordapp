package com.template.flows.example.tokens

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.commands.MoveTokenCommand
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import net.corda.core.contracts.Amount
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class TransferTokens(
    private val issuer: Party,
    private val newTokenHolderParty: Party,
    private val tokensNum: Long
) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {
        val holderParty: Party = ourIdentity

        val signers = listOf(
            holderParty.owningKey
        )

        val availableTokens = subFlow(ListTokensFlow())
        val issuedTokenType = IssuedTokenType(issuer, TokenType("token", 0))
        val (tokensToTransfer, change) = splitTokensIntoTokensToTransferAndChange(
            availableTokens, tokensNum.toInt(), issuedTokenType, holderParty, newTokenHolderParty
        )

        val transactionBuilder = TransactionBuilder(notary = getPreferredNotary(serviceHub))
        availableTokens.forEach { transactionBuilder.addInputState(it) }
        transactionBuilder.addOutputState(tokensToTransfer)
        if(change != null)
            transactionBuilder.addOutputState(change)

        transactionBuilder.addCommand(MoveTokenCommand(
            issuedTokenType,
            inputs = (availableTokens.indices).toList(),
            outputs = if(change == null) listOf(0) else listOf(0, 1)
        ), signers)

        transactionBuilder.verify(serviceHub)
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)

        val newHolderSession = initiateFlow(newTokenHolderParty)
        subFlow(
            FinalityFlow(
                transaction = signedTransaction,
                sessions = listOf(newHolderSession)
            )
        )
    }

    private fun splitTokensIntoTokensToTransferAndChange(
        inputs: List<StateAndRef<FungibleToken>>,
        tokensToTransfer: Int,
        issuedTokenType: IssuedTokenType,
        holderParty: Party,
        newHolderParty: Party
    ): Pair<FungibleToken, FungibleToken?> {
        val availableTokens = inputs.sumBy { it.state.data.amount.quantity.toInt() }
        require(availableTokens >= tokensToTransfer) { "Insufficient tokens. Required $tokensToTransfer, but have $availableTokens" }

        val tokenToTransfer = FungibleToken(Amount(tokensToTransfer.toLong(), issuedTokenType), newHolderParty)

        val changeToken = if (availableTokens == tokensToTransfer) null
        else {
            val changeAmount = Amount((availableTokens - tokensToTransfer).toLong(), issuedTokenType)
            FungibleToken(changeAmount, holderParty)
        }
        return Pair(tokenToTransfer, changeToken)
    }

}


@InitiatedBy(TransferTokens::class)
class TransferTokensResponder(
    private val otherSession: FlowSession
) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(
            ReceiveFinalityFlow(
                otherSession,
                statesToRecord = StatesToRecord.ALL_VISIBLE
            )
        )
    }
}
