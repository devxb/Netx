package org.rooftop.netx.engine

import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import org.rooftop.netx.api.Orchestrator
import org.rooftop.netx.meta.EnableDistributedTransaction
import org.rooftop.netx.redis.RedisContainer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource

@EnableDistributedTransaction
@ContextConfiguration(
    classes = [
        RedisContainer::class,
        OrchestratorConfigurer::class,
    ]
)
@DisplayName("Orchestrator 클래스의")
@TestPropertySource("classpath:fast-recover-mode.properties")
class OrchestratorTest(
    private val numberOrchestrator: Orchestrator<Int>,
    private val homeOrchestrator: Orchestrator<Home>,
    @Qualifier("rollbackOrchestrator") private val rollbackOrchestrator: Orchestrator<String>,
    @Qualifier("noRollbackForOrchestrator") private val noRollbackForOrchestrator: Orchestrator<String>,
) : DescribeSpec({

    describe("numberOrchestrator 구현채는") {
        context("transaction 메소드가 호출되면,") {
            it("처음 입력받은 숫자에 orchestrate만큼의 수를 더한다.") {
                val number = numberOrchestrator.transactionSync(3).decodeResult(Int::class)

                number shouldBeEqual 7
            }
        }
    }

    describe("homeOrchestrator 구현채는") {
        context("아무도 없는 Home을 입력받으면,") {
            val address = "Gangnam, Seoul, South Korea"
            val expected = Home(
                address, mutableListOf(
                    Person("Grand mother"),
                    Person("Father"),
                    Person("Mother"),
                    Person("Son"),
                )
            )

            it("Home에 [Grand mother, Father, Mother, Son] 을 추가한다.") {
                val home = homeOrchestrator.transactionSync(Home(address, mutableListOf()))
                    .decodeResult(Home::class)

                home shouldBeEqualToComparingFields expected
            }
        }
    }

    describe("rollbackOrchestrator 구현채는") {
        context("요청이 실패할경우,") {
            it("isSuccess로 false와 Rollback 메시지를 반환한다.") {
                val result = rollbackOrchestrator.transactionSync("order")

                result.isSuccess shouldBe false
                result.decodeResult(String::class) shouldBe "Rollback"
            }
        }
    }

    describe("noRollbackForOrchestrator 구현채는") {
        context("noRollbackFor에 해당하는 예외가 던져지면,") {
            it("retry를 진행하고, rollback을 하지않는다.") {
                val result = noRollbackForOrchestrator.transactionSync("Start transaction")

                result.isSuccess shouldBe true
                result.decodeResult(NullPointerException::class).message!! shouldBeEqual "Success no rollback for"
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
}
