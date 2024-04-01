package org.rooftop.netx.redis

import org.rooftop.netx.api.SagaStartEvent
import org.rooftop.netx.api.SagaStartListener
import org.rooftop.netx.meta.SagaHandler
import reactor.core.publisher.Mono

@SagaHandler
internal class SagaNoRollbackForStorage {

    @SagaStartListener(
        RedisStreamSagaDispatcherNoRollbackForTest.IllegalArgumentExceptionEvent::class,
        noRollbackFor = [IllegalArgumentException::class]
    )
    fun noRollbackForIllegalArgumentException(sagaStartEvent: SagaStartEvent) {
        throw IllegalArgumentException("No Rollback for IllegalArgumentExceptionEvent")
    }

    @SagaStartListener(
        RedisStreamSagaDispatcherNoRollbackForTest.UnSupportedOperationExceptionEvent::class,
        noRollbackFor = [UnsupportedOperationException::class]
    )
    fun noRollbackForUnSupportedOperationException(sagaStartEvent: SagaStartEvent): Mono<Unit> {
        throw UnsupportedOperationException("No Rollback for UnSupportedOperationExceptionEvent")
    }

    @SagaStartListener(
        RedisStreamSagaDispatcherNoRollbackForTest.NoSuchElementExceptionEvent::class,
        noRollbackFor = [UnsupportedOperationException::class]
    )
    fun doRollbackCauseThrowNoSuchElementException(sagaStartEvent: SagaStartEvent) {
        throw NoSuchElementException("Rollback cause throw NoSuchElementExceptionEvent")
    }
}
