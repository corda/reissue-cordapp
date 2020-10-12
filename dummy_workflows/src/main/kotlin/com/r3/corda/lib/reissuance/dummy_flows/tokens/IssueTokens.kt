package com.r3.corda.lib.reissuance.dummy_flows.tokens

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.workflows.flows.issue.addIssueTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokensHandler
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.ObserverAwareFinalityFlow
import com.r3.corda.lib.tokens.workflows.utilities.addTokenTypeJar
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import net.corda.core.contracts.Amount
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class IssueTokens(
    private val tokenHolderParty: Party,
    private val tokensNum: Long
) : FlowLogic<SecureHash>() {

    @Suspendable
    override fun call(): SecureHash {
        val issuerParty: Party = ourIdentity
        val issuedTokenType = IssuedTokenType(issuerParty, TokenType("token", 0))
        val amount = Amount(tokensNum, issuedTokenType)
        val token = FungibleToken(amount, tokenHolderParty)

        val transactionBuilder = TransactionBuilder(notary = getPreferredNotary(serviceHub))
        addIssueTokens(transactionBuilder, token)
        addTokenTypeJar(token, transactionBuilder)

        val holderSession = initiateFlow(tokenHolderParty)
        return subFlow(
            ObserverAwareFinalityFlow(
                transactionBuilder = transactionBuilder,
                allSessions = listOf(holderSession)
            )
        ).id
    }
}

@InitiatedBy(IssueTokens::class)
class IssueTokensResponder(
    private val otherSession: FlowSession
) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(IssueTokensHandler(otherSession))
    }
}
