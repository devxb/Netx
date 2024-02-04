package org.rooftop.netx.engine

import org.rooftop.netx.api.TransactionManager
import org.rooftop.netx.idl.Transaction
import org.rooftop.netx.idl.TransactionState
import org.rooftop.netx.idl.transaction
import reactor.core.publisher.Mono

abstract class AbstractTransactionManager(
    nodeId: Int,
    private val nodeName: String,
    private val eventPublisher: EventPublisher,
    private val transactionIdGenerator: TransactionIdGenerator = TransactionIdGenerator(nodeId),
) : TransactionManager {

    override fun start(replay: String): Mono<String> {
        return startTransaction(replay)
            .subscribeTransaction()
            .contextWrite { it.put(CONTEXT_TX_KEY, transactionIdGenerator.generate()) }
    }

    private fun startTransaction(replay: String): Mono<String> {
        return Mono.deferContextual<String> { Mono.just(it[CONTEXT_TX_KEY]) }
            .flatMap { transactionId ->
                publishTransaction(transactionId, transaction {
                    id = transactionId
                    serverId = nodeName
                    this.replay = replay
                    this.state = TransactionState.TRANSACTION_STATE_START
                })
            }
    }

    override fun join(transactionId: String, replay: String): Mono<String> {
        return exists(transactionId)
            .joinTransaction(replay)
            .subscribeTransaction()
            .contextWrite { it.put(CONTEXT_TX_KEY, transactionId) }
    }

    private fun Mono<String>.joinTransaction(replay: String): Mono<String> {
        return flatMap { transactionId ->
                publishTransaction(transactionId, transaction {
                    id = transactionId
                    serverId = nodeName
                    this.replay = replay
                    state = TransactionState.TRANSACTION_STATE_JOIN
                })
            }
    }

    private fun Mono<String>.subscribeTransaction(): Mono<String> {
        return this.doOnSuccess {
            eventPublisher.publish(SubscribeTransactionEvent(it))
        }
    }

    override fun rollback(transactionId: String, cause: String): Mono<String> {
        return exists(transactionId)
            .publishTransaction(transaction {
                id = transactionId
                serverId = nodeName
                state = TransactionState.TRANSACTION_STATE_ROLLBACK
                this.cause = cause
            })
            .contextWrite { it.put(CONTEXT_TX_KEY, transactionId) }
    }

    override fun commit(transactionId: String): Mono<String> {
        return exists(transactionId)
            .publishTransaction(transaction {
                id = transactionId
                serverId = nodeName
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