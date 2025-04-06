package org.rooftop.netx.engine.deadletter

import org.rooftop.netx.api.DeadLetterRelay
import org.rooftop.netx.api.SagaEvent
import org.rooftop.netx.engine.AbstractSagaDispatcher
import org.rooftop.netx.engine.logging.error
import org.rooftop.netx.spi.DeadLetterListener
import org.rooftop.netx.spi.DeadLetterRegistry
import reactor.core.publisher.Mono

internal abstract class AbstractDeadLetterManager : DeadLetterRelay, DeadLetterRegistry {

    private val deadLetterListeners: MutableList<DeadLetterListener> = mutableListOf()

    internal lateinit var dispatcher: AbstractSagaDispatcher

    override fun addListener(deadLetterListener: DeadLetterListener) {
        deadLetterListeners.add(deadLetterListener)
    }

    fun addDeadLetter(sagaEvent: SagaEvent): Mono<String> {
        return add(sagaEvent)
            .doOnNext {
                deadLetterListeners.forEach { deadLetterListener ->
                    runCatching {
                        deadLetterListener.listen(it, sagaEvent)
                    }.onFailure {
                        error("Fail to call deadLetterListener.listen", it)
                    }
                }
            }
    }

    abstract fun add(sagaEvent: SagaEvent): Mono<String>
}
