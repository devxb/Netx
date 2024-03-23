package org.rooftop.netx.engine

import org.rooftop.netx.api.*
import org.rooftop.netx.api.OrchestratorFactory

class OrchestratorFactory internal constructor(
    private val transactionManager: TransactionManager,
    private val transactionDispatcher: AbstractTransactionDispatcher,
    private val codec: Codec,
    private val resultHolder: ResultHolder,
    private val requestHolder: RequestHolder,
) : OrchestratorFactory {

    override fun <T : Any, V : Any> get(orchestratorId: String): Orchestrator<T, V> =
        OrchestratorCache.get(orchestratorId)

    override fun <T : Any> create(orchestratorId: String): OrchestrateChain.Pre<T> {
        return DefaultOrchestrateChain.Pre(
            orchestratorId = orchestratorId,
            transactionManager = transactionManager,
            transactionDispatcher = transactionDispatcher,
            codec = codec,
            resultHolder = resultHolder,
            requestHolder = requestHolder,
        )
    }
}
