package org.rooftop.netx.engine

import org.rooftop.netx.api.TransactionCommitEvent
import org.rooftop.netx.api.TransactionJoinEvent
import org.rooftop.netx.api.TransactionRollbackEvent
import org.rooftop.netx.api.TransactionStartEvent
import org.rooftop.netx.idl.Transaction
import org.rooftop.netx.idl.TransactionState
import org.springframework.context.event.EventListener
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

abstract class AbstractTransactionDispatcher(
    private val eventPublisher: EventPublisher,
) {

    @EventListener(SubscribeTransactionEvent::class)
    fun subscribeStream(event: SubscribeTransactionEvent): Flux<Transaction> {
        return receive(event)
            .dispatch()
    }

    protected abstract fun receive(event: SubscribeTransactionEvent): Flux<Transaction>

    private fun Flux<Transaction>.dispatch(): Flux<Transaction> {
        return this.flatMap {
            when (it.state) {
                TransactionState.TRANSACTION_STATE_JOIN -> publishJoin(it)
                TransactionState.TRANSACTION_STATE_COMMIT -> publishCommit(it)
                TransactionState.TRANSACTION_STATE_ROLLBACK -> publishRollback(it)
                TransactionState.TRANSACTION_STATE_START -> publishStart(it)
                else -> error("Cannot find matched transaction state \"${it.state}\"")
            }
        }
    }

    private fun publishJoin(it: Transaction): Mono<Transaction> {
        return Mono.just(it)
            .doOnNext {
                eventPublisher.publish(
                    TransactionJoinEvent(
                        it.id,
                        it.serverId
                    )
                )
            }
    }

    private fun publishCommit(it: Transaction): Mono<Transaction> {
        return Mono.just(it)
            .doOnNext { eventPublisher.publish(TransactionCommitEvent(it.id, it.serverId)) }
    }

    private fun publishRollback(transaction: Transaction): Mono<Transaction> {
        return findOwnTransaction(transaction)
            .doOnNext {
                eventPublisher.publish(
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
                eventPublisher.publish(TransactionStartEvent(it.id, it.serverId))
            }
    }
}
