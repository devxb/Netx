package org.rooftop.netx.client

import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import org.rooftop.netx.api.*
import org.rooftop.netx.meta.TransactionHandler
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

@TransactionHandler
class TransactionReceiveStorage(
    private val storage: ConcurrentMap<String, MutableList<TransactionEvent>> = ConcurrentHashMap(),
) {

    private val logger = LoggerFactory.getLogger(this::class.simpleName)

    fun clear() {
        storage.clear()
    }

    fun joinCountShouldBeGreaterThanOrEqual(count: Int) {
        (storage["JOIN"]?.size ?: 0) shouldBeGreaterThanOrEqual count
    }

    fun startCountShouldBeGreaterThanOrEqual(count: Int) {
        (storage["START"]?.size ?: 0) shouldBeGreaterThanOrEqual count
    }

    fun commitCountShouldBeGreaterThanOrEqual(count: Int) {
        (storage["COMMIT"]?.size ?: 0) shouldBeGreaterThanOrEqual count
    }

    fun rollbackCountShouldBeGreaterThanOrEqual(count: Int) {
        (storage["ROLLBACK"]?.size ?: 0) shouldBeGreaterThanOrEqual count
    }

    @TransactionRollbackHandler
    fun logRollback(transaction: TransactionRollbackEvent): Mono<Unit> {
        logger.info("Receive transaction RollbackEvent ${storage["ROLLBACK"]?.size ?: 0} ")
        return Mono.fromCallable { log("ROLLBACK", transaction) }
    }

    @TransactionStartHandler
    fun logStart(transaction: TransactionStartEvent): Mono<Unit> {
        logger.info("Receive transaction StartEvent ${storage["START"]?.size ?: 0} ")
        return Mono.fromCallable { log("START", transaction) }
    }

    @TransactionJoinHandler
    fun logJoin(transaction: TransactionJoinEvent): Mono<Unit> {
        logger.info("Receive transaction JoinEvent ${storage["JOIN"]?.size ?: 0} ")
        return Mono.fromCallable { log("JOIN", transaction) }
    }

    @TransactionCommitHandler
    fun logCommit(transaction: TransactionCommitEvent): Mono<Unit> {
        logger.info("Receive transaction CommitEvent ${storage["COMMIT"]?.size ?: 0} ")
        return Mono.fromCallable { log("COMMIT", transaction) }
    }

    private fun log(key: String, transaction: TransactionEvent) {
        storage.putIfAbsent(key, mutableListOf())
        storage[key]?.add(transaction)
    }
}
