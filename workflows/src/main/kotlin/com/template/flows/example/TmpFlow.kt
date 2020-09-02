package com.template.flows.example

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import com.template.contracts.example.SimpleStateContract
import com.template.states.example.SimpleState
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.TransactionBuilder

@StartableByRPC
class TmpFlow(
    private val tb: TransactionBuilder
): FlowLogic<Unit>() {
    @Suspendable
    override fun call() {

    }
}

