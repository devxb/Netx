package org.rooftop.netx.engine

import org.rooftop.netx.api.Orchestrator

@Suppress("UNCHECKED_CAST")
internal class OrchestratorCache {

    private val cache: MutableMap<String, Orchestrator<*, *>> = mutableMapOf()

    internal fun <T : Any, V : Any> cache(
        key: String,
        behavior: () -> Orchestrator<T, V>,
    ): Orchestrator<T, V> {
        if (cache.contains(key)) {
            return get(key)
        }
        synchronized(key) {
            if (cache.contains(key)) {
                return get(key)
            }
            cache[key] = behavior.invoke()
            return cache[key] as Orchestrator<T, V>
        }
    }

    internal fun <T : Any, V : Any> get(key: String): Orchestrator<T, V> =
        cache[key]?.castTo(key)
            ?: throw IllegalArgumentException("Cannot find orchestrator by orchestratorId \"$key\"")

    private fun <T : Any, V : Any> Orchestrator<*, *>.castTo(key: String): Orchestrator<T, V> =
        runCatching {
            this as Orchestrator<T, V>
        }.getOrElse {
            throw TypeCastException("Cannot cast Orchestrator \"$key\"")
        }
}
