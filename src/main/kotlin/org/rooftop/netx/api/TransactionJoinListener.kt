package org.rooftop.netx.api

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class TransactionJoinListener(
    val event: KClass<*> = Any::class
)
