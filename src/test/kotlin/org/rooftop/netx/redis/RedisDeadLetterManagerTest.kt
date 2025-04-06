package org.rooftop.netx.redis

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import org.rooftop.netx.api.DeadLetterRelay
import org.rooftop.netx.api.Orchestrator
import org.rooftop.netx.api.SagaManager
import org.rooftop.netx.engine.*
import org.rooftop.netx.meta.EnableSaga
import org.rooftop.netx.spi.DeadLetterRegistry
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import kotlin.time.Duration.Companion.seconds


@EnableSaga
@ContextConfiguration(
    classes = [
        RedisContainer::class,
        DeadLetterConfigurer::class,
        DeadLetterAnnotationClass::class,
    ]
)
@DisplayName("DeadLetterRelay 클래스의")
@TestPropertySource("classpath:application.properties")
internal class RedisDeadLetterManagerTest(
    private val sagaManager: SagaManager,
    private val errorInjector: ErrorInjector,
    private val relayResultHolder: RelayResultHolder,
    private val deadLetterRelay: DeadLetterRelay,
    private val deadLetterRegistry: DeadLetterRegistry,
    @Qualifier("relay1Depths") private val relay1Depths: Orchestrator<RelayEvent, RelayEvent>,
    @Qualifier("relay2Depths") private val relay2Depths: Orchestrator<RelayEvent, RelayEvent>,
) : DescribeSpec({

    beforeEach {
        errorInjector.doError = true
        relayResultHolder.clear()
    }

    describe("relay1Depths 구현채는") {
        context("rollback중 예외가 발생시, relay를 호출하면") {
            val relayEvent = RelayEvent(1, "1", listOf("1", "1"), RelayEvent.Depth(true))

            var isSuccessToAddDeadLetter = false
            deadLetterRegistry.addListener { _, _ ->
                isSuccessToAddDeadLetter = true
            }

            it("실패한 곳 부터 재시도 한다") {
                relay1Depths.sagaSync(relayEvent)

                eventually(5.seconds) {
                    isSuccessToAddDeadLetter.shouldBeTrue()
                }

                errorInjector.doError = false

                deadLetterRelay.relaySync()

                eventually(5.seconds) {
                    relayResultHolder["relay1Depths"] shouldBe listOf(relayEvent)
                }
            }
        }
    }

    describe("relay2Depths 구현채는") {
        context("rollback중 예외가 발생시, relay를 호출하면") {
            val relayEvent = RelayEvent(2, "2", listOf("2", "2", "2"), RelayEvent.Depth(true))

            var isSuccessToAddDeadLetter = false
            deadLetterRegistry.addListener { _, _ ->
                isSuccessToAddDeadLetter = true
            }

            it("실패한 곳 부터 재시도 한다") {
                relay2Depths.sagaSync(relayEvent)

                eventually(5.seconds) {
                    isSuccessToAddDeadLetter.shouldBeTrue()
                }

                errorInjector.doError = false
                deadLetterRelay.relaySync()

                eventually(5.seconds) {
                    relayResultHolder["relay2Depths"] shouldBe listOf(relayEvent, relayEvent)
                }
            }
        }
    }

    describe("DeadLetterAnnotationClass 구현채는") {
        context("SagaRollbackListener에서 에러가 발생시 relay를 호출하면") {
            val relayEvent = RelayEvent(3, "3", listOf("3", "3"), RelayEvent.Depth(true))

            var isSuccessToAddDeadLetter = false
            deadLetterRegistry.addListener { _, _ ->
                isSuccessToAddDeadLetter = true
            }

            it("실패한 RollbackListener를 재시도한다") {
                sagaManager.startSync(relayEvent)

                eventually(5.seconds) {
                    isSuccessToAddDeadLetter.shouldBeTrue()
                }

                errorInjector.doError = false
                deadLetterRelay.relaySync()

                eventually(5.seconds) {
                    relayResultHolder["DeadLetterAnnotationClass"] shouldBe listOf(relayEvent)
                }
            }
        }
    }

    describe("relay(deadLetterId: String) 메소드는") {
        context("특정 deadLetterId를 입력받으면,") {
            val relay1Event = RelayEvent(1, "1", listOf("1"), RelayEvent.Depth(false))
            val relay2Event = RelayEvent(2, "2", listOf("2"), RelayEvent.Depth(false))

            val deadLetterIds: MutableMap<Long, String> = mutableMapOf()
            deadLetterRegistry.addListener { deadLetterId, sagaEvent ->
                val relayEvent = sagaEvent.decodeOrchestrateEvent(RelayEvent::class)
                deadLetterIds[relayEvent.id] = deadLetterId
            }

            it("해당하는 deadLetter 를 retry한다.") {
                relay1Depths.sagaSync(relay1Event)
                relay2Depths.sagaSync(relay2Event)

                eventually(5.seconds) {
                    deadLetterIds.size shouldBe 2
                }

                errorInjector.doError = false
                deadLetterRelay.relaySync(deadLetterIds[1]!!)

                eventually(5.seconds) {
                    relayResultHolder["relay1Depths"] shouldBe listOf(relay1Event)
                    relayResultHolder["relay2Depths"] shouldBe null
                }
            }
        }
    }
})
