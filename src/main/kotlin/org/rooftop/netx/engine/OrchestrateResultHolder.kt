package org.rooftop.netx.engine

import org.rooftop.netx.api.OrchestrateResult
import org.rooftop.netx.engine.core.TransactionState
import reactor.core.publisher.Mono

interface OrchestrateResultHolder {

    fun getResult(transactionId: String): Mono<OrchestrateResult>

    fun <T> setResult(transactionId: String, state: TransactionState, result: T): Mono<T>
}
