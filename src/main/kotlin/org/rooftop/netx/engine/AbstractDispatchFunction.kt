package org.rooftop.netx.engine

import org.rooftop.netx.api.TransactionEvent
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

internal sealed class AbstractDispatchFunction<T>(
    protected val eventType: KClass<*>,
    protected val function: KFunction<T>,
    protected val handler: Any,
    private val noRetryFor: Array<KClass<out Throwable>>,
) {
    fun name(): String = function.name

    abstract fun call(transactionEvent: TransactionEvent): T

    protected fun isNoRetryFor(throwable: Throwable): Boolean {
        return noRetryFor.isNotEmpty() && throwable.cause != null && noRetryFor.contains(throwable.cause!!::class)
    }
}
