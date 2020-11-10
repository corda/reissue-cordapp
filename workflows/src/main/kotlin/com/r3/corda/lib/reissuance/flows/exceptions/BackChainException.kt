package com.r3.corda.lib.reissuance.flows.exceptions

import net.corda.core.flows.FlowException

class BackChainException(
    message: String, cause: Throwable? = null
) : FlowException(message, cause)
