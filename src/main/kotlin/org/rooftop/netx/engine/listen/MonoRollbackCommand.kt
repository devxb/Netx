package org.rooftop.netx.engine.listen

import org.rooftop.netx.api.*
import reactor.core.publisher.Mono

class MonoRollbackCommand<T : Any>(
    private val commandType: CommandType = CommandType.DEFAULT,
    private val codec: Codec,
    private val command: Any
) {

    @Suppress("UNCHECKED_CAST")
    fun command(
        request: T,
        contextData: String,
    ): Mono<Pair<Any?, Context>> {
        val context = Context(
            codec,
            codec.decode(contextData, object : TypeReference<MutableMap<String, String>>() {})
        )
        return when (commandType) {
            CommandType.DEFAULT -> (command as Rollback<T, Mono<Any?>>).rollback(request)

            CommandType.CONTEXT -> (command as ContextRollback<T, Mono<Any?>>).rollback(
                context,
                request,
            )
        }.map { it to context }
            .switchIfEmpty(Mono.just("ROLLBACK SUCCESS" to context))
    }
}
