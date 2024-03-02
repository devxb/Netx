package org.rooftop.netx.engine

import org.rooftop.netx.api.TransactionManager
import org.rooftop.netx.api.Codec
import org.rooftop.netx.engine.logging.info
import org.rooftop.netx.engine.logging.infoOnError
import org.rooftop.netx.engine.logging.warningOnError
import org.rooftop.netx.idl.Transaction
import org.rooftop.netx.idl.TransactionState
import org.rooftop.netx.idl.transaction
import reactor.core.publisher.Mono

abstract class AbstractTransactionManager(
    nodeId: Int,
    private val codec: Codec,
    private val nodeGroup: String,
    private val nodeName: String,
    private val transactionIdGenerator: TransactionIdGenerator = TransactionIdGenerator(nodeId),
) : TransactionManager {

    final override fun <T> syncStart(undo: T): String {
        return start(undo).block() ?: error("Cannot start transaction")
    }

    final override fun <T> syncJoin(transactionId: String, undo: T): String {
        return join(transactionId, undo).block()
            ?: error("Cannot join transaction \"$transactionId\", \"$undo\"")
    }

    final override fun syncExists(transactionId: String): String {
        return exists(transactionId).block()
            ?: error("Cannot exists transaction \"$transactionId\"")
    }

    final override fun syncCommit(transactionId: String): String {
        return commit(transactionId).block()
            ?: error("Cannot commit transaction \"$transactionId\"")
    }

    final override fun syncRollback(transactionId: String, cause: String): String {
        return rollback(transactionId, cause).block()
            ?: error("Cannot rollback transaction \"$transactionId\", \"$cause\"")
    }

    final override fun <T> start(undo: T): Mono<String> {
        return Mono.fromCallable { codec.encode(undo) }
            .flatMap { encodedUndo ->
                startTransaction(encodedUndo)
                    .info("Start transaction undo \"$undo\"")
            }
            .contextWrite { it.put(CONTEXT_TX_KEY, transactionIdGenerator.generate()) }
    }

    private fun startTransaction(undo: String): Mono<String> {
        return Mono.deferContextual<String> { Mono.just(it[CONTEXT_TX_KEY]) }
            .flatMap { transactionId ->
                publishTransaction(transactionId, transaction {
                    id = transactionId
                    serverId = nodeName
                    group = nodeGroup
                    this.state = TransactionState.TRANSACTION_STATE_START
                    this.undo = undo
                })
            }
    }

    final override fun <T> join(transactionId: String, undo: T): Mono<String> {
        return getAnyTransaction(transactionId)
            .map {
                if (it == TransactionState.TRANSACTION_STATE_COMMIT) {
                    error("Cannot join transaction cause, transaction \"$transactionId\" already \"${it.name}\"")
                }
                transactionId
            }
            .warningOnError("Cannot join transaction cause, transaction \"$transactionId\" already Rollback state")
            .map { codec.encode(undo) }
            .flatMap {
                joinTransaction(transactionId, it)
                    .info("Join transaction transactionId \"$transactionId\", undo \"$undo\"")
            }
    }

    private fun joinTransaction(transactionId: String, undo: String): Mono<String> {
        return publishTransaction(transactionId, transaction {
            id = transactionId
            serverId = nodeName
            group = nodeGroup
            state = TransactionState.TRANSACTION_STATE_JOIN
            this.undo = undo
        })
    }

    final override fun rollback(transactionId: String, cause: String): Mono<String> {
        return exists(transactionId)
            .infoOnError("Cannot rollback transaction cause, transaction \"$transactionId\" is not exists")
            .publishTransaction(transaction {
                id = transactionId
                serverId = nodeName
                group = nodeGroup
                state = TransactionState.TRANSACTION_STATE_ROLLBACK
                this.cause = cause
            })
            .info("Rollback transaction \"$transactionId\"")
            .contextWrite { it.put(CONTEXT_TX_KEY, transactionId) }
    }

    final override fun commit(transactionId: String): Mono<String> {
        return exists(transactionId)
            .infoOnError("Cannot commit transaction cause, transaction \"$transactionId\" is not exists")
            .publishTransaction(transaction {
                id = transactionId
                serverId = nodeName
                group = nodeGroup
                state = TransactionState.TRANSACTION_STATE_COMMIT
            })
            .info("Commit transaction \"$transactionId\"")
            .contextWrite { it.put(CONTEXT_TX_KEY, transactionId) }
    }

    final override fun exists(transactionId: String): Mono<String> {
        return getAnyTransaction(transactionId)
            .infoOnError("There is no transaction corresponding to transactionId \"$transactionId\"")
            .mapTransactionId()
            .contextWrite { it.put(CONTEXT_TX_KEY, transactionId) }
    }

    protected abstract fun getAnyTransaction(transactionId: String): Mono<TransactionState>

    private fun Mono<*>.mapTransactionId(): Mono<String> {
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
