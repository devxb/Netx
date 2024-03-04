package org.rooftop.netx.redis

import org.rooftop.netx.api.TransactionStartEvent
import org.rooftop.netx.api.TransactionStartListener
import org.rooftop.netx.meta.TransactionHandler
import reactor.core.publisher.Mono

@TransactionHandler
class TransactionNoRetryStorage {

    @TransactionStartListener(
        RedisDispatcherNoRetryForTest.IllegalArgumentExceptionEvent::class,
        noRetryFor = [IllegalArgumentException::class]
    )
    fun noRetryForIllegalArgumentException(transactionStartEvent: TransactionStartEvent) {
        throw IllegalArgumentException("No retry for IllegalArgumentExceptionEvent")
    }

    @TransactionStartListener(
        RedisDispatcherNoRetryForTest.UnSupportedOperationExceptionEvent::class,
        noRetryFor = [UnsupportedOperationException::class]
    )
    fun noRetryForUnSupportedOperationException(transactionStartEvent: TransactionStartEvent): Mono<Unit> {
        throw UnsupportedOperationException("No retry for UnSupportedOperationExceptionEvent")
    }

    @TransactionStartListener(
        RedisDispatcherNoRetryForTest.NoSuchElementExceptionEvent::class,
        noRetryFor = [UnsupportedOperationException::class]
    )
    fun doRetryCauseThrowNoSuchElementException(transactionStartEvent: TransactionStartEvent) {
        throw NoSuchElementException("Retry cause throw NoSuchElementExceptionEvent")
    }
}
