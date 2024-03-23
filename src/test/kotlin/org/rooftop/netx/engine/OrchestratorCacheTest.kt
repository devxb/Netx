package org.rooftop.netx.engine

import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.equals.shouldNotBeEqual
import org.rooftop.netx.api.Orchestrator
import org.rooftop.netx.factory.OrchestratorFactory
import org.rooftop.netx.meta.EnableDistributedTransaction
import org.rooftop.netx.redis.RedisContainer
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource

@EnableDistributedTransaction
@DisplayName("OrchestratorCache 클래스의")
@ContextConfiguration(classes = [RedisContainer::class])
@TestPropertySource("classpath:application.properties")
internal class OrchestratorCacheTest(
    private val orchestratorFactory: OrchestratorFactory,
) : DescribeSpec({

    describe("cache 메소드는") {
        context("같은 orchestratorId를 가진 Orchestrator 를 생성하려하면,") {
            val expected = orchestratorFactory.createIntOrchestrator("same")

            it("동일한 Orchestrator 를 반환한다.") {
                val sameOrchestrator = orchestratorFactory.createIntOrchestrator("same")

                expected shouldBeEqual sameOrchestrator
            }
        }

        context("다른 orchestratorId를 가진 Orchestrator 를 생성하려하면,") {
            val expected = orchestratorFactory.createIntOrchestrator("same")

            it("동일하지 않은 Orchestrator 를 반환한다.") {
                val notSameOrchestrator = orchestratorFactory.createIntOrchestrator("notSame")

                expected shouldNotBeEqual notSameOrchestrator
            }
        }
    }

}) {

    companion object {
        
        private fun OrchestratorFactory.createIntOrchestrator(orchestratorId: String): Orchestrator<Int, Int> {
            return this.create<Int>(orchestratorId)
                .start({ it + 1 })
                .commit({ it + 1 })
        }
    }
}
