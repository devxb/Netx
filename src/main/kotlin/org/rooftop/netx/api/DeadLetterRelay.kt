package org.rooftop.netx.api

import reactor.core.publisher.Mono

/**
 * An interface for relay dead letters, with support for recovering those that failed during rollback.
 */
interface DeadLetterRelay {

    /**
     * relay dead letter and return SagaEvent
     *
     * @return SagaEvent | A dead letter that was successfully processed
     */
    fun relay(): Mono<SagaEvent>

    /**
     * @see relay
     * @return SagaEvent | A dead letter that was successfully processed
     */
    fun relaySync(): SagaEvent


    /**
     * relay dead letter and return SagaEvent by specific deadLetterId (not a same SagaEvent.id)
     *
     * @param deadLetterId
     * @return SagaEvent | A dead letter that was successfully processed
     */
    fun relay(deadLetterId: String): Mono<SagaEvent>

    /**
     * @see relay
     * @return SagaEvent | A dead letter that was successfully processed (not a same SagaEvent.id)
     */
    fun relaySync(deadLetterId: String): SagaEvent
}
