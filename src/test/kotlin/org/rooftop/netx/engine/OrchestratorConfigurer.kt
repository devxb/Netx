package org.rooftop.netx.engine

import org.rooftop.netx.api.Orchestrator
import org.rooftop.netx.meta.AbstractOrchestratorConfigurer
import org.springframework.context.annotation.Bean
import reactor.core.publisher.Mono

class OrchestratorConfigurer : AbstractOrchestratorConfigurer() {

    @Bean(name = ["numberOrchestrator"])
    fun numberOrchestrator(): Orchestrator<Int> {
        return newOrchestrator().startSync {
            it.decodeEvent(Int::class) + 1
        }.joinSync {
            it.decodeEvent(Int::class) + 1
        }.join {
            Mono.just(it.decodeEvent(Int::class))
                .map { number -> number + 1 }
        }.commitSync {
            it.decodeEvent(Int::class) + 1
        }.build()
    }

    @Bean(name = ["homeOrchestrator"])
    fun homeOrchestrator(): Orchestrator<OrchestratorTest.Home> {
        return newOrchestrator().start {
            Mono.just(it.decodeEvent(OrchestratorTest.Home::class))
                .map { home ->
                    home.addPerson(OrchestratorTest.Person("Grand mother"))
                    home
                }
        }.joinSync {
            val home = it.decodeEvent(OrchestratorTest.Home::class)
            home.addPerson(OrchestratorTest.Person("Father"))
            home
        }.join {
            Mono.just(it.decodeEvent(OrchestratorTest.Home::class))
                .map { home ->
                    home.addPerson(OrchestratorTest.Person("Mother"))
                    home
                }
        }.joinSync {
            val home = it.decodeEvent(OrchestratorTest.Home::class)
            home.addPerson(OrchestratorTest.Person("Son"))
            home
        }.commit {
            Mono.just(it.decodeEvent(OrchestratorTest.Home::class))
        }.build()
    }

    @Bean(name = ["rollbackOrchestrator"])
    fun rollbackOrchestrator(): Orchestrator<String> {
        return newOrchestrator().startSync {
            "Pending order"
        }.joinSync {
            "Pending pay"
        }.joinSync {
            "Success pay"
        }.joinSync {
            "Success order"
        }.joinSync {
            throw IllegalArgumentException("Fail consume product")
        }.commitSync {
            "Success order"
        }.rollbackSync {
            "Rollback"
        }.build()
    }

    @Bean(name = ["noRollbackForOrchestrator"])
    fun noRollbackForOrchestrator(): Orchestrator<String> {
        var rollbackTrig = 1
        return newOrchestrator().startSync(IllegalArgumentException::class) {
            rollbackTrig++
            if (rollbackTrig <= 2) {
                throw IllegalArgumentException("startSync : Retry cause $rollbackTrig <= 2")
            }
            "startSync"
        }.joinSync(IllegalStateException::class) {
            rollbackTrig++
            if (rollbackTrig <= 4) {
                throw IllegalStateException("joinSync : Retry cause $rollbackTrig <= 4")
            }
            "joinSync"
        }.join(NullPointerException::class) {
            Mono.just(rollbackTrig)
                .map {
                    rollbackTrig++
                    if (rollbackTrig <= 6) {
                        throw NullPointerException("join : Retry cause $rollbackTrig <= 6")
                    }
                    NullPointerException("Success no rollback for")
                }
        }.commitSync(Exception::class) {
            it.decodeEvent(NullPointerException::class)
        }.build()
    }

    @Bean(name = ["timeOutOrchestrator"])
    fun timeOutOrchestrator(): Orchestrator<String> {
        return newOrchestrator().startSync {
            Thread.sleep(2000)
            ""
        }.build()
    }
}
