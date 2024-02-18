package org.rooftop.netx.redis

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

class NoAckRedisStreamTransactionDispatcher(
    private val applicationContext: ApplicationContext,
    private val reactiveRedisTemplate: ReactiveRedisTemplate<String, ByteArray>,
    private val nodeGroup: String,
) : AbstractTransactionDispatcher() {
    @Suppress("Unchecked_cast")
    override fun initHandlers() {
        val transactionHandler = findHandlers(TransactionHandler::class)
        transactionHandler.forEach { handler ->
            handler::class.declaredMemberFunctions
                .filter { it.returnType is Mono<*> }
                .forEach { function ->
                    function.annotations
                        .forEach { annotation ->
                            runCatching {
                                val transactionState = matchedTransactionState(annotation)
                                val handlerFunctions = transactionHandlerFunctions.getOrDefault(
                                    transactionState,
                                    mutableListOf()
                                )
                                handlerFunctions.add(function as KFunction<Mono<Any>> to handler)
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

    override fun findOwnUndo(transaction: Transaction): Mono<String> {
        return reactiveRedisTemplate.opsForHash<String, String>()[transaction.id, nodeGroup]
            .switchIfEmpty(
                Mono.error {
                    error("Cannot find undo state in transaction hashes key \"${transaction.id}\"")
                }
            )
    }

    override fun ack(transaction: Transaction, messageId: String): Mono<Pair<Transaction, String>> =
        Mono.just(transaction to messageId)

    private companion object {
        private val notMatchedTransactionHandlerException =
            IllegalStateException("Cannot find matched Transaction handler")
    }
}

