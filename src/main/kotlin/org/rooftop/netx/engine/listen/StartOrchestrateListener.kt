package org.rooftop.netx.engine.listen

import org.rooftop.netx.api.*
import org.rooftop.netx.core.Codec
import org.rooftop.netx.engine.OrchestrateEvent
import org.rooftop.netx.engine.RequestHolder
import org.rooftop.netx.engine.ResultHolder
import reactor.core.publisher.Mono

internal class StartOrchestrateListener<T : Any, V : Any>(
    private val codec: Codec,
    private val sagaManager: SagaManager,
    private val orchestratorId: String,
    orchestrateSequence: Int,
    private val orchestrateCommand: OrchestrateCommand<T, V>,
    private val requestHolder: RequestHolder,
    private val resultHolder: ResultHolder,
    private val typeReference: TypeReference<T>?,
    private val group: String,
) : AbstractOrchestrateListener<T, V>(
    orchestratorId,
    orchestrateSequence,
    codec,
    sagaManager,
    requestHolder,
    resultHolder,
    typeReference,
    group,
) {

    override fun withAnnotated(): AbstractOrchestrateListener<T, V> {
        return when {
            isFirst && isLast -> this.successWithCommit()
            isFirst && !isLast -> this.successWithJoin()
            else -> error("Cannot annotated")
        }
    }

    private fun successWithJoin(): AbstractOrchestrateListener<T, V> {
        return object : AbstractOrchestrateListener<T, V>(
            orchestratorId,
            orchestrateSequence,
            codec,
            sagaManager,
            requestHolder,
            resultHolder,
            typeReference,
            group,
        ) {
            @SagaStartListener(
                event = OrchestrateEvent::class,
                successWith = SuccessWith.PUBLISH_JOIN
            )
            fun handleSagaStartEvent(sagaStartEvent: SagaStartEvent): Mono<OrchestrateEvent> {
                return orchestrate(sagaStartEvent)
            }

            override fun command(request: T, event: OrchestrateEvent): Mono<Pair<V, Context>> {
                return Mono.fromCallable { orchestrateCommand.command(request, event.context) }
            }
        }
    }

    private fun successWithCommit(): AbstractOrchestrateListener<T, V> {
        return object : AbstractOrchestrateListener<T, V>(
            orchestratorId,
            orchestrateSequence,
            codec,
            sagaManager,
            requestHolder,
            resultHolder,
            typeReference,
            group,
        ) {
            @SagaStartListener(
                event = OrchestrateEvent::class,
                successWith = SuccessWith.PUBLISH_COMMIT
            )
            fun handleSagaStartEvent(sagaStartEvent: SagaStartEvent): Mono<OrchestrateEvent> {
                return orchestrate(sagaStartEvent)
            }

            override fun command(request: T, event: OrchestrateEvent): Mono<Pair<V, Context>> {
                return Mono.fromCallable { orchestrateCommand.command(request, event.context) }
            }
        }
    }
}
