package org.rooftop.netx.api

import kotlin.reflect.KClass

/**
 * Listener that can receive SagaRollbackEvent. Methods annotated with this annotation must specify only one SagaRollbackEvent as a parameter,
 * and it is guaranteed that they will be executed at least once. even if any problem occurs (such as server shutdown).
 *
 * Example.
 *
 *      @SagaRollbackListener // Receives SagaRollbackEvent of all types.
 *      fun listenSagaRollbackEvent(sagaRollbackEvent: SagaRollbackEvent): Foo {
 *          // ...
 *      }
 *
 *      @SagaRollbackListener(event = Foo::class) // Receives SagaRollbackEvent that can be decoded into Foo.
 *      ...
 *
 * @see SagaManager.rollback
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class SagaRollbackListener(
    val event: KClass<*> = Any::class,
)
