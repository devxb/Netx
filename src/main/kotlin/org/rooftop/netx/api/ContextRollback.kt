package org.rooftop.netx.api

/**
 * An interface for accessing the Context maintained in each Orchestrator saga.
 *
 * @see Orchestrate
 * @see Context
 * @see Orchestrator
 */
fun interface ContextRollback<T : Any, V : Any?> : TypeReified<T> {

    /**
     * Passes the context with the request to orchestrate.
     *
     * @see Rollback.rollback
     */
    fun rollback(context: Context, request: T): V

    /**
     * @see Rollback.reified
     */
    override fun reified(): TypeReference<T>? = null
}
