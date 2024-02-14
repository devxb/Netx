package org.rooftop.netx.redis

import io.kotest.matchers.shouldBe
import org.rooftop.netx.api.*
import org.rooftop.netx.meta.TransactionHandler
import reactor.core.publisher.Mono

@TransactionHandler
class TransactionHandlerAssertions {

    private val methodInvocationCounts = mutableMapOf<String, Int>()

    fun clear() {
        methodInvocationCounts.clear()
    }

    fun joinCountShouldBe(count: Int) {
        (methodInvocationCounts["JOIN"] ?: 0) shouldBe count
    }

    fun startCountShouldBe(count: Int) {
        (methodInvocationCounts["START"] ?: 0) shouldBe count
    }

    fun commitCountShouldBe(count: Int) {
        (methodInvocationCounts["COMMIT"] ?: 0) shouldBe count
    }

    fun rollbackCountShouldBe(count: Int) {
        (methodInvocationCounts["ROLLBACK"] ?: 0) shouldBe count
    }

    @TransactionRollbackHandler
    fun handleRollback(event: TransactionRollbackEvent): Mono<Unit> {
        put("ROLLBACK")
        return Mono.just(Unit)
    }

    @TransactionCommitHandler
    fun handleCommit(event: TransactionCommitEvent): Mono<Unit> {
        put("COMMIT")
        return Mono.just(Unit)
    }

    @TransactionStartHandler
    fun handleStart(event: TransactionStartEvent): Mono<Unit> {
        put("START")
        return Mono.just(Unit)
    }

    @TransactionJoinHandler
    fun handleJoin(event: TransactionJoinEvent): Mono<Unit> {
        put("JOIN")
        return Mono.just(Unit)
    }

    private fun put(key: String) {
        methodInvocationCounts[key] = methodInvocationCounts.getOrDefault(key, 0) + 1
    }
}
