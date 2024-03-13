package org.rooftop.netx.engine

import org.rooftop.netx.api.Codec
import org.rooftop.netx.api.TransactionException
import org.rooftop.netx.api.TransactionManager
import org.rooftop.netx.engine.core.Transaction
import org.rooftop.netx.engine.core.TransactionState
import org.rooftop.netx.engine.logging.info
import org.rooftop.netx.engine.logging.infoOnError
import org.rooftop.netx.engine.logging.warningOnError
import reactor.core.publisher.Mono

abstract class AbstractTransactionManager(
    private val codec: Codec,
    private val nodeGroup: String,
    private val nodeName: String,
    private val transactionIdGenerator: TransactionIdGenerator,
) : TransactionManager {

    final override fun <T> syncStart(undo: T): String {
        return start(undo).block()
            ?: throw TransactionException("Cannot start transaction \"$undo\"")
    }

    override fun <T, S> syncStart(undo: T, event: S): String {
        return start(undo, event).block()
            ?: throw TransactionException("Cannot start transaction \"$undo\" \"$event\"")
    }

    final override fun <T> syncJoin(transactionId: String, undo: T): String {
        return join(transactionId, undo).block()
            ?: throw TransactionException("Cannot join transaction \"$transactionId\", \"$undo\"")
    }

    override fun <T, S> syncJoin(transactionId: String, undo: T, event: S): String {
        return join(transactionId, undo, event).block()
            ?: throw TransactionException("Cannot join transaction \"$transactionId\", \"$undo\", \"$event\"")
    }

    final override fun syncExists(transactionId: String): String {
        return exists(transactionId).block()
            ?: throw TransactionException("Cannot exists transaction \"$transactionId\"")
    }

    final override fun syncCommit(transactionId: String): String {
        return commit(transactionId).block()
            ?: throw TransactionException("Cannot commit transaction \"$transactionId\"")
    }

    override fun <T> syncCommit(transactionId: String, event: T): String {
        return commit(transactionId, event).block()
            ?: throw TransactionException("Cannot commit transaction \"$transactionId\" \"$event\"")
    }

    final override fun syncRollback(transactionId: String, cause: String): String {
        return rollback(transactionId, cause).block()
            ?: throw TransactionException("Cannot rollback transaction \"$transactionId\", \"$cause\"")
    }

    override fun <T> syncRollback(transactionId: String, cause: String, event: T): String {
        return rollback(transactionId, cause, event).block()
            ?: throw TransactionException("Cannot rollback transaction \"$transactionId\", \"$cause\" \"$event\"")
    }

    final override fun <T> start(undo: T): Mono<String> {
        return Mono.fromCallable { codec.encode(undo) }
            .flatMap { encodedUndo ->
                startTransaction(encodedUndo, null)
                    .info("Start transaction undo \"$undo\"")
            }
            .contextWrite { it.put(CONTEXT_TX_KEY, transactionIdGenerator.generate()) }
    }

    override fun <T, S> start(undo: T, event: S): Mono<String> {
        return Mono.fromCallable { codec.encode(undo) }
            .map { it to codec.encode(event) }
            .flatMap { (encodedUndo, encodedEvent) ->
                startTransaction(encodedUndo, encodedEvent)
                    .info("Start transaction undo \"$undo\"")
            }
            .contextWrite { it.put(CONTEXT_TX_KEY, transactionIdGenerator.generate()) }
    }

    private fun startTransaction(undo: String, event: String?): Mono<String> {
        return Mono.deferContextual<String> { Mono.just(it[CONTEXT_TX_KEY]) }
            .flatMap { transactionId ->
                publishTransaction(
                    transactionId, Transaction(
                        id = transactionId,
                        serverId = nodeName,
                        group = nodeGroup,
                        state = TransactionState.START,
                        undo = undo,
                        event = event,
                    )
                )
            }
    }

    final override fun <T> join(transactionId: String, undo: T): Mono<String> {
        return getAnyTransaction(transactionId)
            .map {
                if (it == TransactionState.COMMIT) {
                    throw TransactionException("Cannot join transaction cause, transaction \"$transactionId\" already \"${it.name}\"")
                }
                transactionId
            }
            .warningOnError("Cannot join transaction")
            .map { codec.encode(undo) }
            .flatMap {
                joinTransaction(transactionId, it, null)
                    .info("Join transaction transactionId \"$transactionId\", undo \"$undo\"")
            }
    }

    override fun <T, S> join(transactionId: String, undo: T, event: S): Mono<String> {
        return getAnyTransaction(transactionId)
            .map {
                if (it == TransactionState.COMMIT) {
                    throw TransactionException("Cannot join transaction cause, transaction \"$transactionId\" already \"${it.name}\"")
                }
                transactionId
            }
            .warningOnError("Cannot join transaction")
            .map { codec.encode(undo) to codec.encode(event) }
            .flatMap { (encodedUndo, encodedEvent) ->
                joinTransaction(transactionId, encodedUndo, encodedEvent)
                    .info("Join transaction transactionId \"$transactionId\", undo \"$undo\"")
            }
    }

    private fun joinTransaction(transactionId: String, undo: String, event: String?): Mono<String> {
        return publishTransaction(
            transactionId, Transaction(
                id = transactionId,
                serverId = nodeName,
                group = nodeGroup,
                state = TransactionState.JOIN,
                undo = undo,
                event = event,
            )
        )
    }

    final override fun rollback(transactionId: String, cause: String): Mono<String> {
        return exists(transactionId)
            .infoOnError("Cannot rollback transaction cause, transaction \"$transactionId\" is not exists")
            .flatMap {
                rollbackTransaction(transactionId, cause, null)
            }
            .info("Rollback transaction \"$transactionId\"")
            .contextWrite { it.put(CONTEXT_TX_KEY, transactionId) }
    }

    override fun <T> rollback(transactionId: String, cause: String, event: T): Mono<String> {
        return exists(transactionId)
            .infoOnError("Cannot rollback transaction cause, transaction \"$transactionId\" is not exists")
            .map { codec.encode(event) }
            .flatMap { encodedEvent ->
                rollbackTransaction(transactionId, cause, encodedEvent)
            }
            .info("Rollback transaction \"$transactionId\"")
            .contextWrite { it.put(CONTEXT_TX_KEY, transactionId) }
    }

    private fun rollbackTransaction(
        transactionId: String,
        cause: String,
        event: String?
    ): Mono<String> {
        return publishTransaction(
            transactionId, Transaction(
                id = transactionId,
                serverId = nodeName,
                group = nodeGroup,
                state = TransactionState.ROLLBACK,
                cause = cause,
                event = event
            )
        )
    }

    final override fun commit(transactionId: String): Mono<String> {
        return exists(transactionId)
            .infoOnError("Cannot commit transaction cause, transaction \"$transactionId\" is not exists")
            .flatMap { commitTransaction(transactionId, null) }
            .info("Commit transaction \"$transactionId\"")
            .contextWrite { it.put(CONTEXT_TX_KEY, transactionId) }
    }

    override fun <T> commit(transactionId: String, event: T): Mono<String> {
        return exists(transactionId)
            .infoOnError("Cannot commit transaction cause, transaction \"$transactionId\" is not exists")
            .map { codec.encode(event) }
            .flatMap { encodedEvent ->
                commitTransaction(transactionId, encodedEvent)
            }
            .info("Commit transaction \"$transactionId\"")
            .contextWrite { it.put(CONTEXT_TX_KEY, transactionId) }
    }

    private fun commitTransaction(transactionId: String, event: String?): Mono<String> {
        return publishTransaction(
            transactionId, Transaction(
                id = transactionId,
                serverId = nodeName,
                group = nodeGroup,
                state = TransactionState.COMMIT,
                event = event,
            )
        )
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

    protected abstract fun publishTransaction(
        transactionId: String,
        transaction: Transaction,
    ): Mono<String>

    private companion object {
        private const val CONTEXT_TX_KEY = "transactionId"
    }
}
