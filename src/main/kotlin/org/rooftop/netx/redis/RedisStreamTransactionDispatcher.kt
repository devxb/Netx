package org.rooftop.netx.redis

import jakarta.annotation.PostConstruct
import org.rooftop.netx.api.TransactionCommitHandler
import org.rooftop.netx.api.TransactionJoinHandler
import org.rooftop.netx.api.TransactionRollbackHandler
import org.rooftop.netx.api.TransactionStartHandler
import org.rooftop.netx.engine.AbstractTransactionDispatcher
import org.rooftop.netx.idl.Transaction
import org.rooftop.netx.idl.TransactionState
import org.rooftop.netx.meta.TransactionHandler
import org.springframework.context.ApplicationContext
import org.springframework.data.redis.connection.stream.ReadOffset
import org.springframework.data.redis.connection.stream.StreamOffset
import org.springframework.data.redis.core.ReactiveRedisTemplate
import reactor.core.publisher.Mono
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredMemberFunctions

class RedisStreamTransactionDispatcher(
    private val applicationContext: ApplicationContext,
    private val reactiveRedisTemplate: ReactiveRedisTemplate<String, ByteArray>,
    private val redisStreamTransactionRemover: RedisStreamTransactionRemover,
    private val nodeGroup: String,
) : AbstractTransactionDispatcher() {

    @PostConstruct
    @Suppress("Unchecked_cast")
    override fun initHandlers() {
        val transactionHandler = findHandlers(TransactionHandler::class)
        transactionHandler.forEach { handler ->
            handler::class.declaredMemberFunctions
                .filter { it.returnType.classifier == Mono::class }
                .forEach { function ->
                    function.annotations
                        .forEach { annotation ->
                            runCatching {
                                val transactionState = matchedTransactionState(annotation)
                                transactionHandlerFunctions.putIfAbsent(
                                    transactionState,
                                    mutableListOf()
                                )
                                transactionHandlerFunctions[transactionState]?.add(function as KFunction<Mono<Any>> to handler)
                            }
                        }
                }
        }
    }

    private fun <T : Annotation> findHandlers(type: KClass<T>): List<Any> {
        return applicationContext.getBeansWithAnnotation(type.java)
            .entries.asSequence()
            .map { it.value }
            .toList()
    }

    private fun matchedTransactionState(annotation: Annotation): TransactionState {
        return when (annotation) {
            is TransactionStartHandler -> TransactionState.TRANSACTION_STATE_START
            is TransactionCommitHandler -> TransactionState.TRANSACTION_STATE_COMMIT
            is TransactionJoinHandler -> TransactionState.TRANSACTION_STATE_JOIN
            is TransactionRollbackHandler -> TransactionState.TRANSACTION_STATE_ROLLBACK
            else -> throw notMatchedTransactionHandlerException
        }
    }

    override fun findOwnTransaction(transaction: Transaction): Mono<Transaction> {
        return reactiveRedisTemplate.opsForStream<String, String>()
            .read(StreamOffset.create(transaction.id, ReadOffset.from("0")))
            .map { Transaction.parseFrom(it.value["data"]?.toByteArray()) }
            .filter { it.group == nodeGroup }
            .filter { hasUndo(it) }
            .next()
    }

    private fun hasUndo(transaction: Transaction): Boolean =
        transaction.state == TransactionState.TRANSACTION_STATE_JOIN
                || transaction.state == TransactionState.TRANSACTION_STATE_START

    override fun ack(transaction: Transaction, messageId: String): Mono<Pair<Transaction, String>> {
        return reactiveRedisTemplate.opsForStream<String, ByteArray>()
            .acknowledge(transaction.id, nodeGroup, messageId)
            .map { transaction to messageId }
            .switchIfEmpty(
                Mono.error {
                    error("Fail to ack transaction transactionId \"${transaction.id}\" messageId \"$messageId\"")
                }
            )
    }

    override fun deleteElastic(
        transaction: Transaction,
        messageId: String
    ) = redisStreamTransactionRemover.deleteElastic(transaction)

    private companion object {
        private val notMatchedTransactionHandlerException =
            IllegalStateException("Cannot find matched Transaction handler")
    }
}
