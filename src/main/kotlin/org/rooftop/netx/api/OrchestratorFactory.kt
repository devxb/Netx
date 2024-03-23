package org.rooftop.netx.api

interface OrchestratorFactory {

    fun <T : Any, V : Any> get(orchestratorId: String): Orchestrator<T, V>

    fun <T : Any> create(orchestratorId: String): OrchestrateChain.Pre<T>

}
