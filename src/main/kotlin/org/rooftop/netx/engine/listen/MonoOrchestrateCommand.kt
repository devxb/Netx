package org.rooftop.netx.engine.listen

import org.rooftop.netx.api.*
import reactor.core.publisher.Mono

class MonoOrchestrateCommand<T : Any, V : Any>(
    private val commandType: CommandType = CommandType.DEFAULT,
    private val codec: Codec,
    private val command: Any
) {

    @Suppress("UNCHECKED_CAST")
    fun command(
        request: T,
        contextData: String,
    ): Mono<Pair<V, Context>> {
        val context = Context(
            codec,
            codec.decode(contextData, object : TypeReference<MutableMap<String, String>>() {})
        )
        return when (commandType) {
            CommandType.DEFAULT -> (command as Orchestrate<T, Mono<V>>).orchestrate(request)

            CommandType.CONTEXT -> (command as ContextOrchestrate<T, Mono<V>>).orchestrate(
                context,
                request,
            )
        }.map {
            it to context
        }
    }
}
