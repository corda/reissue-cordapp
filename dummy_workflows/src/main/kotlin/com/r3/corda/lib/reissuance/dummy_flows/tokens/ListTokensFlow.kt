package com.r3.corda.lib.reissuance.dummy_flows.tokens

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.internal.schemas.PersistentFungibleToken
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder

@StartableByRPC
class ListTokensFlow(
) : FlowLogic<List<StateAndRef<FungibleToken>>>() {

    @Suspendable
    override fun call(): List<StateAndRef<FungibleToken>> {
        val ourIdentity: Party = ourIdentity

        val tokenType = TokenType("token", 0)
        val tokenTypeCriteria = QueryCriteria.VaultCustomQueryCriteria(
            builder { PersistentFungibleToken::tokenIdentifier.equal(tokenType.tokenIdentifier) })
        val tokenHolderCriteria = QueryCriteria.VaultCustomQueryCriteria(
            builder { PersistentFungibleToken::holder.equal(ourIdentity) })
        val criteria = tokenTypeCriteria.and(tokenHolderCriteria)
        val availableTokens = serviceHub.vaultService.queryBy<FungibleToken>(criteria).states
        return availableTokens
    }
}
