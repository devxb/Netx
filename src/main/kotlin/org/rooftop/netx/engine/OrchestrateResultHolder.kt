package org.rooftop.netx.engine

import org.rooftop.netx.api.OrchestrateResult
import org.rooftop.netx.engine.core.TransactionState
import reactor.core.publisher.Mono
import kotlin.time.Duration

internal interface OrchestrateResultHolder {

    fun <T : Any> getResult(timeout: Duration, transactionId: String): Mono<OrchestrateResult<T>>

    fun <T : Any> setResult(transactionId: String, state: TransactionState, result: T): Mono<T>
}
