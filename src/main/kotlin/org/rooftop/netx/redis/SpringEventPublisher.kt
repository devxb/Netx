package org.rooftop.netx.redis

import org.rooftop.netx.engine.EventPublisher
import org.rooftop.netx.engine.SubscribeTransactionEvent
import org.springframework.context.ApplicationEventPublisher

class SpringEventPublisher(private val eventPublisher: ApplicationEventPublisher) : EventPublisher {

    override fun publish(event: SubscribeTransactionEvent) {
        eventPublisher.publishEvent(event)
    }
}
