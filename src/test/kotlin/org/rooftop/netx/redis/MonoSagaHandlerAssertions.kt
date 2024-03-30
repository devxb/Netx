package org.rooftop.netx.redis

import org.rooftop.netx.api.*
import org.rooftop.netx.meta.SagaHandler
import reactor.core.publisher.Mono

@SagaHandler
class MonoSagaHandlerAssertions : AbstractSagaHandlerAssertions() {

    @SagaRollbackListener
    fun handleRollback(event: SagaRollbackEvent): Mono<Unit> {
        put("ROLLBACK")
        return Mono.just(Unit)
    }

    @SagaCommitListener
    fun handleCommit(event: SagaCommitEvent): Mono<Unit> {
        put("COMMIT")
        return Mono.just(Unit)
    }

    @SagaStartListener(successWith = SuccessWith.END)
    fun handleStart(event: SagaStartEvent): Mono<Unit> {
        put("START")
        return Mono.just(Unit)
    }

    @SagaJoinListener(successWith = SuccessWith.END)
    fun handleJoin(event: SagaJoinEvent): Mono<Unit> {
        put("JOIN")
        return Mono.just(Unit)
    }

}
