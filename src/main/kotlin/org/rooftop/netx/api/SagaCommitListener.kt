package org.rooftop.netx.api

import kotlin.reflect.KClass

/**
 * Listener that can receive SagaCommitEvent. Methods annotated with this annotation must specify only one SagaCommitEvent as a parameter,
 * and it is guaranteed that they will be executed at least once. even if any problem occurs (such as server shutdown).
 *
 * If an exception occurs within the method, a RollbackEvent is published.
 *
 * Example.
 *
 *      @SagaCommitListener // Receives SagaCommitEvent.
 *      fun listenSagaCommitEvent(sagaCommitEvent: SagaCommitEvent): Foo {
 *          // ...
 *      }
 *
 *      @SagaCommitListener(event = Foo::class) // Receives SagaCommitEvent that can be decoded into Foo.
 *      ...
 *
 *      @SagaCommitListener(noRollbackFor = [IllegalStateException::class]) // If an IllegalStateException occurs during method processing, do not rollback.
 *      ...
 *
 * @see SagaManager.commit
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class SagaCommitListener(
    val event: KClass<*> = Any::class,
    val noRollbackFor: Array<KClass<out Throwable>> = [],
)
