package com.r3.corda.lib.reissuance.flows

import net.corda.core.flows.FlowException

open class ReissuanceException(
    message: String, cause: Throwable? = null
) : FlowException(message, cause)

class BackChainException(
    message: String, cause: Throwable? = null
) : ReissuanceException(message, cause)

