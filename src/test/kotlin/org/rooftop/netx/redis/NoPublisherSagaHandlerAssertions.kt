package org.rooftop.netx.redis

import org.rooftop.netx.api.*
import org.rooftop.netx.meta.SagaHandler

@SagaHandler
class NoPublisherSagaHandlerAssertions : AbstractSagaHandlerAssertions() {

    @SagaRollbackListener
    fun handleRollback(event: SagaRollbackEvent): Long {
        put("ROLLBACK")
        return Long.MIN_VALUE
    }

    @SagaCommitListener
    fun handleCommit(event: SagaCommitEvent) {
        put("COMMIT")
    }

    @SagaStartListener(successWith = SuccessWith.END)
    fun handleStart(event: SagaStartEvent): Foo {
        put("START")
        return Foo("START")
    }

    @SagaJoinListener(successWith = SuccessWith.END)
    fun handleJoin(event: SagaJoinEvent): Any {
        put("JOIN")
        return Any::class
    }

    class Foo(name: String)

}
