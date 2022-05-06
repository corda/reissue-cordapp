package com.r3.corda.lib.reissuance.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.node.ServiceHub
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.transactions.WireTransaction

/**
 * Convert this [WireTransaction] into a [TransactionBuilder] instance.
 * @return [TransactionBuilder] instance.
 */
@Suspendable
fun WireTransaction.toTransactionBuilder(serviceHub: ServiceHub): TransactionBuilder {
    return TransactionBuilder(
        notary = this.notary,
        inputs = this.inputs.toMutableList(),
        attachments = this.attachments.toMutableList(),
        outputs = this.outputs.toMutableList(),
        commands = this.commands.toMutableList(),
        window = this.timeWindow,
        privacySalt = this.privacySalt,
        references = this.references.toMutableList(),
        serviceHub = serviceHub)
}