package org.rooftop.netx.engine

import org.rooftop.netx.api.Codec
import org.rooftop.netx.api.TransactionEvent
import org.rooftop.netx.api.TransactionManager
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import kotlin.reflect.KClass

internal abstract class AbstractOrchestrateListener<T : Any, V : Any> internal constructor(
    private val orchestratorId: String,
    private val orchestrateSequence: Int,
    private val codec: Codec,
    private val transactionManager: TransactionManager,
) {

    var isFirst: Boolean = true
    var isLast: Boolean = true

    var nextOrchestrateListener: AbstractOrchestrateListener<V, Any>? = null
    var nextRollbackOrchestrateListener: AbstractOrchestrateListener<V, Any>? = null
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
        rollback(it, transactionId, orchestrateEvent)
        Mono.empty()
    }

    private fun rollback(
        it: Throwable,
        transactionId: String,
        orchestrateEvent: OrchestrateEvent,
    ) {
        transactionManager.rollback(
            transactionId = transactionId,
            cause = it.message ?: it.localizedMessage,
            event = orchestrateEvent
        ).subscribeOn(Schedulers.boundedElastic()).subscribe()
    }

    override fun toString(): String {
        return "AbstractOrchestrateListener(orchestratorId='$orchestratorId', orchestrateSequence=$orchestrateSequence, codec=$codec, transactionManager=$transactionManager, isFirst=$isFirst, isLast=$isLast, nextOrchestrateListener=$nextOrchestrateListener, nextRollbackOrchestrateListener=$nextRollbackOrchestrateListener, castableType=$castableType)"
    }


}
