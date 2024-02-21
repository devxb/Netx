package org.rooftop.netx.redis

import org.rooftop.netx.api.*
import org.rooftop.netx.meta.TransactionHandler
import reactor.core.publisher.Mono

@TransactionHandler
class MonoTransactionHandlerAssertions : AbstractTransactionHandlerAssertions() {

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

}
