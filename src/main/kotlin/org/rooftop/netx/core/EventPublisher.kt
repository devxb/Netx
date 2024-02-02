package org.rooftop.netx.core

fun interface EventPublisher {

    fun publish(event: TransactionJoinedEvent)
}
