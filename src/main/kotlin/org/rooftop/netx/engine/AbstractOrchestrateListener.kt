package org.rooftop.netx.engine

import org.rooftop.netx.api.Codec
import org.rooftop.netx.api.TransactionEvent
import org.rooftop.netx.api.TransactionManager
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import kotlin.reflect.KClass

internal abstract class AbstractOrchestrateListener<T : Any, V : Any> internal constructor(
    private val orchestratorId: String,
    val orchestrateSequence: Int,
    private val codec: Codec,
    private val transactionManager: TransactionManager,
    private val requestHolder: RequestHolder,
    private val resultHolder: ResultHolder,
) {

    var isFirst: Boolean = true
    var isLast: Boolean = true
    var isRollbackable: Boolean = false
    var beforeRollbackOrchestrateSequence: Int = 0

    private var nextOrchestrateListener: AbstractOrchestrateListener<V, Any>? = null
    private var nextRollbackOrchestrateListener: AbstractOrchestrateListener<V, Any>? = null
    private var castableType: KClass<out T>? = null

    @Suppress("UNCHECKED_CAST")
    internal fun setNextOrchestrateListener(nextOrchestrateListener: AbstractOrchestrateListener<out Any, out Any>) {
        this.nextOrchestrateListener =
            nextOrchestrateListener as AbstractOrchestrateListener<V, Any>
    }

    @Suppress("UNCHECKED_CAST")
    internal fun setNextRollbackOrchestrateListener(nextRollbackOrchestrateListener: AbstractOrchestrateListener<out Any, out Any>) {
        this.nextRollbackOrchestrateListener =
            nextRollbackOrchestrateListener as AbstractOrchestrateListener<V, Any>
    }

    internal fun setCastableType(type: KClass<out T>) {
        castableType = type
    }

    internal fun Mono<V>.setNextCastableType(): Mono<V> {
        return this.doOnNext {
            nextOrchestrateListener?.castableType = it::class
            nextRollbackOrchestrateListener?.castableType = it::class
        }
    }

    protected fun Mono<*>.getHeldRequest(transactionEvent: TransactionEvent): Mono<T> {
        return this.flatMap {
            requestHolder.getRequest(
                "${transactionEvent.transactionId}:$orchestrateSequence",
                getCastableType()
            )
        }
    }

    protected fun Mono<T>.holdRequestIfRollbackable(transactionEvent: TransactionEvent): Mono<T> {
        return this.flatMap { request ->
            if (!isRollbackable) {
                Mono.just(request)
            }
            requestHolder.setRequest(
                "${transactionEvent.transactionId}:$orchestrateSequence",
                request
            )
        }
    }

    protected fun getCastableType(): KClass<out T> {
        return castableType
            ?: throw NullPointerException("OrchestratorId \"$orchestratorId\", OrchestrateSequence \"$orchestrateSequence\"'s CastableType was null")
    }

    protected fun cast(data: String): T {
        return castableType?.let {
            codec.decode(data, it)
        } ?: throw NullPointerException("Cannot cast \"$data\" cause, castableType is null")
    }

    protected fun TransactionEvent.toOrchestrateEvent(): Mono<OrchestrateEvent> =
        Mono.just(this.decodeEvent(OrchestrateEvent::class))

    protected fun <S> Mono<S>.onErrorRollback(
        transactionId: String,
        orchestrateEvent: OrchestrateEvent,
    ): Mono<S> = this.onErrorResume {
        holdFailResult(transactionId, it)
        rollback(transactionId, it, orchestrateEvent)
        Mono.empty()
    }

    private fun holdFailResult(transactionId: String, throwable: Throwable) {
        resultHolder.setFailResult(transactionId, throwable)
            .subscribeOn(Schedulers.parallel()).subscribe()
    }

    private fun rollback(
        transactionId: String,
        throwable: Throwable,
        orchestrateEvent: OrchestrateEvent,
    ) {
        transactionManager.rollback(
            transactionId = transactionId,
            cause = throwable.message ?: throwable.localizedMessage,
            event = orchestrateEvent
        ).subscribeOn(Schedulers.parallel()).subscribe()
    }

    override fun toString(): String {
        return "AbstractOrchestrateListener(orchestratorId='$orchestratorId', orchestrateSequence=$orchestrateSequence, codec=$codec, transactionManager=$transactionManager, requestHolder=$requestHolder, isFirst=$isFirst, isLast=$isLast, isRollbackable=$isRollbackable, beforeRollbackOrchestrateSequence=$beforeRollbackOrchestrateSequence, nextOrchestrateListener=$nextOrchestrateListener, nextRollbackOrchestrateListener=$nextRollbackOrchestrateListener, castableType=$castableType)"
    }


}
