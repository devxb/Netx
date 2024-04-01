package org.rooftop.netx.engine

import org.rooftop.netx.api.*
import org.rooftop.netx.api.OrchestratorFactory
import org.rooftop.netx.core.Codec

internal class OrchestratorFactory internal constructor(
    private val sagaManager: SagaManager,
    private val sagaDispatcher: AbstractSagaDispatcher,
    private val codec: Codec,
    private val resultHolder: ResultHolder,
    private val requestHolder: RequestHolder,
) : OrchestratorFactory {

    private val orchestratorCache: OrchestratorCache = OrchestratorCache()

    override fun <T : Any, V : Any> get(orchestratorId: String): Orchestrator<T, V> =
        orchestratorCache.get(orchestratorId)

    override fun <T : Any> create(orchestratorId: String): OrchestrateChain.Pre<T> {
        return DefaultOrchestrateChain.Pre(
            orchestratorId = orchestratorId,
            sagaManager = sagaManager,
            sagaDispatcher = sagaDispatcher,
            codec = codec,
            resultHolder = resultHolder,
            requestHolder = requestHolder,
            orchestratorCache = orchestratorCache,
        )
    }
}
