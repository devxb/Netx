package org.rooftop.netx.engine

fun interface EventPublisher {

    fun publish(event: TransactionJoinedEvent)
}
