package org.rooftop.netx.engine

import org.rooftop.netx.engine.logging.info
import org.rooftop.netx.engine.logging.warningOnError
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
            .doOnNext {
                info("Listen transaction \n{\n${it.first}}\nmessageId \"${it.second}\"")
            }
            .map { (transaction, messageId) ->
                val isSuccess = transactionDispatcher.dispatch(transaction, messageId)
                if (!isSuccess) {
                    warningOnError("Error occurred when listen transaction \n{\n$transaction}\nmessageId \"$messageId\"")
                }
                isSuccess
            }
            .onErrorResume { Mono.empty() }
            .restartWhenTerminated()
            .subscribe()
    }

    protected abstract fun receive(): Flux<Pair<Transaction, String>>

    private fun <T> Flux<T>.restartWhenTerminated(): Flux<T> {
        return this.doAfterTerminate {
            subscribeStream()
        }
    }
}
