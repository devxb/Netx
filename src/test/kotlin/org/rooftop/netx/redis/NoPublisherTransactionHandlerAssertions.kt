package org.rooftop.netx.redis

import org.rooftop.netx.api.*
import org.rooftop.netx.meta.TransactionHandler

@TransactionHandler
class NoPublisherTransactionHandlerAssertions : AbstractTransactionHandlerAssertions() {

    @TransactionRollbackHandler
    fun handleRollback(event: TransactionRollbackEvent): Long {
        put("ROLLBACK")
        return Long.MIN_VALUE
    }

    @TransactionCommitHandler
    fun handleCommit(event: TransactionCommitEvent) {
        put("COMMIT")
    }

    @TransactionStartHandler
    fun handleStart(event: TransactionStartEvent): Foo {
        put("START")
        return Foo("START")
    }

    @TransactionJoinHandler
    fun handleJoin(event: TransactionJoinEvent): Any {
        put("JOIN")
        return Any::class
    }

    class Foo(name: String)

}
