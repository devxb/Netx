package org.rooftop.netx.engine

import org.rooftop.netx.api.Orchestrator
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

class ErrorInjector {
    var doError: Boolean = false
}

class RelayResultHolder {

    private val holder: MutableMap<String, MutableList<RelayEvent>> = mutableMapOf()

    operator fun get(key: String) = holder[key]

    fun hold(key: String, value: RelayEvent) {
        holder.putIfAbsent(key, mutableListOf())
        holder[key]?.add(value)
    }

    fun clear() {
        holder.clear()
    }
}

data class RelayEvent(
    val id: Long,
    val name: String,
    val list: List<String>,
    val depth: Depth,
) {

    data class Depth(
        val success: Boolean,
    )
}

@TestConfiguration
internal class DeadLetterConfigurer(
    private val orchestratorFactory: OrchestratorFactory,
    private val deadLetterAnnotationClass: DeadLetterAnnotationClass,
) {

    @Bean
    fun errorInjector() = ErrorInjector().apply {
        deadLetterAnnotationClass.errorInjector = this
    }

    @Bean
    fun relayResultHolder() = RelayResultHolder().apply {
        deadLetterAnnotationClass.relayResultHolder = this
    }

    @Bean(name = ["relay1Depths"])
    fun relay1Depths(
        relayResultHolder: RelayResultHolder,
        errorInjector: ErrorInjector,
    ): Orchestrator<RelayEvent, RelayEvent> {
        return orchestratorFactory.create<RelayEvent>("relay1Depths")
            .start(
                orchestrate = {
                    it
                },
                rollback = {
                    if (errorInjector.doError) {
                        throw IllegalStateException("Error on rollback")
                    }
                    relayResultHolder.hold("relay1Depths", it)
                }
            )
            .commit {
                throw IllegalStateException("")
            }
    }

    @Bean(name = ["relay2Depths"])
    fun relay2Depths(
        relayResultHolder: RelayResultHolder,
        errorInjector: ErrorInjector,
    ): Orchestrator<RelayEvent, RelayEvent> {
        return orchestratorFactory.create<RelayEvent>("relay2Depths")
            .start(
                orchestrate = {
                    it
                },
                rollback = {
                    if (errorInjector.doError) {
                        throw IllegalStateException("Error on rollback")
                    }
                    relayResultHolder.hold("relay2Depths", it)
                }
            )
            .joinWithContext(
                contextOrchestrate = { _, request ->
                    request
                },
                contextRollback = { _, request ->
                    if (errorInjector.doError) {
                        throw IllegalStateException("Error on rollback")
                    }
                    relayResultHolder.hold("relay2Depths", request)
                }
            )
            .commit {
                throw IllegalStateException("")
            }
    }
}
