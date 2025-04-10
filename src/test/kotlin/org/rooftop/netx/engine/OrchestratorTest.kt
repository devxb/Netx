package org.rooftop.netx.engine

import io.jsonwebtoken.JwtException
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldNotBe
import org.rooftop.netx.api.*
import org.rooftop.netx.meta.EnableSaga
import org.rooftop.netx.redis.RedisContainer
import org.rooftop.netx.spi.DeadLetterRegistry
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

@EnableSaga
@ContextConfiguration(
    classes = [
        RedisContainer::class,
        OrchestratorConfigurer::class,
    ]
)
@DisplayName("Orchestrator 클래스의")
@TestPropertySource("classpath:application.properties")
internal class OrchestratorTest(
    private val numberOrchestrator: Orchestrator<Int, Int>,
    private val homeOrchestrator: Orchestrator<Home, Home>,
    private val instantOrchestrator: Orchestrator<InstantWrapper, InstantWrapper>,
    private val manyTypeOrchestrator: Orchestrator<Int, Home>,
    private val deadLetterRegistry: DeadLetterRegistry,
    @Qualifier("rollbackOrchestrator") private val rollbackOrchestrator: Orchestrator<String, String>,
    @Qualifier("upChainRollbackOrchestrator") private val upChainRollbackOrchestrator: Orchestrator<String, String>,
    @Qualifier("monoRollbackOrchestrator") private val monoRollbackOrchestrator: Orchestrator<String, String>,
    @Qualifier("contextOrchestrator") private val contextOrchestrator: Orchestrator<String, String>,
    @Qualifier("pairOrchestrator") private val pairOrchestrator: Orchestrator<String, Pair<Foo, Foo>>,
    @Qualifier("startWithContextOrchestrator") private val startWithContextOrchestrator: Orchestrator<String, String>,
    @Qualifier("fooContextOrchestrator") private val fooContextOrchestrator: Orchestrator<String, List<Foo>>,
    private val privateOrchestrator: Orchestrator<Private, Private>,
    @Qualifier("throwOnStartOrchestrator") private val throwOnStartOrchestrator: Orchestrator<String, String>,
    @Qualifier("throwOnJoinOrchestrator") private val throwOnJoinOrchestrator: Orchestrator<String, String>,
    @Qualifier("throwOnStartWithContextOrchestrator") private val throwOnStartWithContextOrchestrator: Orchestrator<List<Home>, List<Home>>,
    @Qualifier("throwOnJoinWithContextOrchestrator") private val throwOnJoinWithContextOrchestrator: Orchestrator<List<Home>, List<Home>>,
    @Qualifier("throwOnCommitWithContextOrchestrator") private val throwOnCommitWithContextOrchestrator: Orchestrator<List<Home>, List<Home>>,
    @Qualifier("throwJwtExceptionOnStartOrchestrator") private val throwJwtExceptionOnStartOrchestrator: Orchestrator<String, String>,
    @Qualifier("throwHttpClientErrorExceptionOnStartOrchestrator") private val throwHttpClientErrorExceptionOnStartOrchestrator: Orchestrator<String, String>,
    @Qualifier("whenErrorOccurredRollbackThenAddDeadLetterOrchestrator") private val whenErrorOccurredRollbackThenAddDeadLetterOrchestrator: Orchestrator<String, String>,
    @Qualifier("whenErrorOccurredRollbackThenAddDeadLetterContextOrchestrator") private val whenErrorOccurredRollbackThenAddDeadLetterContextOrchestrator: Orchestrator<String, String>,
    @Qualifier("whenErrorOccurredRollbackThenAddDeadLetterReactiveOrchestrator") private val whenErrorOccurredRollbackThenAddDeadLetterReactiveOrchestrator: Orchestrator<String, String>,
) : DescribeSpec({

    describe("numberOrchestrator 구현채는") {
        context("saga 메소드가 호출되면,") {
            it("처음 입력받은 숫자에 orchestrate만큼의 수를 더한다.") {
                val result = numberOrchestrator.sagaSync(3)

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

        context("saga 메소드가 호출되면,") {
            it("처음 입력 받은 Home에 Mother, Father, Son을 추가한다.") {
                val result =
                    homeOrchestrator.saga(Home("Korea, Seoul, Gangnam", mutableListOf()))
                        .block()

                result!!.isSuccess shouldBeEqual true
                result.decodeResult(Home::class) shouldBeEqualToComparingFields expected
            }
        }
    }

    describe("instantOrchestrator 구현채는") {
        val expected = InstantWrapper(Instant.now())

        context("saga 메소드가 호출되면,") {
            it("처음 입력받은 instantWrapper를 그대로 반환한다.") {
                val result = instantOrchestrator.sagaSync(expected)

                result.isSuccess shouldBeEqual true
                result.decodeResult(InstantWrapper::class) shouldBeEqualToComparingFields expected
            }
        }
    }

    describe("manyTypeOrchestrator 구현채는") {
        val expected = Home("HOME", mutableListOf())

        context("saga 메소드가 호출되면,") {
            it("처음 Home을 반환한다.") {
                val result = manyTypeOrchestrator.sagaSync(1)

                result.isSuccess shouldBeEqual true
                result.decodeResult(Home::class) shouldBeEqualToComparingFields expected
            }
        }
    }

    describe("rollbackOrchestrator 구현채는") {
        val expected = listOf("1", "2", "3", "4", "-3", "-1")

        context("saga 메소드가 호출되면,") {
            it("실패한 부분 위부터 위로 거슬러 올라가며 롤백한다") {
                val result = rollbackOrchestrator.sagaSync("")

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

    describe("upStreamRollbackOrchestrator 구현채는") {
        val expected = listOf("1", "2", "3", "4", "-3", "-1")

        it("호출할 rollback function이 없으면, 가장 가까운 상단의 rollback을 호출한다.") {
            val result = upChainRollbackOrchestrator.sagaSync("")

            result.isSuccess shouldBeEqual false
            shouldThrowWithMessage<IllegalArgumentException>("Rollback for test") {
                result.throwError()
            }
            eventually(5.seconds) {
                upChainResult shouldBeEqual expected
            }
        }
    }

    describe("monoRollbackOrchestrator 구현채는") {
        context("saga 메소드가 호출되면,") {
            val expected = listOf("1", "2", "3", "4", "-3", "-1")

            it("실패한 부분위부터 위로 거슬러 올라가며 롤백한다.") {
                val result = monoRollbackOrchestrator.sagaSync("")

                result.isSuccess shouldBeEqual false
                shouldThrowWithMessage<IllegalArgumentException>("Rollback for test") {
                    result.throwError()
                }
                eventually(5.seconds) {
                    monoRollbackResult shouldBeEqual expected
                }
            }
        }
    }

    describe("contextOrchestrator 구현채는") {
        context("saga 메소드가 호출되면,") {
            val expected = listOf("0", "1", "2", "r2")

            it("context 에서 아이템을 교환하며 Saga를 진행한다.") {
                val result = contextOrchestrator.sagaSync("0")

                result.isSuccess shouldBeEqual false
                shouldThrowWithMessage<IllegalArgumentException>("Rollback") {
                    result.throwError()
                }
                eventually(5.seconds) {
                    contextResult shouldBeEqual expected
                }
            }
        }
    }

    describe("pairOrchestrator 구현채는") {
        context("saga 메소드가 호출되면,") {
            it("입력받은 파라미터를 name 으로 갖는 Foo pair 를 반환한다. ") {
                val result = pairOrchestrator.sagaSync("james")

                result.isSuccess shouldBeEqual false
                shouldThrowWithMessage<IllegalArgumentException>("Rollback") {
                    result.throwError()
                }
            }
        }
    }

    describe("startWithContextOrchestrator 구현채는") {
        context("context와 함께 saga 메소드가 호출되면,") {
            it("key에 해당하는 context를 반환한다.") {
                val result = startWithContextOrchestrator.sagaSync(
                    "ignored request",
                    mutableMapOf("key" to "hello")
                )

                result.decodeResultOrThrow(String::class) shouldBeEqual "hello"
            }
        }
    }

    describe("fooContextOrchestrator 구현채는") {
        context("context 와 함께 saga 메소드가 호출되면,") {
            val expected = listOf(
                Foo("startSync"),
                Foo("startWithContext"),
                Foo("joinWithContext"),
            )

            it("0,1,2 Foo가 들어있는 Foo list를 반환한다.") {
                val result = fooContextOrchestrator.sagaSync(
                    "",
                    mutableMapOf("0" to Foo("startSync"))
                )

                result.isSuccess shouldBeEqual true
                result.decodeResultOrThrow(object :
                    TypeReference<List<Foo>>() {}) shouldBeEqual expected
            }
        }
    }

    describe("privateFieldOrchestrator 구현채는") {
        context("saga 메소드가 호출되면,") {
            val private = Private("I'm private")

            it("Private 필드가 포함된 객체를 반환한다.") {
                val result = privateOrchestrator.sagaSync(private)

                result.isSuccess shouldBeEqual true
                result.decodeResultOrThrow(Private::class) shouldBeEqual private
            }
        }
    }

    describe("throwOnStartOrchestrator 구현채는") {
        context("start에서 예외가 던져지면,") {
            it("해당 예외를 Result에서 throw한다.") {
                shouldThrowWithMessage<IllegalArgumentException>("Throw error for test.") {
                    throwOnStartOrchestrator.sagaSync("throw error in start.")
                        .decodeResultOrThrow(String::class)
                }
            }
        }
    }

    describe("throwOnJoinOrchestrator 구현채는") {
        context("join에서 예외가 던져지면,") {
            it("해당 예외를 Result에서 throw한다.") {
                shouldThrowWithMessage<IllegalArgumentException>("Throw error for test.") {
                    throwOnJoinOrchestrator.sagaSync("throw error in join.")
                        .decodeResultOrThrow(String::class)
                }
            }
        }
    }

    describe("throwOnStartWithContextOrchestrator 구현채는") {
        context("startWithContext에서 예외가 던져지면,") {
            it("해당 예외를 Result에서 throw한다.") {
                shouldThrowWithMessage<IllegalArgumentException>("Throw error for test.") {
                    throwOnStartWithContextOrchestrator.sagaSync(listOf())
                        .decodeResultOrThrow(object : TypeReference<List<Home>>() {})
                }
            }
        }
    }

    describe("throwOnJoinWithContextOrchestrator 구현채는") {
        context("joinWithContext에서 예외가 던져지면,") {
            it("해당 예외를 Result에서 throw한다.") {
                shouldThrowWithMessage<IllegalArgumentException>("Throw error for test.") {
                    throwOnJoinWithContextOrchestrator.sagaSync(listOf())
                        .decodeResultOrThrow(object : TypeReference<List<Home>>() {})
                }
            }
        }
    }

    describe("throwOnCommitWithContextOrchestrator 구현채는") {
        context("commitWithContext에서 예외가 던져지면,") {
            it("해당 예외를 Result에서 throw한다.") {
                shouldThrowWithMessage<IllegalArgumentException>("Throw error for test.") {
                    throwOnCommitWithContextOrchestrator.sagaSync(listOf())
                        .decodeResultOrThrow(object : TypeReference<List<Home>>() {})
                }
            }
        }
    }

    describe("throwJwtExceptionOnStartOrchestrator 구현채는") {
        context("JwtException이 던져져도,") {
            it("해당 예외를 Result에 담고 timeout시간안에 예외를 반환한다") {
                shouldThrowWithMessage<JwtException>("Authorization fail") {
                    throwJwtExceptionOnStartOrchestrator.sagaSync("")
                        .decodeResultOrThrow(String::class)
                }
            }
        }
    }

    describe("throwHttpClientErrorExceptionOnStartOrchestrator 구현채는") {
        context("처리할 수 없는 HttpClientErrorException이 던져지면") {
            it("ResultException을 Result에 담고 timeout시간안에 예외를 반환한다") {
                shouldThrowExactly<ResultException> {
                    throwHttpClientErrorExceptionOnStartOrchestrator.sagaSync("")
                        .decodeResultOrThrow(String::class)
                }
            }
        }
    }

    describe("whenErrorOccurredRollbackThenAddDeadLetterOrchestrator 구현채는") {
        context("롤백 파이프라인에서 에러가 던져지면,") {
            var result: SagaEvent? = null
            deadLetterRegistry.addListener { _, event ->
                result = event
            }

            it("DeadLetterQueue 에 메시지를 저장한다") {
                whenErrorOccurredRollbackThenAddDeadLetterOrchestrator.sagaSync("")

                eventually(5.seconds) {
                    result shouldNotBe null
                }
            }
        }
    }

    describe("whenErrorOccurredRollbackThenAddDeadLetterContextOrchestrator 구현채는") {
        context("롤백 파이프라인에서 에러가 던져지면,") {
            var result: SagaEvent? = null
            deadLetterRegistry.addListener { _, event ->
                result = event
            }

            it("DeadLetterQueue 에 메시지를 저장한다") {
                whenErrorOccurredRollbackThenAddDeadLetterContextOrchestrator.sagaSync("")

                eventually(5.seconds) {
                    result shouldNotBe null
                }
            }
        }
    }

    describe("whenErrorOccurredRollbackThenAddDeadLetterReactiveOrchestrator 구현채는") {
        context("롤백 파이프라인에서 에러가 던져지면,") {
            var result: SagaEvent? = null
            deadLetterRegistry.addListener { _, event ->
                result = event
            }

            it("DeadLetterQueue 에 메시지를 저장한다") {
                whenErrorOccurredRollbackThenAddDeadLetterReactiveOrchestrator.sagaSync("")

                eventually(5.seconds) {
                    result shouldNotBe null
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

    data class Foo(val name: String)

    data class Private(private val name: String)

    companion object {
        val rollbackOrchestratorResult = mutableListOf<String>()
        val upChainResult = mutableListOf<String>()
        val monoRollbackResult = mutableListOf<String>()
        val contextResult = mutableListOf<String>()
    }
}
