package org.rooftop.netx.engine

import org.rooftop.netx.idl.Transaction
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers

abstract class AbstractTransactionListener(
    private val transactionDispatcher: AbstractTransactionDispatcher,
) {

    fun subscribeStream() {
        receive()
            .flatMap { (transaction, messageId) ->
                transactionDispatcher.dispatch(transaction, messageId)
                    .map { transaction to messageId }
            }
            .subscribeOn(Schedulers.parallel())
            .subscribe()
    }

    protected abstract fun receive(): Flux<Pair<Transaction, String>>
}
