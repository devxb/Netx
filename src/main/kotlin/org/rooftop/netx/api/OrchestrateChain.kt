package org.rooftop.netx.api

import reactor.core.publisher.Mono

/**
 * Used to create an Orchestrator.
 *
 * Each operation in the OrchestrateChain is not executed immediately but deferred until the Orchestrator saga is executed.
 *
 * For detailed usage, refer to the Example of Orchestrator.
 *
 * @see Orchestrator
 * @param OriginReq The first request of Orchestrator
 * @param T The request type of each Chain
 * @param V The response type of each Chain
 */
interface OrchestrateChain<OriginReq : Any, T : Any, V : Any> {

    /**
     * Joins the saga with the operation.
     *
     * @param orchestrate Operation to be executed along with the join
     * @param rollback Rollback function to be executed if an exception is thrown in the current orchestrate or sub-orchestrate.
     * @param S Response passed as the request to the next orchestrate and rollback.
     * @return OrchestrateChain
     * @see Orchestrate
     * @see Rollback
     */
    fun <S : Any> join(
        orchestrate: Orchestrate<V, S>,
        rollback: Rollback<V, *>? = null,
    ): OrchestrateChain<OriginReq, V, S>

    /**
     * @see join
     */
    fun <S : Any> joinReactive(
        orchestrate: Orchestrate<V, Mono<S>>,
        rollback: Rollback<V, Mono<*>>? = null,
    ): OrchestrateChain<OriginReq, V, S>

    /**
     * @param contextOrchestrate Allows using Context maintained in each Saga.
     * @param contextRollback Allows using Context maintained in each Saga.
     * @see join
     * @see ContextOrchestrate
     * @see ContextRollback
     */
    fun <S : Any> joinWithContext(
        contextOrchestrate: ContextOrchestrate<V, S>,
        contextRollback: ContextRollback<V, *>? = null,
    ): OrchestrateChain<OriginReq, V, S>

    /**
     * @see joinReactiveWithContext
     */
    fun <S : Any> joinReactiveWithContext(
        contextOrchestrate: ContextOrchestrate<V, Mono<S>>,
        contextRollback: ContextRollback<V, Mono<*>>? = null,
    ): OrchestrateChain<OriginReq, V, S>

    /**
     * Commits the saga with the operation.
     *
     * @param orchestrate Operation to be executed along with the commit.
     * @param rollback Rollback function to be executed if an exception is thrown in the current orchestrate.
     * @param S The final return value of Orchestrator.
     * @return Orchestrator
     * @see Orchestrate
     * @see Rollback*
     */
    fun <S : Any> commit(
        orchestrate: Orchestrate<V, S>,
        rollback: Rollback<V, *>? = null,
    ): Orchestrator<OriginReq, S>

    /**
     * @see commit
     */
    fun <S : Any> commitReactive(
        orchestrate: Orchestrate<V, Mono<S>>,
        rollback: Rollback<V, Mono<*>>? = null,
    ): Orchestrator<OriginReq, S>

    /**
     * @param contextOrchestrate Allows using Context maintained in each Saga.
     * @param contextRollback Allows using Context maintained in each Saga.
     * @see commit
     * @see contextOrchestrate
     * @see contextRollback
     */
    fun <S : Any> commitWithContext(
        contextOrchestrate: ContextOrchestrate<V, S>,
        contextRollback: ContextRollback<V, *>? = null,
    ): Orchestrator<OriginReq, S>

    /**
     * @see commitWithContext
     */
    fun <S : Any> commitReactiveWithContext(
        contextOrchestrate: ContextOrchestrate<V, Mono<S>>,
        contextRollback: ContextRollback<V, Mono<*>>? = null,
    ): Orchestrator<OriginReq, S>

    interface Pre<T : Any> {

        /**
         * Starts the saga with the operation.
         *
         * @param orchestrate Operation to be executed along with the start.
         * @param rollback Rollback function to be executed if an exception is thrown in the current orchestrate or sub-orchestrate.
         * @param S Response passed as the request to the next orchestrate and rollback.
         * @return OrchestrateChain
         * @see Orchestrate
         * @see Rollback
         */
        fun <V : Any> start(
            orchestrate: Orchestrate<T, V>,
            rollback: Rollback<T, *>? = null,
        ): OrchestrateChain<T, T, V>

        /**
         * @see start
         */
        fun <V : Any> startReactive(
            orchestrate: Orchestrate<T, Mono<V>>,
            rollback: Rollback<T, Mono<*>>? = null,
        ): OrchestrateChain<T, T, V>

        /**
         * @param contextOrchestrate Allows using Context maintained in each Saga.
         * @param contextRollback Allows using Context maintained in each Saga.
         * @see start
         * @see ContextOrchestrate
         * @see ContextRollback
         */
        fun <V : Any> startWithContext(
            contextOrchestrate: ContextOrchestrate<T, V>,
            contextRollback: ContextRollback<T, *>? = null,
        ): OrchestrateChain<T, T, V>

        /**
         * @see startWithContext
         */
        fun <V : Any> startReactiveWithContext(
            contextOrchestrate: ContextOrchestrate<T, Mono<V>>,
            contextRollback: ContextRollback<T, Mono<*>>? = null,
        ): OrchestrateChain<T, T, V>
    }
}
