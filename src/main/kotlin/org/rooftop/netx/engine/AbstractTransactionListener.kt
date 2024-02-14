package org.rooftop.netx.engine

import org.rooftop.netx.idl.Transaction
import reactor.core.publisher.Flux

abstract class AbstractTransactionListener(
    private val transactionDispatcher: AbstractTransactionDispatcher,
) {

    fun subscribeStream(transactionId: String): Flux<Pair<Transaction, String>> {
        return receive(transactionId)
            .flatMap { (transaction, messageId) ->
                transactionDispatcher.dispatch(transaction, messageId)
                    .map { transaction to messageId }
            }
    }

    protected abstract fun receive(transactionId: String): Flux<Pair<Transaction, String>>
}
