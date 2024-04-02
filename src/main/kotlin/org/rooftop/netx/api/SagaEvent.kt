package org.rooftop.netx.api

import org.rooftop.netx.core.Codec
import kotlin.reflect.KClass

/**
 * SagaEvent passed as a parameter to Saga...Listener.
 *
 * SagaEvent is Thread safe.
 *
 * @see SagaStartEvent
 * @see SagaJoinEvent
 * @see SagaCommitEvent
 * @see SagaRollbackEvent
 */
sealed class SagaEvent(
    /**
     * The saga ID.
     */
    val id: String,
    /**
     * The name of the node that published the saga event.
     */
    val nodeName: String,
    /**
     * The group of the node that published the saga event.
     */
    val group: String,
    internal val event: String?,
    internal val codec: Codec,
    internal var nextEvent: Any? = null,
) {

    /**
     * Sets the event to be published when the Saga...Listener is completed.
     *
     * If nothing is specified, the next Saga state is automatically published along with an empty event.
     *
     * @see SuccessWith
     * @param T next event
     * @return T
     */
    fun <T : Any> setNextEvent(event: T): T {
        this.nextEvent = event
        return event
    }

    /**
     * Decodes the event using the specified type reference.
     *
     * Use TypeReference when decoding a type that includes generics.
     *
     * <a href="http://gafter.blogspot.com/2006/12/super-type-tokens.html"/>
     *
     * Example.
     *
     *      sagaEvent.decodeEvent(object: TypeReference<List<Foo>>(){})
     *
     * @see SagaManager
     * @param typeReference
     * @param T
     */
    fun <T : Any> decodeEvent(typeReference: TypeReference<T>): T =
        codec.decode(
            event ?: throw NullPointerException("Cannot decode event cause event is null"),
            typeReference
        )

    /**
     * Decodes the event using the specified class.
     *
     * @see SagaManager
     * @param type
     * @param T
     */
    fun <T : Any> decodeEvent(type: Class<T>): T = decodeEvent(type.kotlin)

    /**
     * @see decodeEvent
     */
    fun <T : Any> decodeEvent(type: KClass<T>): T =
        codec.decode(
            event ?: throw NullPointerException("Cannot decode event cause event is null"),
            type
        )

    internal abstract fun copy(): SagaEvent
}
