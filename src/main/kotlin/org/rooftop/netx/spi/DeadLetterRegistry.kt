package org.rooftop.netx.spi

/**
 * An interface for registry dead letter listener.
 *
 * @see DeadLetterListener
 */
fun interface DeadLetterRegistry {

    /**
     * Adding a hook to be executed when a dead letter occurs.
     *
     * @see DeadLetterListener
     */
    fun addListener(deadLetterListener: DeadLetterListener)
}
