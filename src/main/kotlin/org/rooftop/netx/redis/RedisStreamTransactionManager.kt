package org.rooftop.netx.redis

import org.redisson.api.RedissonReactiveClient
import org.rooftop.netx.engine.AbstractTransactionManager
import org.rooftop.netx.engine.AbstractTransactionRetrySupporter
import org.rooftop.netx.idl.Transaction
import org.rooftop.netx.idl.TransactionState
import org.springframework.data.redis.connection.stream.Record
import org.springframework.data.redis.core.ReactiveRedisTemplate
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.concurrent.TimeUnit

class RedisStreamTransactionManager(
    nodeId: Int,
    nodeName: String,
    nodeGroup: String,
    transactionRetrySupporter: AbstractTransactionRetrySupporter,
    private val reactiveRedisTemplate: ReactiveRedisTemplate<String, ByteArray>,
    private val redissonReactiveClient: RedissonReactiveClient,
) : AbstractTransactionManager(
    nodeId = nodeId,
    nodeName = nodeName,
    nodeGroup = nodeGroup,
) {

    override fun findAnyTransaction(transactionId: String): Mono<TransactionState> {
        return reactiveRedisTemplate
            .opsForValue()[transactionId]
            .switchIfEmpty(
                Mono.error {
                    error("Cannot find exists transaction id \"$transactionId\"")
                }
            )
            .map { TransactionState.valueOf(String(it)) }
    }

    override fun publishTransaction(transactionId: String, transaction: Transaction): Mono<String> {
        return reactiveRedisTemplate.opsForStream<String, ByteArray>()
            .add(
                Record.of<String?, String?, ByteArray?>(mapOf(DATA to transaction.toByteArray()))
                    .withStreamKey(STREAM_KEY)
            )
            .flatMap {
                redissonReactiveClient.getLock("$transactionId-key")
                    .tryLock(10, TimeUnit.MINUTES)
            }
            .flatMap {
                reactiveRedisTemplate.opsForValue()
                    .set(transactionId, transaction.state.name.toByteArray())
            }
            .doFinally {
                redissonReactiveClient.getLock("$transactionId-key")
                    .forceUnlock()
                    .subscribeOn(Schedulers.parallel())
                    .subscribe()
            }
            .map { transactionId }
    }

    private companion object {
        private const val DATA = "data"
        private const val STREAM_KEY = "NETX_STREAM"
    }
}
