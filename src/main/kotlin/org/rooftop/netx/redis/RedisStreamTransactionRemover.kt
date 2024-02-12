package org.rooftop.netx.redis

import org.rooftop.netx.api.TransactionCommitEvent
import org.rooftop.netx.api.TransactionRollbackEvent
import org.springframework.context.event.EventListener
import org.springframework.data.domain.Range
import org.springframework.data.redis.core.ReactiveRedisTemplate
import reactor.core.scheduler.Schedulers
import reactor.util.retry.RetrySpec
import java.time.Duration

class RedisStreamTransactionRemover(
    private val nodeGroup: String,
    private val reactiveRedisTemplate: ReactiveRedisTemplate<String, ByteArray>,
) {

    @EventListener(TransactionCommitEvent::class)
    fun handleTransactionCommitEvent(event: TransactionCommitEvent) {
        deleteElastic(event.transactionId)
    }

    @EventListener(TransactionRollbackEvent::class)
    fun handleTransactionRollbackEvent(event: TransactionRollbackEvent) {
        deleteElastic(event.transactionId)
    }

    private fun deleteElastic(transactionId: String) {
        reactiveRedisTemplate.opsForStream<String, String>()
            .pending(transactionId, nodeGroup, Range.closed("-", "+"), Long.MAX_VALUE)
            .filter {
                when (it.get().toList().isEmpty()) {
                    true -> true
                    false -> error(TRANSACTION_IS_PENDING_STATUS)
                }
            }
            .retryWhen(retryIfTransactionPending)
            .flatMap {
                reactiveRedisTemplate.opsForSet()
                    .remove(nodeGroup, transactionId.toByteArray())
            }
            .subscribeOn(Schedulers.parallel())
            .subscribe()
    }

    companion object {
        private const val TRANSACTION_IS_PENDING_STATUS =
            "Transaction message remains in pending status."
        private val retryIfTransactionPending =
            RetrySpec.fixedDelay(Long.MAX_VALUE, Duration.ofMillis(3000))
                .jitter(1.0)
                .filter { it.message == TRANSACTION_IS_PENDING_STATUS }
    }
}
