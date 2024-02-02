package org.rooftop.netx.engine

import org.rooftop.netx.api.TransactionIdGenerator
import org.rooftop.netx.api.TransactionManager
import org.rooftop.netx.idl.Transaction
import org.rooftop.netx.idl.TransactionState
import org.rooftop.netx.idl.transaction
import reactor.core.publisher.Mono

abstract class AbstractTransactionManager(
    private val appServerId: String,
    private val eventPublisher: EventPublisher,
    private val transactionIdGenerator: TransactionIdGenerator,
) : TransactionManager {

    override fun start(replay: String): Mono<String> {
        return startTransaction(replay)
            .publishJoinedEvent()
            .contextWrite { it.put(CONTEXT_TX_KEY, transactionIdGenerator.generate()) }
    }

    private fun startTransaction(replay: String): Mono<String> {
        return Mono.deferContextual<String> { Mono.just(it[CONTEXT_TX_KEY]) }
            .flatMap { transactionId ->
                publishTransaction(transactionId, transaction {
                    id = transactionId
                    serverId = appServerId
                    this.replay = replay
                })
            }
    }

    override fun join(transactionId: String, replay: String): Mono<String> {
        return exists(transactionId)
            .joinTransaction(replay)
            .publishJoinedEvent()
            .contextWrite { it.put(CONTEXT_TX_KEY, transactionId) }
    }

    private fun Mono<String>.joinTransaction(replay: String): Mono<String> {
        return flatMap { transactionId ->
                publishTransaction(transactionId, transaction {
                    id = transactionId
                    serverId = appServerId
                    this.replay = replay
                    state = TransactionState.TRANSACTION_STATE_JOIN
                })
            }
    }

    private fun Mono<String>.publishJoinedEvent(): Mono<String> {
        return this.doOnSuccess {
            eventPublisher.publish(TransactionJoinedEvent(it))
        }
    }

    override fun rollback(transactionId: String, cause: String): Mono<String> {
        return exists(transactionId)
            .publishTransaction(transaction {
                id = transactionId
                serverId = appServerId
                state = TransactionState.TRANSACTION_STATE_ROLLBACK
                this.cause = cause
            })
            .contextWrite { it.put(CONTEXT_TX_KEY, transactionId) }
    }

    override fun commit(transactionId: String): Mono<String> {
        return exists(transactionId)
            .publishTransaction(transaction {
                id = transactionId
                serverId = appServerId
                state = TransactionState.TRANSACTION_STATE_COMMIT
            })
            .contextWrite { it.put(CONTEXT_TX_KEY, transactionId) }
    }

    private fun Mono<String>.publishTransaction(transaction: Transaction): Mono<String> {
        return this.flatMap {
            publishTransaction(it, transaction)
        }
    }

    abstract fun publishTransaction(transactionId: String, transaction: Transaction): Mono<String>

    private companion object {
        private const val CONTEXT_TX_KEY = "transactionId"
    }
}
