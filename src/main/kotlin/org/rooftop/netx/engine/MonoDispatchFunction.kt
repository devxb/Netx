package org.rooftop.netx.engine

import org.rooftop.netx.api.TransactionEvent
import org.rooftop.netx.engine.logging.info
import reactor.core.publisher.Mono
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

internal fun Mono<TransactionEvent>.callMono(function: MonoDispatchFunction): Mono<*> {
    return this.flatMap {
        function.call(it)
    }
}

internal class MonoDispatchFunction(
    eventType: KClass<*>,
    function: KFunction<Mono<*>>,
    handler: Any,
    noRetryFor: Array<KClass<out Throwable>>,
) : AbstractDispatchFunction<Mono<*>>(eventType, function, handler, noRetryFor) {

    override fun call(transactionEvent: TransactionEvent): Mono<*> {
        runCatching { transactionEvent.decodeEvent(eventType) }
            .fold(
                onSuccess = {
                    return Mono.just(DEFAULT_MONO)
                        .flatMap { function.call(handler, transactionEvent) }
                        .onErrorResume { throwable ->
                            if (!noRetryFor.contains(throwable.cause!!::class)) {
                                throw throwable
                            }
                            info("Call Mono TransactionHandler \"${name()}\" with transactionId \"${transactionEvent.transactionId}\" no retry for mode")
                            Mono.empty()
                        }
                        .info("Call Mono TransactionHandler \"${name()}\" with transactionId \"${transactionEvent.transactionId}\"")
                },
                onFailure = {
                    if (it is NullPointerException && eventType == Any::class) {
                        return Mono.just(DEFAULT_MONO)
                            .flatMap { function.call(handler, transactionEvent) }
                            .onErrorResume { throwable ->
                                if (!noRetryFor.contains(throwable.cause!!::class)) {
                                    throw throwable
                                }
                                info("Call Mono TransactionHandler \"${name()}\" with transactionId \"${transactionEvent.transactionId}\" no retry for mode")
                                Mono.empty()
                            }
                            .info("Call Mono TransactionHandler \"${name()}\" with transactionId \"${transactionEvent.transactionId}\"")
                    }
                    return Mono.just("Skip \"${name()}\" handler")
                }
            )
    }

    private companion object {
        private const val DEFAULT_MONO = "DEFAULT"
    }
}
