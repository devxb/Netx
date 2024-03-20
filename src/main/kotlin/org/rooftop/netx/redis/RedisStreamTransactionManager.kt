package org.rooftop.netx.redis

import com.fasterxml.jackson.databind.ObjectMapper
import org.rooftop.netx.api.Codec
import org.rooftop.netx.api.TransactionException
import org.rooftop.netx.engine.AbstractTransactionManager
import org.rooftop.netx.engine.TransactionIdGenerator
import org.rooftop.netx.engine.core.Transaction
import org.rooftop.netx.engine.core.TransactionState
import org.springframework.data.redis.connection.stream.Record
import org.springframework.data.redis.core.ReactiveRedisTemplate
import reactor.core.publisher.Mono

class RedisStreamTransactionManager(
    codec: Codec,
    nodeName: String,
    transactionIdGenerator: TransactionIdGenerator,
    private val nodeGroup: String,
    private val reactiveRedisTemplate: ReactiveRedisTemplate<String, Transaction>,
    private val objectMapper: ObjectMapper,
) : AbstractTransactionManager(
    nodeName = nodeName,
    nodeGroup = nodeGroup,
    codec = codec,
    transactionIdGenerator = transactionIdGenerator
) {

    override fun getAnyTransaction(transactionId: String): Mono<TransactionState> {
        return reactiveRedisTemplate
            .opsForHash<String, String>()[transactionId, STATE_KEY]
            .switchIfEmpty(
                Mono.error {
                    throw TransactionException("Cannot find exists transaction id \"$transactionId\"")
                }
            )
            .map { TransactionState.valueOf(it) }
    }

    /*
        --- 요구사항 ---

        1. transaciton의 join마다, undo를 설정할 수 있어야함. -> 새로운 인터페이스를 만든다..?
        2. 자신이 참여하지 않은 transaction을 수신하지 않아야함. -> 방법이 없다면.. transactionId에 참여중인 서버의 group을 알고있어야할듯
            --> silent drop undo가 없다면 참여하지 않은것.
     */
    override fun publishTransaction(transactionId: String, transaction: Transaction): Mono<String> {
        return Mono.fromCallable { hasUndo(transaction) }
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
            .map { objectMapper.writeValueAsString(transaction) }
            .flatMap {
                reactiveRedisTemplate.opsForStream<String, Transaction>()
                    .add(
                        Record.of<String, String, String>(mapOf(DATA to it))
                            .withStreamKey(STREAM_KEY)
                    )
            }
            .map { transactionId }
    }

    private fun hasUndo(transaction: Transaction): Boolean =
        transaction.state == TransactionState.JOIN || transaction.state == TransactionState.START

    private companion object {
        private const val DATA = "data"
        private const val STREAM_KEY = "NETX_STREAM"
        private const val STATE_KEY = "TX_STATE"
    }
}
