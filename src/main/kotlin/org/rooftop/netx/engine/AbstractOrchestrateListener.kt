package org.rooftop.netx.engine

import org.rooftop.netx.api.Codec
import org.rooftop.netx.api.Context
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
    var rollbackSequence: Int = orchestrateSequence

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

    internal fun Mono<Pair<V, Context>>.setNextCastableType(): Mono<Pair<V, Context>> {
        return this.doOnNext { (request, _) ->
            nextOrchestrateListener?.castableType = request::class
            nextRollbackOrchestrateListener?.castableType = request::class
        }
    }

    protected fun Mono<OrchestrateEvent>.getHeldRequest(transactionEvent: TransactionEvent): Mono<Pair<T, OrchestrateEvent>> {
        return this.flatMap { event ->
            requestHolder.getRequest(
                "${transactionEvent.transactionId}:$orchestrateSequence",
                getCastableType()
            ).map { it to event }
        }
    }

    protected fun holdRequestIfRollbackable(request: T, transactionId: String): Mono<T> {
        if (!isRollbackable) {
            Mono.just(request)
        }
        return requestHolder.setRequest(
            "$transactionId:$orchestrateSequence",
            request
        )
    }

    protected fun Mono<Pair<V, Context>>.toOrchestrateEvent(): Mono<OrchestrateEvent> {
        return this.map { (response, context) ->
            OrchestrateEvent(
                orchestratorId = orchestratorId,
                orchestrateSequence = orchestrateSequence + 1,
                clientEvent = codec.encode(response),
                context = codec.encode(context.contexts),
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
        throwable.stackTrace = arrayOf()
        resultHolder.setFailResult(transactionId, throwable)
            .subscribeOn(Schedulers.parallel()).subscribe()
    }

    private fun rollback(
        transactionId: String,
        throwable: Throwable,
        orchestrateEvent: OrchestrateEvent,
    ) {
        val rollbackOrchestrateEvent =
            OrchestrateEvent(
                orchestrateEvent.orchestratorId,
                rollbackSequence,
                "",
                orchestrateEvent.context,
            )
        transactionManager.rollback(
            transactionId = transactionId,
            cause = throwable.message ?: throwable.localizedMessage,
            event = rollbackOrchestrateEvent
        ).subscribeOn(Schedulers.parallel()).subscribe()
    }

    override fun toString(): String {
        return "AbstractOrchestrateListener(orchestrateSequence=$orchestrateSequence, " +
                "isFirst=$isFirst, isLast=$isLast, isRollbackable=$isRollbackable, " +
                "beforeRollbackOrchestrateSequence=$beforeRollbackOrchestrateSequence, " +
                "rollbackSequence=$rollbackSequence)"
    }


}
