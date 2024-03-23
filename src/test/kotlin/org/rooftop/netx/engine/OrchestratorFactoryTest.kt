package org.rooftop.netx.engine

import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.equals.shouldNotBeEqual
import org.rooftop.netx.api.Orchestrator
import org.rooftop.netx.meta.EnableDistributedTransaction
import org.rooftop.netx.redis.RedisContainer
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource

@EnableDistributedTransaction
@DisplayName("OrchestratorFactory 클래스의")
@ContextConfiguration(classes = [RedisContainer::class])
@TestPropertySource("classpath:application.properties")
internal class OrchestratorFactoryTest(
    private val orchestratorFactory: OrchestratorFactory,
) : DescribeSpec({

    describe("create 메소드는") {
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

    describe("get 메소드는") {
        context("존재하는 orchestratorId를 입력받으면,") {
            val expected = orchestratorFactory.createIntOrchestrator("exist")

            it("해당하는 Orchestrator를 반환한다.") {
                val existOrchestrator = orchestratorFactory.get<Int, Int>("exist")

                expected shouldBeEqual existOrchestrator
            }
        }

        context("존재하지 않는 orchestratorId를 입력받으면,") {
            it("IllegalArgumentException을 던진다.") {
                shouldThrowWithMessage<IllegalArgumentException>("Cannot find orchestrator by orchestratorId \"notExists\"")
                {
                    orchestratorFactory.get<Int, Int>("notExists")
                }
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
