package org.rooftop.netx.api

import kotlin.reflect.KClass

/**
 * Listener that can receive SagaJoinEvent. Methods annotated with this annotation must specify only one SagaJoinEvent as a parameter,
 * and it is guaranteed that they will be executed at least once. even if any problem occurs (such as server shutdown).
 *
 * If an exception occurs within the method, a RollbackEvent is published, and the event set by sagaEvent.setNextEvent is published along with it.
 *
 * If the method ends successfully, the event specified by the successWith field is published. Likewise, the event set by sagaEvent.setNextEvent is published along with it.
 *
 * Example.
 *
 *      @SagaJoinListener // Receives SagaJoinEvent of all types.
 *      fun listenSagaJoinEvent(sagaJoinEvent: SagaJoinEvent): Foo {
 *          // ...
 *      }
 *
 *      @SagaJoinListener(event = Foo::class) // Receives SagaJoinEvent that can be decoded into Foo.
 *      ...
 *
 *      @SagaJoinListener(noRollbackFor = [IllegalStateException::class]) // If an IllegalStateException occurs during method processing, do not rollback and publish the next event specified by successWith.
 *      ...
 *
 *      @SagaJoinListener(successWith = SuccessWith.PUBLISH_COMMIT) // You can specify the next event to publish when the method succeeds.
 *      ...
 *
 * @see SagaManager.join
 * @see SuccessWith
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class SagaJoinListener(
    val event: KClass<*> = Any::class,
    val noRollbackFor: Array<KClass<out Throwable>> = [],
    val successWith: SuccessWith = SuccessWith.PUBLISH_JOIN,
)
