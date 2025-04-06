package org.rooftop.netx.engine

import io.jsonwebtoken.JwtException
import org.rooftop.netx.api.*
import org.rooftop.netx.api.OrchestratorFactory
import org.rooftop.netx.engine.OrchestratorConfigurer.ListOrchestrate
import org.rooftop.netx.engine.OrchestratorTest.Companion.contextResult
import org.rooftop.netx.engine.OrchestratorTest.Companion.monoRollbackResult
import org.rooftop.netx.engine.OrchestratorTest.Companion.rollbackOrchestratorResult
import org.rooftop.netx.engine.OrchestratorTest.Companion.upChainResult
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import reactor.core.publisher.Mono
import java.time.Instant

@Configuration
internal class OrchestratorConfigurer(
    private val orchestratorFactory: OrchestratorFactory,
) {

    @Bean(name = ["numberOrchestrator"])
    fun numberOrchestrator(): Orchestrator<Int, Int> {
        return OrchestratorFactory.instance().create<Int>("numberOrchestrator")
            .start(orchestrate = { it + 1 })
            .join(orchestrate = { it + 1 })
            .joinReactive(orchestrate = { Mono.just(it + 1) })
            .commit(orchestrate = { it + 1 })
    }

    @Bean(name = ["homeOrchestrator"])
    fun homeOrchestrator(): Orchestrator<OrchestratorTest.Home, OrchestratorTest.Home> {
        return OrchestratorFactory.instance().create<OrchestratorTest.Home>("homeOrchestrator")
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
        return OrchestratorFactory.instance()
            .create<OrchestratorTest.InstantWrapper>("instantOrchestrator")
            .start({ it })
            .commit({ it })
    }

    @Bean(name = ["manyTypeOrchestrator"])
    fun manyTypeOrchestrator(): Orchestrator<Int, OrchestratorTest.Home> {
        return OrchestratorFactory.instance().create<Int>("manyTypeOrchestrator")
            .start({ "String" })
            .join({ 1L })
            .join({ 0.1 })
            .join({ OrchestratorTest.InstantWrapper(Instant.now()) })
            .commit({ OrchestratorTest.Home("HOME", mutableListOf()) })
    }

    @Bean(name = ["rollbackOrchestrator"])
    fun rollbackOrchestrator(): Orchestrator<String, String> {
        return OrchestratorFactory.instance().create<String>("rollbackOrchestrator")
            .start(
                orchestrate = {
                    rollbackOrchestratorResult.add("1")
                },
                rollback = {
                    rollbackOrchestratorResult.add("-1")
                }
            )
            .join(
                orchestrate = {
                    rollbackOrchestratorResult.add("2")
                }
            )
            .join(
                orchestrate = {
                    rollbackOrchestratorResult.add("3")
                },
                rollback = { rollbackOrchestratorResult.add("-3") }
            )
            .commit(
                orchestrate = {
                    rollbackOrchestratorResult.add("4")
                    throw IllegalArgumentException("Rollback")
                },
            )
    }

    @Bean(name = ["upChainRollbackOrchestrator"])
    fun upChainRollbackOrchestrator(): Orchestrator<String, String> {
        return OrchestratorFactory.instance().create<String>("upChainRollbackOrchestrator")
            .start({ upChainResult.add("1") }, { upChainResult.add("-1") })
            .join({ upChainResult.add("2") })
            .join({ upChainResult.add("3") }, { upChainResult.add("-3") })
            .commit({
                upChainResult.add("4")
                throw IllegalArgumentException("Rollback for test")
            })
    }

    @Bean(name = ["monoRollbackOrchestrator"])
    fun monoRollbackOrchestrator(): Orchestrator<String, String> {
        return OrchestratorFactory.instance().create<String>("monoRollbackOrchestrator")
            .startReactive(
                { Mono.fromCallable { monoRollbackResult.add("1") } },
                { Mono.fromCallable { monoRollbackResult.add("-1") } }
            )
            .joinReactive({ Mono.fromCallable { monoRollbackResult.add("2") } })
            .joinReactive(
                { Mono.fromCallable { monoRollbackResult.add("3") } },
                { Mono.fromCallable { monoRollbackResult.add("-3") } }
            )
            .commitReactive({
                Mono.fromCallable {
                    monoRollbackResult.add("4")
                    throw IllegalArgumentException("Rollback for test")
                }
            })
    }

    @Bean(name = ["contextOrchestrator"])
    fun contextOrchestrator(): Orchestrator<String, String> {
        return OrchestratorFactory.instance().create<String>("contextOrchestrator")
            .startWithContext(
                contextOrchestrate = { context, request ->
                    context.set("start-1", request)
                    "1"
                },
                contextRollback = { context, _ ->
                    val start1 = context.decodeContext("start-1", String::class)
                    val join2 = context.decodeContext("join-2", String::class)
                    val join3 = context.decodeContext("join-3", String::class)
                    val rJoin3 = context.decodeContext("r-join-3", String::class)

                    contextResult.addAll(listOf(start1, join2, join3, rJoin3))
                }
            )
            .joinWithContext(
                contextOrchestrate = { context, request ->
                    context.set("join-2", request)
                    "2"
                }
            )
            .joinReactiveWithContext(
                contextOrchestrate = { context, request ->
                    Mono.fromCallable {
                        context.set("join-3", request)
                        "3"
                    }
                },
                contextRollback = { context, request ->
                    Mono.fromCallable {
                        context.set("r-join-3", "r$request")
                    }
                }
            )
            .commitWithContext(
                contextOrchestrate = { context, request ->
                    context.set("commit-4", request)
                    throw IllegalArgumentException("Rollback")
                },
            )
    }

    @Bean(name = ["pairOrchestrator"])
    fun pairOrchestrator(): Orchestrator<String, Pair<OrchestratorTest.Foo, OrchestratorTest.Foo>> {
        return OrchestratorFactory.instance().create<String>("pairOrchestrator")
            .start({ OrchestratorTest.Foo(it) to OrchestratorTest.Foo(it) })
            .join(PairOrchestrate, PairRollback)
            .joinReactive(MonoPairOrchestrate, MonoPairRollback)
            .commit(object :
                Orchestrate<Pair<OrchestratorTest.Foo, OrchestratorTest.Foo>, Pair<OrchestratorTest.Foo, OrchestratorTest.Foo>> {
                override fun orchestrate(request: Pair<OrchestratorTest.Foo, OrchestratorTest.Foo>): Pair<OrchestratorTest.Foo, OrchestratorTest.Foo> {
                    throw IllegalArgumentException("Rollback")
                }

                override fun reified(): TypeReference<Pair<OrchestratorTest.Foo, OrchestratorTest.Foo>> {
                    return object :
                        TypeReference<Pair<OrchestratorTest.Foo, OrchestratorTest.Foo>>() {}
                }
            })
    }

    @Bean(name = ["startWithContextOrchestrator"])
    fun startWithContextOrchestrator(): Orchestrator<String, String> {
        return OrchestratorFactory.instance().create<String>("startWithContextOrchestrator")
            .startWithContext({ context, _ ->
                context.decodeContext("key", String::class)
            })
            .commitWithContext({ context, _ ->
                context.decodeContext("key", String::class)
            })
    }

    @Bean(name = ["fooContextOrchestrator"])
    fun fooContextOrchestrator(): Orchestrator<String, List<OrchestratorTest.Foo>> {
        return OrchestratorFactory.instance().create<String>("fooContextOrchestrator")
            .startWithContext({ context, _ ->
                val before = context.decodeContext("0", OrchestratorTest.Foo::class)
                context.set("1", OrchestratorTest.Foo("startWithContext"))
            })
            .joinWithContext({ context, _ ->
                val before = context.decodeContext("1", OrchestratorTest.Foo::class)
                context.set("2", OrchestratorTest.Foo("joinWithContext"))
            })
            .commitWithContext({ context, _ ->
                val before = context.decodeContext("2", OrchestratorTest.Foo::class)
                listOf(
                    context.decodeContext("0", OrchestratorTest.Foo::class),
                    context.decodeContext("1", OrchestratorTest.Foo::class),
                    context.decodeContext("2", OrchestratorTest.Foo::class),
                )
            })
    }

    @Bean(name = ["privateFieldOrchestrator"])
    fun privateFieldOrchestrator(): Orchestrator<OrchestratorTest.Private, OrchestratorTest.Private> {
        return OrchestratorFactory.instance()
            .create<OrchestratorTest.Private>("privateFieldOrchestrator")
            .start({ it })
            .join({ it })
            .commit({ it })
    }

    @Bean(name = ["throwOnStartOrchestrator"])
    fun throwOnStartOrchestrator(): Orchestrator<String, String> {
        return OrchestratorFactory.instance()
            .create<String>("throwOnStartOrchestrator")
            .start(
                orchestrate = {
                    throw IllegalArgumentException("Throw error for test.")
                }
            )
            .commit({
                "Never reach this line."
            })
    }

    @Bean(name = ["throwOnJoinOrchestrator"])
    fun throwOnJoinOrchestrator(): Orchestrator<String, String> {
        return OrchestratorFactory.instance()
            .create<String>("throwOnJoinOrchestrator")
            .start({
                "start success"
            })
            .join({
                throw IllegalArgumentException("Throw error for test.")
            })
            .commit({
                "Never reach this line."
            })
    }

    @Bean(name = ["throwOnStartWithContextOrchestrator"])
    fun throwOnStartWithContextOrchestrator(): Orchestrator<List<OrchestratorTest.Home>, List<OrchestratorTest.Home>> {
        return OrchestratorFactory.instance()
            .create<List<OrchestratorTest.Home>>("throwOnStartWithContextOrchestrator")
            .startWithContext(ListOrchestrate { _, _ ->
                throw IllegalArgumentException("Throw error for test.")
            })
            .joinWithContext(ListOrchestrate { _, _ ->
                listOf(OrchestratorTest.Home("", mutableListOf()))
            })
            .commitWithContext(ListOrchestrate { _, _ ->
                listOf(OrchestratorTest.Home("", mutableListOf()))
            })
    }

    @Bean(name = ["throwOnJoinWithContextOrchestrator"])
    fun throwOnJoinWithContextOrchestrator(): Orchestrator<List<OrchestratorTest.Home>, List<OrchestratorTest.Home>> {
        return OrchestratorFactory.instance()
            .create<List<OrchestratorTest.Home>>("throwOnJoinWithContextOrchestrator")
            .startWithContext(ListOrchestrate { _, _ ->
                listOf(OrchestratorTest.Home("", mutableListOf()))
            })
            .joinWithContext(ListOrchestrate { _, _ ->
                throw IllegalArgumentException("Throw error for test.")
            })
            .commitWithContext(ListOrchestrate { _, _ ->
                listOf(OrchestratorTest.Home("", mutableListOf()))
            })
    }

    @Bean(name = ["throwOnCommitWithContextOrchestrator"])
    fun throwOnCommitWithContextOrchestrator(): Orchestrator<List<OrchestratorTest.Home>, List<OrchestratorTest.Home>> {
        return OrchestratorFactory.instance()
            .create<List<OrchestratorTest.Home>>("throwOnCommitWithContextOrchestrator")
            .startWithContext(ListOrchestrate { _, _ ->
                listOf(OrchestratorTest.Home("", mutableListOf()))
            })
            .joinWithContext(ListOrchestrate { _, _ ->
                listOf(OrchestratorTest.Home("", mutableListOf()))
            })
            .commitWithContext(ListOrchestrate { _, _ ->
                throw IllegalArgumentException("Throw error for test.")
            })
    }

    @Bean(name = ["throwJwtExceptionOnStartOrchestrator"])
    fun throwJwtExceptionOnStartOrchestrator(): Orchestrator<String, String> {
        return OrchestratorFactory.instance()
            .create<String>("throwJwtExceptionOnStartOrchestrator")
            .startWithContext({ context, event ->
                throw JwtException("Authorization fail")
            })
            .joinWithContext({ _, _ -> "" })
            .commit { "" }
    }

    @Bean(name = ["throwHttpClientErrorExceptionOnStartOrchestrator"])
    fun throwHttpClientErrorExceptionOnStartOrchestrator(): Orchestrator<String, String> {
        return OrchestratorFactory.instance()
            .create<String>("throwHttpClientErrorExceptionOnStartOrchestrator")
            .startWithContext({ _, _ ->
                throw HttpClientErrorException(HttpStatus.UNAUTHORIZED)
            })
            .commitWithContext({ _, _ -> "" })
    }

    @Bean(name = ["whenErrorOccurredRollbackThenAddDeadLetterOrchestrator"])
    fun whenErrorOccurredRollbackThenAddDeadLetterOrchestrator(): Orchestrator<String, String> {
        return OrchestratorFactory.instance()
            .create<String>("whenErrorOccurredRollbackThenAddDeadLetterOrchestrator")
            .start(
                orchestrate = { "" },
                rollback = {
                    throw IllegalStateException("Add dead letter")
                }
            )
            .commit(
                orchestrate = {
                    throw IllegalStateException("Throw error")
                }
            )
    }

    @Bean(name = ["whenErrorOccurredRollbackThenAddDeadLetterContextOrchestrator"])
    fun whenErrorOccurredRollbackThenAddDeadLetterContextOrchestrator(): Orchestrator<String, String> {
        return OrchestratorFactory.instance()
            .create<String>("whenErrorOccurredRollbackThenAddDeadLetterContextOrchestrator")
            .startWithContext(
                contextOrchestrate = { _, _ -> "" },
                contextRollback = { _, _ ->
                    throw IllegalStateException("Add dead letter")
                }
            )
            .joinWithContext(
                contextOrchestrate = { _, _ -> "" },
                contextRollback = { _, _ ->
                    throw IllegalStateException("Add dead letter")
                }
            )
            .commitWithContext { _, _ ->
                throw IllegalStateException("Throw error")
            }
    }

    @Bean(name = ["whenErrorOccurredRollbackThenAddDeadLetterReactiveOrchestrator"])
    fun whenErrorOccurredRollbackThenAddDeadLetterReactiveOrchestrator(): Orchestrator<String, String> {
        return OrchestratorFactory.instance()
            .create<String>("whenErrorOccurredRollbackThenAddDeadLetterContextOrchestrator")
            .startReactive(
                orchestrate = { Mono.just("") },
                rollback = {
                    throw IllegalStateException("Add dead letter")
                }
            )
            .joinReactive(
                orchestrate = { Mono.just("") },
                rollback = {
                    throw IllegalStateException("Add dead letter")
                }
            )
            .commitReactive {
                throw IllegalStateException("Throw error")
            }
    }

    fun interface ListOrchestrate :
        ContextOrchestrate<List<OrchestratorTest.Home>, List<OrchestratorTest.Home>> {

        override fun reified(): TypeReference<List<OrchestratorTest.Home>>? {
            return object : TypeReference<List<OrchestratorTest.Home>>() {}
        }
    }

    object PairOrchestrate :
        Orchestrate<Pair<OrchestratorTest.Foo, OrchestratorTest.Foo>, Pair<OrchestratorTest.Foo, OrchestratorTest.Foo>> {
        override fun orchestrate(request: Pair<OrchestratorTest.Foo, OrchestratorTest.Foo>): Pair<OrchestratorTest.Foo, OrchestratorTest.Foo> {
            return OrchestratorTest.Foo(request.first.name) to OrchestratorTest.Foo(request.first.name)
        }

        override fun reified(): TypeReference<Pair<OrchestratorTest.Foo, OrchestratorTest.Foo>> {
            return object : TypeReference<Pair<OrchestratorTest.Foo, OrchestratorTest.Foo>>() {}
        }
    }

    object PairRollback :
        Rollback<Pair<OrchestratorTest.Foo, OrchestratorTest.Foo>, Pair<OrchestratorTest.Foo, OrchestratorTest.Foo>> {
        override fun rollback(request: Pair<OrchestratorTest.Foo, OrchestratorTest.Foo>): Pair<OrchestratorTest.Foo, OrchestratorTest.Foo> {
            return OrchestratorTest.Foo(request.first.name) to OrchestratorTest.Foo(request.first.name)
        }

        override fun reified(): TypeReference<Pair<OrchestratorTest.Foo, OrchestratorTest.Foo>> {
            return object : TypeReference<Pair<OrchestratorTest.Foo, OrchestratorTest.Foo>>() {}
        }
    }

    object MonoPairOrchestrate :
        Orchestrate<Pair<OrchestratorTest.Foo, OrchestratorTest.Foo>, Mono<Pair<OrchestratorTest.Foo, OrchestratorTest.Foo>>> {

        override fun orchestrate(request: Pair<OrchestratorTest.Foo, OrchestratorTest.Foo>): Mono<Pair<OrchestratorTest.Foo, OrchestratorTest.Foo>> {
            return Mono.fromCallable {
                OrchestratorTest.Foo(request.first.name) to OrchestratorTest.Foo(
                    request.first.name
                )
            }
        }

        override fun reified(): TypeReference<Pair<OrchestratorTest.Foo, OrchestratorTest.Foo>> {
            return object : TypeReference<Pair<OrchestratorTest.Foo, OrchestratorTest.Foo>>() {}
        }
    }

    object MonoPairRollback :
        Rollback<Pair<OrchestratorTest.Foo, OrchestratorTest.Foo>, Mono<*>> {
        override fun rollback(request: Pair<OrchestratorTest.Foo, OrchestratorTest.Foo>): Mono<Pair<OrchestratorTest.Foo, OrchestratorTest.Foo>> {
            return Mono.fromCallable {
                OrchestratorTest.Foo(request.first.name) to OrchestratorTest.Foo(
                    request.first.name
                )
            }
        }

        override fun reified(): TypeReference<Pair<OrchestratorTest.Foo, OrchestratorTest.Foo>> {
            return object : TypeReference<Pair<OrchestratorTest.Foo, OrchestratorTest.Foo>>() {}
        }
    }
}
