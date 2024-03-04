package org.rooftop.netx.engine

import org.rooftop.netx.api.TransactionEvent
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

internal sealed class AbstractDispatchFunction<T>(
    protected val eventType: KClass<*>,
    protected val function: KFunction<T>,
    protected val handler: Any,
    protected val noRetryFor: Array<KClass<out Throwable>>,
) {
    fun name(): String = function.name

    abstract fun call(transactionEvent: TransactionEvent): T
}
