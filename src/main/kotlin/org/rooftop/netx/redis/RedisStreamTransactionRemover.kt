package org.rooftop.netx.redis

import org.rooftop.netx.idl.Transaction
import org.rooftop.netx.idl.TransactionState
import org.springframework.data.domain.Range
import org.springframework.data.redis.core.ReactiveRedisTemplate
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.util.retry.RetrySpec
import java.time.Duration

class RedisStreamTransactionRemover(
    private val nodeGroup: String,
    private val reactiveRedisTemplate: ReactiveRedisTemplate<String, ByteArray>,
) {

    fun deleteElastic(transaction: Transaction) {
        Mono.just(transaction)
            .filter { isTransactionEndState(transaction) }
            .flatMap {
                reactiveRedisTemplate.opsForStream<String, String>()
                    .pending(transaction.id, nodeGroup, Range.closed("-", "+"), Long.MAX_VALUE)
                    .filter {
                        when (it.get().toList().isEmpty()) {
                            true -> true
                            false -> error(TRANSACTION_IS_PENDING_STATUS)
                        }
                    }
                    .retryWhen(retryIfTransactionPending)
                    .flatMap {
                        reactiveRedisTemplate.opsForSet()
                            .remove(nodeGroup, transaction.id.toByteArray())
                    }
            }.subscribeOn(Schedulers.parallel())
            .subscribe()
    }

    private fun isTransactionEndState(transaction: Transaction): Boolean {
        return transaction.state == TransactionState.TRANSACTION_STATE_ROLLBACK
                || transaction.state == TransactionState.TRANSACTION_STATE_COMMIT
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
