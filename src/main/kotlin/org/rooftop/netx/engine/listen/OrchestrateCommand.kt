package org.rooftop.netx.engine.listen

import org.rooftop.netx.api.*

internal class OrchestrateCommand<T : Any, V : Any>(
    private val commandType: CommandType = CommandType.DEFAULT,
    private val codec: Codec,
    private val command: Any
) {

    @Suppress("UNCHECKED_CAST")
    fun command(
        request: T,
        contextData: String,
    ): Pair<V, Context> {
        val context = Context(
            codec,
            codec.decode(contextData, object : TypeReference<MutableMap<String, String>>() {})
        )
        return when (commandType) {
            CommandType.DEFAULT -> (command as Orchestrate<T, V>).orchestrate(request)

            CommandType.CONTEXT -> (command as ContextOrchestrate<T, V>).orchestrate(
                context,
                request,
            )
        } to context
    }
}
