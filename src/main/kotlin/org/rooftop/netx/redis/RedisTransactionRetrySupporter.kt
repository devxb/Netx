package org.rooftop.netx.redis

import org.redisson.api.RedissonReactiveClient
import org.rooftop.netx.engine.AbstractTransactionDispatcher
import org.rooftop.netx.engine.AbstractTransactionRetrySupporter
import org.rooftop.netx.idl.Transaction
import org.springframework.data.domain.Range
import org.springframework.data.redis.connection.RedisStreamCommands.XClaimOptions
import org.springframework.data.redis.core.ReactiveRedisTemplate
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.concurrent.TimeUnit

class RedisTransactionRetrySupporter(
    private val nodeGroup: String,
    private val nodeName: String,
    private val reactiveRedisTemplate: ReactiveRedisTemplate<String, ByteArray>,
    private val redissonReactiveClient: RedissonReactiveClient,
    private val transactionDispatcher: AbstractTransactionDispatcher,
    private val orphanMilli: Long,
    recoveryMilli: Long,
) : AbstractTransactionRetrySupporter(recoveryMilli) {

    override fun watchTransaction(transactionId: String): Mono<String> {
        return reactiveRedisTemplate.opsForSet()
            .add(nodeGroup, transactionId.toByteArray())
            .map { transactionId }
    }

    override fun handleOrphanTransaction(): Flux<Pair<Transaction, String>> {
        return reactiveRedisTemplate.opsForSet()
            .members(nodeGroup)
            .flatMap { claimTransactions(String(it)) }
            .publishOn(Schedulers.parallel())
            .flatMap { transactionDispatcher.dispatchAndAck(it.first, it.second) }
    }

    private fun claimTransactions(transactionId: String): Flux<Pair<Transaction, String>> {
        return reactiveRedisTemplate.opsForStream<String, String>()
            .pending(transactionId, nodeGroup, Range.closed("-", "+"), Long.MAX_VALUE)
            .filter { it.get().toList().isNotEmpty() }
            .flatMap { pendingMessage ->
                redissonReactiveClient.getLock("$nodeGroup-key")
                    .tryLock(0, orphanMilli, TimeUnit.MILLISECONDS)
                    .map { pendingMessage }
            }
            .flatMapMany {
                reactiveRedisTemplate.opsForStream<String, String>()
                    .claim(
                        transactionId, nodeGroup, nodeName, XClaimOptions
                            .minIdleMs(orphanMilli)
                            .ids(it.get().map { eachMessage -> eachMessage.id.value }.toList())
                    )
            }
            .map { Transaction.parseFrom(it.value["data"]?.toByteArray()) to it.id.toString() }
            .flatMap { transactionWithMessageId ->
                redissonReactiveClient.getLock("$nodeGroup-key")
                    .unlock()
                    .flatMapMany { Flux.just(transactionWithMessageId) }
            }
            .doOnError {
                redissonReactiveClient.getLock("$nodeGroup-key")
                    .unlock()
                    .subscribeOn(Schedulers.parallel())
                    .subscribe()
            }
    }
}