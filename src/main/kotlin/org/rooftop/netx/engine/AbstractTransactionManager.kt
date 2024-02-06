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
    private val undoManager: UndoManager,
) : TransactionManager {

    final override fun start(undo: String): Mono<String> {
        return startTransaction()
            .subscribeTransaction()
            .saveUndoState(undo)
            .contextWrite { it.put(CONTEXT_TX_KEY, transactionIdGenerator.generate()) }
    }

    private fun startTransaction(): Mono<String> {
        return Mono.deferContextual<String> { Mono.just(it[CONTEXT_TX_KEY]) }
            .flatMap { transactionId ->
                publishTransaction(transactionId, transaction {
                    id = transactionId
                    serverId = nodeName
                    this.state = TransactionState.TRANSACTION_STATE_START
                })
            }
    }

    final override fun join(transactionId: String, undo: String): Mono<String> {
        return exists(transactionId)
            .joinTransaction()
            .subscribeTransaction()
            .saveUndoState(undo)
            .contextWrite { it.put(CONTEXT_TX_KEY, transactionId) }
    }

    private fun Mono<String>.joinTransaction(): Mono<String> {
        return flatMap { transactionId ->
            publishTransaction(transactionId, transaction {
                id = transactionId
                serverId = nodeName
                state = TransactionState.TRANSACTION_STATE_JOIN
            })
        }
    }

    private fun Mono<String>.subscribeTransaction(): Mono<String> {
        return this.doOnSuccess {
            eventPublisher.publish(SubscribeTransactionEvent(it))
        }
    }

    private fun Mono<String>.saveUndoState(undo: String): Mono<String> {
        return this.flatMap { undoManager.save(it, undo) }
    }

    final override fun rollback(transactionId: String, cause: String): Mono<String> {
        return exists(transactionId)
            .publishTransaction(transaction {
                id = transactionId
                serverId = nodeName
                state = TransactionState.TRANSACTION_STATE_ROLLBACK
                this.cause = cause
            })
            .contextWrite { it.put(CONTEXT_TX_KEY, transactionId) }
    }

    final override fun commit(transactionId: String): Mono<String> {
        return exists(transactionId)
            .publishTransaction(transaction {
                id = transactionId
                serverId = nodeName
                state = TransactionState.TRANSACTION_STATE_COMMIT
            })
            .contextWrite { it.put(CONTEXT_TX_KEY, transactionId) }
    }

    final override fun exists(transactionId: String): Mono<String> {
        return findAnyTransaction(transactionId)
            .switchIfEmpty(
                Mono.error {
                    IllegalStateException("Cannot find exists transaction id \"$transactionId\"")
                }
            ).mapTransactionId()
            .contextWrite { it.put(CONTEXT_TX_KEY, transactionId) }
    }

    protected abstract fun findAnyTransaction(transactionId: String): Mono<Transaction>

    protected fun Mono<*>.mapTransactionId(): Mono<String> {
        return this.flatMap {
            Mono.deferContextual { Mono.just(it["transactionId"]) }
        }
    }

    private fun Mono<String>.publishTransaction(transaction: Transaction): Mono<String> {
        return this.flatMap {
            publishTransaction(it, transaction)
        }
    }

    protected abstract fun publishTransaction(
        transactionId: String,
        transaction: Transaction,
    ): Mono<String>

    private companion object {
        private const val CONTEXT_TX_KEY = "transactionId"
    }
}
