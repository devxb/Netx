package org.rooftop.netx.client

import org.rooftop.netx.api.Orchestrate
import org.rooftop.netx.api.Orchestrator
import org.rooftop.netx.engine.OrchestratorFactory
import org.springframework.context.annotation.Bean
import reactor.core.publisher.Mono

class OrchestratorConfigurer(
    private val orchestratorFactory: OrchestratorFactory,
) {

    @Bean
    fun sum3Orchestrator(): Orchestrator<Int, Int> {
        return orchestratorFactory.create<Int>("sum3Orchestrator")
            .start(IntOrchestrator)
            .join(IntOrchestrator)
            .commit(IntOrchestrator)
    }

    object IntOrchestrator : Orchestrate<Int, Int> {

        override fun orchestrate(request: Int): Int = request + 1
    }

    object MonoIntOrchestrator : Orchestrate<Int, Mono<Int>> {

        override fun orchestrate(request: Int): Mono<Int> = Mono.fromCallable { request + 1 }
    }
}
