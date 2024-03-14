package org.rooftop.netx.api

fun interface OrchestrateFunction<T> {

    fun invoke(orchestrateRequest: OrchestrateRequest): T
}
