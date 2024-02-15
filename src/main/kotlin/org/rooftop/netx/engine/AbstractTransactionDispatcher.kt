package org.rooftop.netx.engine

import org.rooftop.netx.api.*
import org.rooftop.netx.idl.Transaction
import org.rooftop.netx.idl.TransactionState
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import kotlin.reflect.KFunction

abstract class AbstractTransactionDispatcher {

    protected val transactionHandlerFunctions =
        mutableMapOf<TransactionState, MutableList<Pair<KFunction<Mono<Any>>, Any>>>()

    protected abstract fun initHandlers()

    fun dispatch(transaction: Transaction, messageId: String): Flux<Any> {
        return Mono.just(transaction.state)
            .doOnNext { deleteElastic(transaction, messageId) }
            .filter { state -> transactionHandlerFunctions.containsKey(state) }
            .flatMapMany { state ->
                Flux.fromIterable(
                    transactionHandlerFunctions[state]
                        ?: throw cannotFindMatchedHandlerFunctionException
                )
            }
            .flatMap { (function, instance) ->
                mapToTransactionEvent(transaction)
                    .flatMap { function.call(instance, it) }
            }
            .doOnComplete {
                ack(transaction, messageId)
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe()
            }
    }

    private fun mapToTransactionEvent(transaction: Transaction): Mono<TransactionEvent> {
        return when (transaction.state) {
            TransactionState.TRANSACTION_STATE_START -> Mono.just(
                TransactionStartEvent(
                    transaction.id,
                    transaction.serverId,
                    transaction.group
                )
            )

            TransactionState.TRANSACTION_STATE_COMMIT -> Mono.just(
                TransactionCommitEvent(
                    transaction.id,
                    transaction.serverId,
                    transaction.group,
                )
            )

            TransactionState.TRANSACTION_STATE_JOIN -> Mono.just(
                TransactionJoinEvent(
                    transaction.id,
                    transaction.serverId,
                    transaction.group,
                )
            )

            TransactionState.TRANSACTION_STATE_ROLLBACK -> findOwnTransaction(transaction)
                .map {
                    TransactionRollbackEvent(
                        transaction.id,
                        transaction.serverId,
                        transaction.group,
                        transaction.cause,
                        it.undo,
                    )
                }

            else -> throw cannotFindMatchedTransactionEventException
        }
    }

    protected abstract fun findOwnTransaction(transaction: Transaction): Mono<Transaction>

    protected abstract fun ack(
        transaction: Transaction,
        messageId: String
    ): Mono<Pair<Transaction, String>>

    protected abstract fun deleteElastic(
        transaction: Transaction,
        messageId: String
    )

    private companion object {
        private val cannotFindMatchedTransactionEventException =
            java.lang.IllegalStateException("Cannot find matched transaction event")

        private val cannotFindMatchedHandlerFunctionException =
            IllegalStateException("Cannot find matched handler function")
    }
}
