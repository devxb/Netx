package org.rooftop.netx.client

import org.springframework.boot.test.context.TestComponent
import java.util.concurrent.Callable
import java.util.concurrent.Executors

@TestComponent
class LoadRunner {

    private val executor = Executors.newFixedThreadPool(32)

    fun load(count: Int, behavior: Callable<Any>) {
        val behaviors = mutableListOf<Callable<Any>>()
        for (i in 1..count) {
            behaviors.add(behavior)
        }
        executor.invokeAll(behaviors)
    }
}
