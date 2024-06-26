package org.rooftop.netx.client

import org.rooftop.netx.api.Orchestrate
import org.rooftop.netx.api.Orchestrator
import org.rooftop.netx.api.OrchestratorFactory
import org.springframework.context.annotation.Bean
import reactor.core.publisher.Mono

internal class OrchestratorConfigurer {

    @Bean
    fun sum3Orchestrator(): Orchestrator<Int, Int> {
        return OrchestratorFactory.instance().create<Int>("sum3Orchestrator")
            .startReactive(MonoIntOrchestrator, rollback = { Mono.fromCallable { it - 1 } })
            .joinReactive(MonoIntOrchestrator, rollback = { Mono.fromCallable { it - 1 } })
            .commitReactiveWithContext({ _, request ->
                Mono.fromCallable {
                    request + 1
                }
            })
    }

    object IntOrchestrator : Orchestrate<Int, Int> {

        override fun orchestrate(request: Int): Int = request + 1
    }

    object MonoIntOrchestrator : Orchestrate<Int, Mono<Int>> {

        override fun orchestrate(request: Int): Mono<Int> = Mono.fromCallable { request + 1 }
    }
}
