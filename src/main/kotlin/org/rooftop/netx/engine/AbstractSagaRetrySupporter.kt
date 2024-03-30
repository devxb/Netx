package org.rooftop.netx.engine

import jakarta.annotation.PreDestroy
import org.rooftop.netx.engine.core.Saga
import org.rooftop.netx.engine.logging.info
import org.rooftop.netx.engine.logging.warningOnError
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

internal abstract class AbstractSagaRetrySupporter(
    private val backpressureSize: Int,
    private val recoveryMilli: Long,
    private val sagaDispatcher: AbstractSagaDispatcher,
) {

    private lateinit var executor: ScheduledExecutorService;
    private lateinit var scheduledFuture: ScheduledFuture<*>;

    fun watchOrphanSaga() {
        this.executor = Executors.newSingleThreadScheduledExecutor()

        this.scheduledFuture = executor.scheduleWithFixedDelay(
            handleOrphanSaga(),
            0,
            recoveryMilli,
            TimeUnit.MILLISECONDS,
        )
    }

    private fun handleOrphanSaga(): Runnable {
        return Runnable {
            claimOrphanSaga(backpressureSize)
                .doOnNext {
                    info("Retry orphan saga ${it.first}\nmessageId \"${it.second}\"")
                }
                .flatMap { (saga, messageId) ->
                    sagaDispatcher.dispatch(saga, messageId)
                        .warningOnError("Error occurred when retry orphan saga \"${saga.id}\"")
                }
                .onErrorResume { Mono.empty() }
                .subscribeOn(Schedulers.immediate())
                .subscribe()
        }
    }

    protected abstract fun claimOrphanSaga(backpressureSize: Int): Flux<Pair<Saga, String>>

    @PreDestroy
    private fun shutdownGracefully() {
        scheduledFuture.cancel(true)
        executor.shutdown()
        runCatching {
            if (!executor.awaitTermination(10, TimeUnit.MINUTES)) {
                executor.shutdownNow()
                if (!executor.awaitTermination(1, TimeUnit.MINUTES)) {
                    org.rooftop.netx.engine.logging.error("Cannot shutdown SagaRetrySupporter thread")
                }
            }
        }.onFailure {
            executor.shutdownNow()
            Thread.currentThread().interrupt()
        }.onSuccess {
            info("Shutdown SagaRetrySupporter gracefully")
        }
    }
}
