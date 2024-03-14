package org.rooftop.netx.engine

import org.rooftop.netx.api.TransactionEvent
import org.rooftop.netx.engine.logging.info
import reactor.core.publisher.Mono
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

internal fun Mono<TransactionEvent>.callNotPublish(function: NotPublishDispatchFunction): Mono<*> {
    return this.map { function.call(it) }
}

internal class NotPublishDispatchFunction(
    eventType: KClass<*>,
    function: KFunction<*>,
    handler: Any,
    noRetryFor: Array<KClass<out Throwable>>,
) : AbstractDispatchFunction<Any?>(eventType, function, handler, noRetryFor) {

    override fun call(transactionEvent: TransactionEvent): Any? {
        if (!isDecodable(transactionEvent)) {
            return SKIP
        }
        runCatching {
            val result = function.call(handler, transactionEvent)
            info("Call NotPublisher TransactionHandler \"${name()}\" with transactionId \"${transactionEvent.transactionId}\"")
            return result
        }.onFailure { throwable ->
            if (isNoRetryFor(throwable)) {
                info("Call NotPublisher TransactionHandler \"${name()}\" with transactionId \"${transactionEvent.transactionId}\" no retry for mode")
                return SUCCESS_CAUSE_NO_RETRY_FOR
            }
            throw throwable
        }
        throw IllegalStateException("Unreachable code")
    }

    private fun isDecodable(transactionEvent: TransactionEvent): Boolean {
        runCatching { transactionEvent.decodeEvent(eventType) }
            .onFailure {
                return it is NullPointerException && eventType == Any::class
            }
        return true
    }

    private companion object {
        private const val SUCCESS_CAUSE_NO_RETRY_FOR = "SUCCESS"
        private const val SKIP = "SKIP"
    }
}
