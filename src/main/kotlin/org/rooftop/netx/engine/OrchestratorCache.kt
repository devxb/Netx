package org.rooftop.netx.engine

import org.rooftop.netx.api.Orchestrator

@Suppress("UNCHECKED_CAST")
internal object OrchestratorCache {

    private val cache: MutableMap<String, Orchestrator<*, *>> = mutableMapOf()

    internal fun <T : Any, V : Any> get(key: String): Orchestrator<T, V> {
        return cache[key]?.let {
            it as Orchestrator<T, V>
        } ?: throw IllegalArgumentException("Cannot find orchestrator by orchestratorId \"$key\"")
    }

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
