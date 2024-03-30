package org.rooftop.netx.engine

import org.rooftop.netx.api.SagaEvent
import org.rooftop.netx.api.SagaManager
import reactor.core.scheduler.Schedulers
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

internal sealed class AbstractDispatchFunction<T>(
    private val eventType: KClass<*>,
    protected val function: KFunction<T>,
    protected val handler: Any,
    private val noRollbackFor: Array<KClass<out Throwable>>,
    private val nextState: NextSagaState,
    private val sagaManager: SagaManager,
) {
    fun name(): String = function.name

    abstract fun call(sagaEvent: SagaEvent): T

    protected fun isNoRollbackFor(throwable: Throwable): Boolean {
        return noRollbackFor.isNotEmpty() && throwable.cause != null && noRollbackFor.contains(
            throwable.cause!!::class
        )
    }

    protected fun isProcessable(sagaEvent: SagaEvent): Boolean {
        return runCatching {
            sagaEvent.decodeEvent(eventType)
        }.onFailure {
            return it is NullPointerException && eventType == Any::class
        }.isSuccess
    }

    protected fun rollback(sagaEvent: SagaEvent, throwable: Throwable) {
        sagaEvent.nextEvent?.let {
            sagaManager.rollback(sagaEvent.id, throwable.getCause(), it)
                .subscribeOn(Schedulers.parallel())
                .subscribe()
        } ?: sagaManager.rollback(sagaEvent.id, throwable.getCause())
            .subscribeOn(Schedulers.parallel())
            .subscribe()
    }

    protected fun publishNextSaga(sagaEvent: SagaEvent) {
        when (nextState) {
            NextSagaState.JOIN -> sagaEvent.nextEvent?.let {
                sagaManager.join(sagaEvent.id, it)
            } ?: sagaManager.join(sagaEvent.id)

            NextSagaState.COMMIT -> sagaEvent.nextEvent?.let {
                sagaManager.commit(sagaEvent.id, it)
            } ?: sagaManager.commit(sagaEvent.id)

            NextSagaState.END -> return
        }.subscribeOn(Schedulers.parallel())
            .subscribe()
    }

    private fun Throwable.getCause(): String {
        return this.message ?: this.cause?.message ?: this::class.java.name
    }

    internal enum class NextSagaState {
        JOIN,
        COMMIT,
        END
    }
}
