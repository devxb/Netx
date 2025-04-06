package org.rooftop.netx.spi

import org.rooftop.netx.api.SagaEvent

/**
 * By implementing this class, you can listen for dead letter occurrence events.
 *
 * @see DeadLetterRegistry.addListener
 */
fun interface DeadLetterListener {

    /**
     * handle dead letter add event
     *
     * @param deadLetterId | DeadLetter Id
     * @param sagaEvent | DeadLetter
     */
    fun listen(deadLetterId: String, sagaEvent: SagaEvent)
}
