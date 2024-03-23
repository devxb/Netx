package org.rooftop.netx.api

import org.rooftop.netx.engine.DefaultOrchestrateChain
import reactor.core.publisher.Mono

interface OrchestrateChain<OriginReq : Any, T : Any, V : Any> {

    fun <S : Any> join(
        function: OrchestrateFunction<V, S>,
        rollback: RollbackFunction<V, *>? = null,
    ): DefaultOrchestrateChain<OriginReq, V, S>

    fun <S : Any> joinReactive(
        function: OrchestrateFunction<V, Mono<S>>,
        rollback: RollbackFunction<V, Mono<*>>? = null,
    ): DefaultOrchestrateChain<OriginReq, V, S>

    fun <S : Any> commit(
        function: OrchestrateFunction<V, S>,
        rollback: RollbackFunction<V, *>? = null,
    ): Orchestrator<OriginReq, S>

    fun <S : Any> commitReactive(
        function: OrchestrateFunction<V, Mono<S>>,
        rollback: RollbackFunction<V, Mono<*>>? = null,
    ): Orchestrator<OriginReq, S>

    interface Pre<T : Any> {
        fun <V : Any> start(
            function: OrchestrateFunction<T, V>,
            rollback: RollbackFunction<T, *>? = null,
        ): DefaultOrchestrateChain<T, T, V>

        fun <V : Any> startReactive(
            function: OrchestrateFunction<T, Mono<V>>,
            rollback: RollbackFunction<T, Mono<*>>? = null,
        ): DefaultOrchestrateChain<T, T, V>

    }
}
