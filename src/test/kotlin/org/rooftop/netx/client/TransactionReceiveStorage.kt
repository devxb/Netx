package org.rooftop.netx.client

import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import org.rooftop.netx.api.*
import org.rooftop.netx.meta.TransactionHandler
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.CopyOnWriteArrayList

@TransactionHandler
class TransactionReceiveStorage(
    private val storage: ConcurrentMap<String, CopyOnWriteArrayList<TransactionEvent>> = ConcurrentHashMap(),
) {

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
        return Mono.fromCallable { log("ROLLBACK", transaction) }
    }

    @TransactionStartHandler
    fun logStart(transaction: TransactionStartEvent): Mono<Unit> {
        return Mono.fromCallable { log("START", transaction) }
    }

    @TransactionJoinHandler
    fun logJoin(transaction: TransactionJoinEvent): Mono<Unit> {
        return Mono.fromCallable { log("JOIN", transaction) }
    }

    @TransactionCommitHandler
    fun logCommit(transaction: TransactionCommitEvent): Mono<Unit> {
        return Mono.fromCallable { log("COMMIT", transaction) }
    }

    private fun log(key: String, transaction: TransactionEvent) {
        storage.putIfAbsent(key, CopyOnWriteArrayList())
        storage[key]?.add(transaction)
    }
}
