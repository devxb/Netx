package org.rooftop.netx.client

import io.kotest.matchers.shouldBe
import org.rooftop.netx.api.*
import org.rooftop.netx.meta.TransactionHandler
import reactor.core.publisher.Mono

@TransactionHandler
class TransactionReceiveStorage(
    private val storage: MutableMap<String, MutableList<TransactionEvent>>,
) {

    fun clear() {
        storage.clear()
    }

    fun joinCountShouldBe(count: Int) {
        (storage["JOIN"]?.size ?: 0) shouldBe count
    }

    fun startCountShouldBe(count: Int) {
        (storage["START"]?.size ?: 0) shouldBe count
    }

    fun commitCountShouldBe(count: Int) {
        (storage["COMMIT"]?.size ?: 0) shouldBe count
    }

    fun rollbackCountShouldBe(count: Int) {
        (storage["ROLLBACK"]?.size ?: 0) shouldBe count
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
        storage.putIfAbsent(key, mutableListOf())
        storage[key]?.add(transaction)
    }
}
