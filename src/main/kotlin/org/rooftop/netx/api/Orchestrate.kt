package org.rooftop.netx.api

/**
 * Unit of operation for Orchestrator.
 *
 * Orchestrate is guaranteed to be executed at least once under any circumstances.
 *
 * @see Orchestrator
 */
fun interface Orchestrate<T : Any, V : Any> : TypeReified<T> {

    /**
     * Takes a request and responds with the result of orchestration.
     *
     * The result of orchestration is passed to the subsequent orchestrate.
     *
     * @param T request type of Orchestrate
     * @param V response type of Orchestrate
     */
    fun orchestrate(request: T): V

    /**
     * If the request parameter includes generics such as List<Foo>, this function must be implemented.
     *
     * Generics prevent type inference, so this function relies on the return value of the reified function to decode and pass the request.
     */
    override fun reified(): TypeReference<T>? = null
}
