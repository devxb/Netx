package org.rooftop.netx.engine

import jakarta.annotation.PreDestroy
import org.rooftop.netx.engine.core.Saga
import org.rooftop.netx.engine.logging.info
import org.rooftop.netx.engine.logging.warningOnError
import reactor.core.Disposable
import reactor.core.publisher.BufferOverflowStrategy
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

internal abstract class AbstractSagaListener(
    private val backpressureSize: Int,
    private val sagaDispatcher: AbstractSagaDispatcher,
) {

    private lateinit var disposable: Disposable
    private var isShutdown = false

    fun subscribeStream() {
        disposable = receive()
            .publishOn(Schedulers.boundedElastic())
            .onBackpressureBuffer(backpressureSize, BufferOverflowStrategy.DROP_LATEST)
            .doOnNext {
                info("Listen saga ${it.first}\nmessageId \"${it.second}\"")
            }
            .flatMap { (saga, messageId) ->
                sagaDispatcher.dispatch(saga, messageId)
                    .warningOnError("Error occurred when listen saga id ${saga.id}")
            }
            .onErrorResume { Mono.empty() }
            .restartWhenTerminated()
            .subscribe()
    }

    protected abstract fun receive(): Flux<Pair<Saga, String>>

    private fun <T> Flux<T>.restartWhenTerminated(): Flux<T> {
        return this.doAfterTerminate {
            if (isShutdown.not()) {
                subscribeStream()
            }
        }
    }

    @PreDestroy
    private fun shutdownGracefully() {
        disposable.dispose()
        shutdownCascade()
        info("Shutdown SagaListenerSupporter gracefully")
    }

    protected abstract fun shutdownCascade()
}
