package org.rooftop.netx.engine.listen

import org.rooftop.netx.api.*
import org.rooftop.netx.core.Codec

internal class RollbackCommand<T : Any>(
    private val commandType: CommandType = CommandType.DEFAULT,
    private val codec: Codec,
    private val command: Any
) {

    @Suppress("UNCHECKED_CAST")
    fun command(
        request: T,
        contextData: String,
    ): Pair<Any?, Context> {
        val context = Context(
            codec,
            codec.decode(contextData, object : TypeReference<MutableMap<String, String>>() {})
        )
        return when (commandType) {
            CommandType.DEFAULT -> (command as Rollback<T, *>).rollback(request)

            CommandType.CONTEXT -> (command as ContextRollback<T, *>).rollback(
                context,
                request,
            )
        } to context
    }
}
