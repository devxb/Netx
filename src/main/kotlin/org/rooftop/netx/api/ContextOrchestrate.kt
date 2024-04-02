package org.rooftop.netx.api

/**
 * An interface for accessing the Context maintained in each Orchestrator saga.
 *
 * @see Orchestrate
 * @see Context
 * @see Orchestrator
 */
fun interface ContextOrchestrate<T : Any, V : Any> : TypeReified<T> {

    /**
     * Passes the context with the request to orchestrate.
     *
     * @see Orchestrate.orchestrate
     */
    fun orchestrate(context: Context, request: T): V

    /**
     * @see Orchestrate.reified
     */
    override fun reified(): TypeReference<T>? = null
}
