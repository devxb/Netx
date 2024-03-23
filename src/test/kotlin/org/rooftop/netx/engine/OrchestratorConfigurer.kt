package org.rooftop.netx.engine

import org.rooftop.netx.api.Orchestrator
import org.rooftop.netx.engine.OrchestratorTest.Companion.rollbackOrchestratorResult
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Mono
import java.time.Instant

@Configuration
class OrchestratorConfigurer(
    private val orchestratorFactory: OrchestratorFactory,
) {

    @Bean(name = ["numberOrchestrator"])
    fun numberOrchestrator(): Orchestrator<Int, Int> {
        return orchestratorFactory.create<Int>("numberOrchestrator")
            .start(function = { it + 1 })
            .join(function = { it + 1 })
            .joinReactive(function = { Mono.just(it + 1) })
            .commit(function = { it + 1 })
    }

    @Bean(name = ["homeOrchestartor"])
    fun homeOrchestrator(): Orchestrator<OrchestratorTest.Home, OrchestratorTest.Home> {
        return orchestratorFactory.create<OrchestratorTest.Home>("homeOrchestrator")
            .startReactive({ home ->
                Mono.fromCallable {
                    home.addPerson(OrchestratorTest.Person("Mother"))
                    home
                }
            })
            .join({
                it.addPerson(OrchestratorTest.Person("Father"))
                it
            })
            .commitReactive({ home ->
                Mono.fromCallable {
                    home.addPerson(OrchestratorTest.Person("Son"))
                    home
                }
            })
    }

    @Bean(name = ["instantOrchestrator"])
    fun instantOrchestrator(): Orchestrator<OrchestratorTest.InstantWrapper, OrchestratorTest.InstantWrapper> {
        return orchestratorFactory.create<OrchestratorTest.InstantWrapper>("instantOrchestrator")
            .start({ it })
            .commit({ it })
    }

    @Bean(name = ["manyTypeOrchestrator"])
    fun manyTypeOrchestrator(): Orchestrator<Int, OrchestratorTest.Home> {
        return orchestratorFactory.create<Int>("manyTypeOrchestrator")
            .start({ "String" })
            .join({ 1L })
            .join({ 0.1 })
            .join({ OrchestratorTest.InstantWrapper(Instant.now()) })
            .commit({ OrchestratorTest.Home("HOME", mutableListOf()) })
    }

    @Bean(name = ["rollbackOrchestrator"])
    fun rollbackOrchestrator(): Orchestrator<String, String> {
        return orchestratorFactory.create<String>("rollbackOrchestrator")
            .start(
                function = {
                    rollbackOrchestratorResult.add("1")
                },
                rollback = {
                    rollbackOrchestratorResult.add("-1")
                }
            )
            .join(
                function = {
                    rollbackOrchestratorResult.add("2")
                }
            )
            .join(
                function = {
                    rollbackOrchestratorResult.add("3")
                },
                rollback = { rollbackOrchestratorResult.add("-3") }
            )
            .commit(
                function = {
                    rollbackOrchestratorResult.add("4")
                    throw IllegalArgumentException("Rollback")
                },
                rollback = {
                    rollbackOrchestratorResult.add("-4")
                }
            )
    }
}
