package org.rooftop.netx.client

import org.springframework.boot.test.context.TestComponent
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers

@TestComponent
class LoadRunner {

    fun load(count: Int, behavior: Runnable) {
        val iter = mutableListOf<Runnable>()
        for (i in 1..count) {
            iter.add(behavior)
        }
        Flux.fromIterable(iter)
            .publishOn(Schedulers.boundedElastic())
            .map { it.run() }
            .subscribe()
    }
}
