package org.rooftop.netx.engine

import org.rooftop.netx.api.SagaEvent
import org.rooftop.netx.api.SagaManager
import org.rooftop.netx.api.SagaRollbackEvent
import org.rooftop.netx.engine.deadletter.AbstractDeadLetterManager
import org.rooftop.netx.engine.logging.info
import reactor.core.publisher.Mono
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

internal fun Mono<SagaEvent>.callOrchestrate(function: OrchestrateDispatchFunction): Mono<*> {
    return this.flatMap {
        function.call(it)
    }
}

internal class OrchestrateDispatchFunction(
    eventType: KClass<*>,
    function: KFunction<Mono<*>>,
    handler: Any,
    noRetryFor: Array<KClass<out Throwable>>,
    nextState: NextSagaState,
    sagaManager: SagaManager,
    private val abstractDeadLetterManager: AbstractDeadLetterManager,
) : AbstractDispatchFunction<Mono<*>>(
    eventType,
    function,
    handler,
    noRetryFor,
    nextState,
    sagaManager,
) {

    override fun call(sagaEvent: SagaEvent): Mono<*> {
        return Mono.just(sagaEvent)
            .filter { isProcessable(sagaEvent) }
            .map { sagaEvent.copy() }
            .info("Call OrchestrateHandler \"${name()}\" with id \"${sagaEvent.id}\"")
            .flatMap { function.call(handler, sagaEvent) }
            .info("Call OrchestratorHandler success \"${name()}\" with id \"${sagaEvent.id}\"")
            .map {
                publishNextSaga(sagaEvent)
                it
            }
            .onErrorResume {
                if (sagaEvent is SagaRollbackEvent) {
                    abstractDeadLetterManager.addDeadLetter(sagaEvent.copy())
                } else {
                    Mono.error(it)
                }
            }
            .switchIfEmpty(`continue`)
            .onErrorResume {
                if (isNoRollbackFor(it)) {
                    return@onErrorResume noRollbackFor
                }
                `continue`
            }
    }

    private companion object {
        private val `continue` = Mono.just("CONTINUE")
        private val noRollbackFor = Mono.just("NO_ROLLBACK_FOR")
    }
}
