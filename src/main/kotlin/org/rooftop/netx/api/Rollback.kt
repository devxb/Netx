package org.rooftop.netx.api

/**
 * Rollback operation unit of Orchestrator.
 *
 * Rollback is guaranteed to be executed at least once under any circumstances.
 */
fun interface Rollback<T : Any, V : Any?>: TypeReified<T> {

    /**
     * Receives the request and performs rollback.
     *
     * The return value of rollback is ignored.
     */
    fun rollback(request: T): V

    /**
     * If the request parameter includes generics such as List<Foo>, this function must be implemented.
     *
     * Generics prevent type inference, so this function relies on the return value of the reified function to decode and pass the request.
     */
    override fun reified(): TypeReference<T>? = null
}
