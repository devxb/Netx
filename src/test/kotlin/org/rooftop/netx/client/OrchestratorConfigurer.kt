package org.rooftop.netx.client

import org.rooftop.netx.api.OrchestrateFunction
import org.rooftop.netx.api.OrchestrateRequest
import org.rooftop.netx.api.Orchestrator
import org.rooftop.netx.meta.AbstractOrchestratorConfigurer
import org.springframework.context.annotation.Bean
import reactor.core.publisher.Mono

class OrchestratorConfigurer : AbstractOrchestratorConfigurer() {

    @Bean
    fun sum3Orchestrator(): Orchestrator<Int> {
        return newOrchestrator()
            .startSync(IntOrchestrator)
            .joinSync(IntOrchestrator)
            .commitSync(IntOrchestrator)
            .build()
    }

    object IntOrchestrator : OrchestrateFunction<Int> {

        override fun orchestrate(orchestrateRequest: OrchestrateRequest): Int {
            return orchestrateRequest.decodeEvent(Int::class) + 1
        }
    }

    object MonoIntOrchestrator : OrchestrateFunction<Mono<Int>> {
        override fun orchestrate(orchestrateRequest: OrchestrateRequest): Mono<Int> {
            return Mono.fromCallable { orchestrateRequest.decodeEvent(Int::class) }
                .map { it + 1 }
        }
    }
}
