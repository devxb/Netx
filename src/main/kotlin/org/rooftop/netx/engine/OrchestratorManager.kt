package org.rooftop.netx.engine

import org.rooftop.netx.api.Orchestrator
import org.rooftop.netx.api.Result
import org.rooftop.netx.api.SagaException
import org.rooftop.netx.api.SagaManager
import org.rooftop.netx.core.Codec
import org.rooftop.netx.engine.listen.AbstractOrchestrateListener
import reactor.core.publisher.Mono
import kotlin.time.Duration.Companion.milliseconds

internal class OrchestratorManager<T : Any, V : Any> internal constructor(
    private val sagaManager: SagaManager,
    private val codec: Codec,
    private val orchestratorId: String,
    private val resultHolder: ResultHolder,
    private val orchestrateListener: AbstractOrchestrateListener<T, out Any>,
    private val rollbackOrchestrateListener: AbstractOrchestrateListener<T, out Any>?,
) : Orchestrator<T, V> {

    override fun sagaSync(request: T): Result<V> {
        return saga(request).block()
            ?: throw SagaException("Cannot start saga \"$request\"")
    }

    override fun sagaSync(timeoutMillis: Long, request: T): Result<V> {
        return saga(timeoutMillis, request).block()
            ?: throw SagaException("Cannot start saga \"$request\"")
    }

    override fun sagaSync(request: T, context: Map<String, Any>): Result<V> {
        return saga(request, context).block()
            ?: throw SagaException("Cannot start saga \"$request\"")
    }

    override fun sagaSync(
        timeoutMillis: Long,
        request: T,
        context: Map<String, Any>
    ): Result<V> {
        return saga(timeoutMillis, request, context).block()
            ?: throw SagaException("Cannot start saga \"$request\"")
    }

    override fun saga(request: T): Mono<Result<V>> {
        return saga(TEN_SECONDS_TO_TIME_OUT, request, mutableMapOf())
    }

    override fun saga(timeoutMillis: Long, request: T): Mono<Result<V>> {
        return saga(timeoutMillis, request, mutableMapOf())
    }

    override fun saga(request: T, context: Map<String, Any>): Mono<Result<V>> {
        return saga(TEN_SECONDS_TO_TIME_OUT, request, context)
    }

    override fun saga(
        timeoutMillis: Long,
        request: T,
        context: Map<String, Any>
    ): Mono<Result<V>> {
        return Mono.just(request)
            .doOnNext { _ ->
                orchestrateListener.setCastableType(request::class)
                rollbackOrchestrateListener?.setCastableType(request::class)
            }
            .map {
                OrchestrateEvent(
                    orchestratorId = orchestratorId,
                    clientEvent = codec.encode(request),
                    context = codec.encode(context.mapValues { codec.encode(it.value) })
                )
            }
            .flatMap { sagaManager.start(it) }
            .flatMap { resultHolder.getResult(timeoutMillis.milliseconds, it) }
    }

    private companion object {
        private const val TEN_SECONDS_TO_TIME_OUT = 10000L
    }
}
