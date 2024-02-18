package org.rooftop.netx.redis

import org.redisson.api.RedissonReactiveClient
import org.rooftop.netx.engine.AbstractTransactionDispatcher
import org.rooftop.netx.engine.AbstractTransactionRetrySupporter
import org.rooftop.netx.idl.Transaction
import org.springframework.data.domain.Range
import org.springframework.data.redis.connection.RedisStreamCommands.XClaimOptions
import org.springframework.data.redis.core.ReactiveRedisTemplate
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import java.util.concurrent.TimeUnit

class RedisTransactionRetrySupporter(
    recoveryMilli: Long,
    private val nodeGroup: String,
    private val nodeName: String,
    private val reactiveRedisTemplate: ReactiveRedisTemplate<String, ByteArray>,
    private val redissonReactiveClient: RedissonReactiveClient,
    private val transactionDispatcher: AbstractTransactionDispatcher,
    private val orphanMilli: Long,
    private val lockKey: String = "$nodeGroup-key",
) : AbstractTransactionRetrySupporter(recoveryMilli) {

    override fun handleOrphanTransaction(): Flux<Pair<Transaction, String>> {
        return claimTransactions()
            .publishOn(Schedulers.parallel())
            .flatMap { (transaction, messageId) ->
                transactionDispatcher.dispatch(transaction, messageId)
                    .map { transaction to messageId }
            }
    }

    private fun claimTransactions(): Flux<Pair<Transaction, String>> {
        return reactiveRedisTemplate.opsForStream<String, String>()
            .pending(STREAM_KEY, nodeGroup, Range.closed("-", "+"), Long.MAX_VALUE)
            .filter { it.get().toList().isNotEmpty() }
            .flatMap { pendingMessage ->
                redissonReactiveClient.getLock(lockKey)
                    .tryLock(0, orphanMilli, TimeUnit.MILLISECONDS)
                    .map { pendingMessage }
            }
            .flatMapMany {
                reactiveRedisTemplate.opsForStream<String, String>()
                    .claim(
                        STREAM_KEY,
                        nodeGroup,
                        nodeName,
                        XClaimOptions.minIdleMs(orphanMilli)
                            .ids(it.get().map { eachMessage -> eachMessage.id.value }.toList())
                    )
            }
            .map { Transaction.parseFrom(it.value["data"]?.toByteArray()) to it.id.toString() }
            .flatMap { transactionWithMessageId ->
                redissonReactiveClient.getLock(lockKey)
                    .forceUnlock()
                    .flatMapMany { Flux.just(transactionWithMessageId) }
            }
            .doOnError {
                redissonReactiveClient.getLock(lockKey)
                    .unlock()
                    .subscribeOn(Schedulers.parallel())
                    .subscribe()
            }
    }

    private companion object {
        private const val STREAM_KEY = "NETX_STREAM"
    }
}
