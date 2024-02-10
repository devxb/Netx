package org.rooftop.netx.engine

import org.rooftop.netx.idl.Transaction
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

abstract class AbstractTransactionRetrySupporter(
    recoveryMilli: Long,
) {

    init {
        Flux.interval(recoveryMilli.milliseconds.toJavaDuration())
            .publishOn(Schedulers.parallel())
            .flatMap { handleOrphanTransaction() }
            .subscribe()
    }

    abstract fun watchTransaction(transactionId: String): Mono<String>

    protected abstract fun handleOrphanTransaction(): Flux<Pair<Transaction, String>>
}
