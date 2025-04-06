package org.rooftop.netx.engine

import org.rooftop.netx.api.SagaEvent
import org.rooftop.netx.api.SagaManager
import org.rooftop.netx.api.SagaRollbackEvent
import org.rooftop.netx.engine.deadletter.AbstractDeadLetterManager
import org.rooftop.netx.engine.logging.info
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

internal fun Mono<SagaEvent>.callNotPublish(function: NotPublishDispatchFunction): Mono<*> {
    return this.map { function.call(it) }
}

internal class NotPublishDispatchFunction(
    eventType: KClass<*>,
    function: KFunction<*>,
    handler: Any,
    noRollbackFor: Array<KClass<out Throwable>>,
    nextState: NextSagaState,
    sagaManager: SagaManager,
    private val abstractDeadLetterManager: AbstractDeadLetterManager,
) : AbstractDispatchFunction<Any?>(
    eventType,
    function,
    handler,
    noRollbackFor,
    nextState,
    sagaManager,
) {

    override fun call(sagaEvent: SagaEvent): Any {
        if (isProcessable(sagaEvent)) {
            return runCatching {
                info("Call NotPublisher SagaHandler \"${name()}\" with id \"${sagaEvent.id}\"")
                val result = function.call(handler, sagaEvent)
                info("Call NotPublisher SagaHandler success \\\"${name()}\\\" with id \\\"${sagaEvent.id}\\\"\"")
                result
            }.fold(
                onSuccess = { publishNextSaga(sagaEvent) },
                onFailure = {
                    if (sagaEvent is SagaRollbackEvent) {
                        abstractDeadLetterManager.addDeadLetter(sagaEvent)
                            .subscribeOn(Schedulers.boundedElastic())
                            .subscribe()
                        return@fold SKIP
                    }

                    if (isNoRollbackFor(it)) {
                        return@fold NO_ROLLBACK_FOR
                    }
                    rollback(sagaEvent, it)
                },
            )
        }
        return SKIP
    }

    private companion object {
        private const val NO_ROLLBACK_FOR = "SUCCESS"
        private const val SKIP = "SKIP"
    }
}
