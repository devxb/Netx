package org.rooftop.netx.engine

import org.rooftop.netx.api.Orchestrator

internal object OrchestratorCache {

    private val cache: MutableMap<String, Orchestrator<*, *>> = mutableMapOf()

    @Suppress("UNCHECKED_CAST")
    internal fun <T : Any, V : Any> cache(
        key: String,
        behavior: () -> Orchestrator<*, *>,
    ): Orchestrator<T, V> {
        if (cache.contains(key)) {
            return cache[key] as Orchestrator<T, V>
        }
        synchronized(key) {
            if (cache.contains(key)) {
                return cache[key] as Orchestrator<T, V>
            }
            cache[key] = behavior.invoke()
            return cache[key] as Orchestrator<T, V>
        }
    }
}
