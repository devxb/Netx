package org.rooftop.netx.factory

import org.rooftop.netx.api.Codec
import org.rooftop.netx.api.TransactionManager
import org.rooftop.netx.engine.AbstractTransactionDispatcher
import org.rooftop.netx.engine.OrchestrateChain
import org.rooftop.netx.engine.RequestHolder
import org.rooftop.netx.engine.ResultHolder

class OrchestratorFactory internal constructor(
    private val transactionManager: TransactionManager,
    private val transactionDispatcher: AbstractTransactionDispatcher,
    private val codec: Codec,
    private val resultHolder: ResultHolder,
    private val requestHolder: RequestHolder,
) {

    fun <T : Any> create(orchestratorId: String): OrchestrateChain.Pre<T> {
        return OrchestrateChain.Pre(
            orchestratorId = orchestratorId,
            transactionManager = transactionManager,
            transactionDispatcher = transactionDispatcher,
            codec = codec,
            resultHolder = resultHolder,
            requestHolder = requestHolder,
        )
    }
}
