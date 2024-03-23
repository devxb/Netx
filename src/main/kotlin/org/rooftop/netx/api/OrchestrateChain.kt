package org.rooftop.netx.api

import org.rooftop.netx.engine.DefaultOrchestrateChain
import reactor.core.publisher.Mono

interface OrchestrateChain<OriginReq : Any, T : Any, V : Any> {

    fun <S : Any> join(
        orchestrate: Orchestrate<V, S>,
        rollback: Rollback<V, *>? = null,
    ): DefaultOrchestrateChain<OriginReq, V, S>

    fun <S : Any> joinReactive(
        orchestrate: Orchestrate<V, Mono<S>>,
        rollback: Rollback<V, Mono<*>>? = null,
    ): DefaultOrchestrateChain<OriginReq, V, S>

    fun <S : Any> joinWithContext(
        contextOrchestrate: ContextOrchestrate<V, S>,
        contextRollback: ContextRollback<V, *>? = null,
    ): DefaultOrchestrateChain<OriginReq, V, S>

    fun <S : Any> joinReactiveWithContext(
        contextOrchestrate: ContextOrchestrate<V, Mono<S>>,
        contextRollback: ContextRollback<V, Mono<*>>? = null,
    ): DefaultOrchestrateChain<OriginReq, V, S>

    fun <S : Any> commit(
        orchestrate: Orchestrate<V, S>,
        rollback: Rollback<V, *>? = null,
    ): Orchestrator<OriginReq, S>

    fun <S : Any> commitReactive(
        orchestrate: Orchestrate<V, Mono<S>>,
        rollback: Rollback<V, Mono<*>>? = null,
    ): Orchestrator<OriginReq, S>

    fun <S : Any> commitWithContext(
        contextOrchestrate: ContextOrchestrate<V, S>,
        contextRollback: ContextRollback<V, *>? = null,
    ): Orchestrator<OriginReq, S>

    fun <S : Any> commitReactiveWithContext(
        contextOrchestrate: ContextOrchestrate<V, Mono<S>>,
        contextRollback: ContextRollback<V, Mono<*>>? = null,
    ): Orchestrator<OriginReq, S>

    interface Pre<T : Any> {
        fun <V : Any> start(
            orchestrate: Orchestrate<T, V>,
            rollback: Rollback<T, *>? = null,
        ): DefaultOrchestrateChain<T, T, V>

        fun <V : Any> startReactive(
            orchestrate: Orchestrate<T, Mono<V>>,
            rollback: Rollback<T, Mono<*>>? = null,
        ): DefaultOrchestrateChain<T, T, V>

        fun <V : Any> startWithContext(
            contextOrchestrate: ContextOrchestrate<T, V>,
            contextRollback: ContextRollback<T, *>? = null,
        ): DefaultOrchestrateChain<T, T, V>

        fun <V : Any> startReactiveWithContext(
            contextOrchestrate: ContextOrchestrate<T, Mono<V>>,
            contextRollback: ContextRollback<T, Mono<*>>? = null,
        ): DefaultOrchestrateChain<T, T, V>
    }
}
