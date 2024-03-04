package org.rooftop.netx.api

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class TransactionStartListener(
    val event: KClass<*> = Any::class,
    val noRetryFor: Array<KClass<out Throwable>> = [],
)
