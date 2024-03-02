package org.rooftop.netx.redis

import org.rooftop.netx.api.Codec
import org.rooftop.netx.engine.AbstractTransactionManager
import org.rooftop.netx.idl.Transaction
import org.rooftop.netx.idl.TransactionState
import org.springframework.data.redis.connection.stream.Record
import org.springframework.data.redis.core.ReactiveRedisTemplate
import reactor.core.publisher.Mono

class RedisStreamTransactionManager(
    nodeId: Int,
    codec: Codec,
    nodeName: String,
    private val nodeGroup: String,
    private val reactiveRedisTemplate: ReactiveRedisTemplate<String, ByteArray>,
) : AbstractTransactionManager(
    nodeId = nodeId,
    nodeName = nodeName,
    nodeGroup = nodeGroup,
    codec = codec,
) {

    override fun getAnyTransaction(transactionId: String): Mono<TransactionState> {
        return reactiveRedisTemplate
            .opsForHash<String, String>()[transactionId, STATE_KEY]
            .switchIfEmpty(
                Mono.error {
                    error("Cannot find exists transaction id \"$transactionId\"")
                }
            )
            .map { TransactionState.valueOf(it) }
    }

    override fun publishTransaction(transactionId: String, transaction: Transaction): Mono<String> {
        return reactiveRedisTemplate.opsForStream<String, ByteArray>()
            .add(
                Record.of<String?, String?, ByteArray?>(mapOf(DATA to transaction.toByteArray()))
                    .withStreamKey(STREAM_KEY)
            )
            .flatMap {
                if (hasUndo(transaction)) {
                    return@flatMap reactiveRedisTemplate.opsForHash<String, String>()
                        .putAll(
                            transactionId, mapOf(
                                STATE_KEY to transaction.state.name,
                                nodeGroup to transaction.undo
                            )
                        )
                }
                reactiveRedisTemplate.opsForHash<String, String>()
                    .putAll(
                        transactionId, mapOf(
                            STATE_KEY to transaction.state.name,
                        )
                    )
            }
            .map { transactionId }
    }

    private fun hasUndo(transaction: Transaction): Boolean =
        transaction.state == TransactionState.TRANSACTION_STATE_JOIN
                || transaction.state == TransactionState.TRANSACTION_STATE_START

    private companion object {
        private const val DATA = "data"
        private const val STREAM_KEY = "NETX_STREAM"
        private const val STATE_KEY = "TX_STATE"
    }
}
