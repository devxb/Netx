package org.rooftop.netx.api

fun interface OrchestrateFunction<T> {

    fun orchestrate(orchestrateRequest: OrchestrateRequest): T
}
