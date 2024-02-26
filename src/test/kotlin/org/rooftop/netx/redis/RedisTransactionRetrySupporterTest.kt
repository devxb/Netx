package org.rooftop.netx.redis

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import org.rooftop.netx.api.TransactionManager
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@ContextConfiguration(
    classes = [
        RedisContainer::class,
        RedisAssertions::class,
        NoAckRedisTransactionConfigurer::class,
        MonoTransactionHandlerAssertions::class,
        NoPublisherTransactionHandlerAssertions::class,
    ]
)
@TestPropertySource("classpath:application.properties")
@DisplayName("RedisTransactionRetrySupporter 클래스의")
internal class RedisTransactionRetrySupporterTest(
    private val redisAssertions: RedisAssertions,
    private val transactionManager: TransactionManager,
    private val monoTransactionHandlerAssertions: MonoTransactionHandlerAssertions,
    private val noPublisherTransactionHandlerAssertions: NoPublisherTransactionHandlerAssertions,
) : DescribeSpec({

    beforeEach {
        monoTransactionHandlerAssertions.clear()
        noPublisherTransactionHandlerAssertions.clear()
    }

    describe("handleOrphanTransaction 메소드는") {
        context("pending되었지만, ack되지 않은 트랜잭션이 있다면,") {
            it("해당 트랜잭션을 찾아서 처리하고, ack 상태로 변경한다.") {
                val transactionId = transactionManager.start("undo").block()!!

                eventually(1.minutes) {
                    noPublisherTransactionHandlerAssertions.startCountShouldBe(1)
                    monoTransactionHandlerAssertions.startCountShouldBe(1)

                    redisAssertions.pendingMessageCountShouldBe(transactionId, 0)
                }
            }
        }
    }
})
