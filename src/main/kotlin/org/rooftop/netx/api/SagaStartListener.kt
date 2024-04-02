package org.rooftop.netx.api

import kotlin.reflect.KClass

/**
 * Listener that can receive SagaStartEvent. Methods annotated with this annotation must specify only one SagaStartEvent as a parameter,
 * and it is guaranteed that they will be executed at least once. even if any problem occurs (such as server shutdown).
 *
 * If an exception occurs within the method, a RollbackEvent is published, and at this time, the event set by sagaEvent.setNextEvent is also published.
 *
 * If the method ends successfully, the event set by the successWith field is published. Similarly, the event set by sagaEvent.setNextEvent is also published.
 *
 * Example.
 *
 *      @SagaStartListener // Receives SagaStartEvent of all types.
 *      fun listenSagaStartEvent(sagaStartEvent: SagaStartEvent): Foo {
 *          // ...
 *      }
 *
 *      @SagaStartListener(event = Foo::class) // Receives SagaStartEvent that can be decoded into Foo.
 *      ...
 *
 *      @SagaStartListener(noRollbackFor = IllegalStateException::class) // If IllegalStateException occurs during method processing, it does not rollback and publishes the next event set by successWith.
 *      ...
 *
 *      @SagaStartListener(successWith = SuccessWith.PUBLISH_COMMIT) // You can specify the next event to be published if the method succeeds.
 *      ...
 *
 * @see SagaManager.start
 * @see SuccessWith
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class SagaStartListener(
    val event: KClass<*> = Any::class,
    val noRollbackFor: Array<KClass<out Throwable>> = [],
    val successWith: SuccessWith = SuccessWith.PUBLISH_JOIN,
)
