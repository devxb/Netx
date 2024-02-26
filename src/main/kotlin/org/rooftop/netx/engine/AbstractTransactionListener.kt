package org.rooftop.netx.engine

import org.rooftop.netx.idl.Transaction
import reactor.core.publisher.BufferOverflowStrategy
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

abstract class AbstractTransactionListener(
    private val backpressureSize: Int,
    private val transactionDispatcher: AbstractTransactionDispatcher,
) {

    fun subscribeStream() {
        receive()
            .publishOn(Schedulers.boundedElastic())
            .onBackpressureBuffer(backpressureSize, BufferOverflowStrategy.DROP_LATEST)
            .flatMap { (transaction, messageId) ->
                transactionDispatcher.dispatch(transaction, messageId)
                    .map { transaction to messageId }
                    .onErrorResume { Mono.empty() }
            }
            .restartWhenTerminated()
            .subscribe()
    }

    protected abstract fun receive(): Flux<Pair<Transaction, String>>

    private fun <T> Flux<T>.restartWhenTerminated(): Flux<T> {
        return this.doOnTerminate {
            subscribeStream()
        }
    }
}
