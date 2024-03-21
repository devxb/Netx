package org.rooftop.netx.engine

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.kotest.matchers.equals.shouldBeEqual
import org.rooftop.netx.api.Orchestrator
import org.rooftop.netx.meta.EnableDistributedTransaction
import org.rooftop.netx.redis.RedisContainer
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

@EnableDistributedTransaction
@ContextConfiguration(
    classes = [
        RedisContainer::class,
        OrchestratorConfigurer::class,
    ]
)
@DisplayName("Orchestrator 클래스의")
@TestPropertySource("classpath:application.properties")
class OrchestratorTest(
    private val numberOrchestrator: Orchestrator<Int, Int>,
    private val homeOrchestrator: Orchestrator<Home, Home>,
    private val instantOrchestrator: Orchestrator<InstantWrapper, InstantWrapper>,
    private val manyTypeOrchestrator: Orchestrator<Int, Home>,
    private val rollbackOrchestrator: Orchestrator<String, String>,
) : DescribeSpec({

    describe("numberOrchestrator 구현채는") {
        context("transaction 메소드가 호출되면,") {
            it("처음 입력받은 숫자에 orchestrate만큼의 수를 더한다.") {
                val result = numberOrchestrator.transactionSync(3)

                result.isSuccess shouldBeEqual true
                result.decodeResult(Int::class) shouldBeEqual 7
            }
        }
    }

    describe("homeOrchestrator 구현채는") {
        val expected = Home(
            "Korea, Seoul, Gangnam",
            mutableListOf(Person("Mother"), Person("Father"), Person("Son"))
        )

        context("transaction 메소드가 호출되면,") {
            it("처음 입력 받은 Home에 Mother, Father, Son을 추가한다.") {
                val result =
                    homeOrchestrator.transaction(Home("Korea, Seoul, Gangnam", mutableListOf()))
                        .block()

                result!!.isSuccess shouldBeEqual true
                result.decodeResult(Home::class) shouldBeEqualToComparingFields expected
            }
        }
    }

    describe("instantOrchestrator 구현채는") {
        val expected = InstantWrapper(Instant.now())

        context("transaction 메소드가 호출되면,") {
            it("처음 입력받은 instantWrapper를 그대로 반환한다.") {
                val result = instantOrchestrator.transactionSync(expected)

                result.isSuccess shouldBeEqual true
                result.decodeResult(InstantWrapper::class) shouldBeEqualToComparingFields expected
            }
        }
    }

    describe("manyTypeOrchestrator 구현채는") {
        val expected = Home("HOME", mutableListOf())

        context("transaction메소드가 호출되면,") {
            it("처음 Home을 반환한다.") {
                val result = manyTypeOrchestrator.transactionSync(1)

                result.isSuccess shouldBeEqual true
                result.decodeResult(Home::class) shouldBeEqualToComparingFields expected
            }
        }
    }

    describe("rollbackOrchestrator 구현채는") {
        val expected = listOf("1", "2", "3", "4", "-4", "-3", "-1")

        context("transaction 메소드가 호출되면,") {
            it("실패한 부분부터 위로 거슬러 올라가며 롤백한다") {
                val result = rollbackOrchestrator.transactionSync("")

                result.isSuccess shouldBeEqual false
                shouldThrowWithMessage<IllegalArgumentException>("Rollback") {
                    result.throwError()
                }
                eventually(5.seconds) {
                    rollbackOrchestratorResult shouldBeEqual expected
                }
            }
        }
    }
}) {
    data class Home(
        val address: String,
        val persons: MutableList<Person>
    ) {
        fun addPerson(person: Person) {
            persons.add(person)
        }
    }

    data class Person(val name: String)

    data class InstantWrapper(
        val time: Instant,
    )

    companion object {
        val rollbackOrchestratorResult = mutableListOf<String>()
    }
}
