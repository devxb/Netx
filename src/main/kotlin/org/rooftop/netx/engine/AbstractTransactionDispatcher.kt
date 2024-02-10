package org.rooftop.netx.engine

import org.rooftop.netx.api.TransactionCommitEvent
import org.rooftop.netx.api.TransactionJoinEvent
import org.rooftop.netx.api.TransactionRollbackEvent
import org.rooftop.netx.api.TransactionStartEvent
import org.rooftop.netx.idl.Transaction
import org.rooftop.netx.idl.TransactionState
import org.springframework.context.ApplicationEventPublisher
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

abstract class AbstractTransactionDispatcher(
    private val eventPublisher: ApplicationEventPublisher,
) {

    fun subscribeStream(transactionId: String): Flux<Pair<Transaction, String>> {
        return receive(transactionId)
            .flatMap { dispatchAndAck(it.first, it.second) }
    }

    protected abstract fun receive(transactionId: String): Flux<Pair<Transaction, String>>

    fun dispatchAndAck(transaction: Transaction, messageId: String): Flux<Pair<Transaction, String>> {
        return Flux.just(transaction to messageId)
            .dispatch()
            .ack()
    }

    private fun Flux<Pair<Transaction, String>>.dispatch(): Flux<Pair<Transaction, String>> {
        return this.flatMap { (transaction, messageId) ->
            when (transaction.state) {
                TransactionState.TRANSACTION_STATE_JOIN -> publishJoin(transaction)
                TransactionState.TRANSACTION_STATE_COMMIT -> publishCommit(transaction)
                TransactionState.TRANSACTION_STATE_ROLLBACK -> publishRollback(transaction)
                TransactionState.TRANSACTION_STATE_START -> publishStart(transaction)
                else -> error("Cannot find matched transaction state \"${transaction.state}\"")
            }.map { transaction to messageId }
        }
    }

    private fun publishJoin(it: Transaction): Mono<Transaction> {
        return Mono.just(it)
            .doOnNext {
                eventPublisher.publishEvent(
                    TransactionJoinEvent(
                        it.id,
                        it.serverId
                    )
                )
            }
    }

    private fun publishCommit(it: Transaction): Mono<Transaction> {
        return Mono.just(it)
            .doOnNext { eventPublisher.publishEvent(TransactionCommitEvent(it.id, it.serverId)) }
    }

    private fun publishRollback(transaction: Transaction): Mono<Transaction> {
        return findOwnTransaction(transaction)
            .doOnNext {
                eventPublisher.publishEvent(
                    TransactionRollbackEvent(
                        transaction.id,
                        transaction.serverId,
                        transaction.cause,
                        it.undo
                    )
                )
            }
            .map { transaction }
    }

    protected abstract fun findOwnTransaction(transaction: Transaction): Mono<Transaction>

    private fun publishStart(it: Transaction): Mono<Transaction> {
        return Mono.just(it)
            .doOnNext {
                eventPublisher.publishEvent(TransactionStartEvent(it.id, it.serverId))
            }
    }

    protected abstract fun Flux<Pair<Transaction, String>>.ack(): Flux<Pair<Transaction, String>>
}
