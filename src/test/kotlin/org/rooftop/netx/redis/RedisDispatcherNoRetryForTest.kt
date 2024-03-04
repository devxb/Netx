package org.rooftop.netx.redis

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.StringSpec
import org.rooftop.netx.api.TransactionManager
import org.rooftop.netx.meta.EnableDistributedTransaction
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import kotlin.time.Duration.Companion.seconds

@EnableDistributedTransaction
@ContextConfiguration(
    classes = [
        RedisContainer::class,
        RedisAssertions::class,
        TransactionNoRetryStorage::class,
    ]
)
@DisplayName("RedisDispatcherNoRetryForTest")
@TestPropertySource("classpath:application.properties")
class RedisDispatcherNoRetryForTest(
    private val redisAssertions: RedisAssertions,
    private val transactionManager: TransactionManager,
) : StringSpec({

    "noForRetry로 IllegalArgumentException이 걸려있으면, 해당 예외가 발생해도 ack를 한다." {
        transactionManager.syncStart(UNDO, IllegalArgumentExceptionEvent("illegal"))

        eventually(5.seconds) {
            redisAssertions.pendingMessageCountShouldBe(0)
        }
    }

    "noForRetry로 UnSupportedOperationException이 걸려있으면, 해당 예외가 발생해도 ack를 진행한다." {
        transactionManager.syncStart(UNDO, UnSupportedOperationExceptionEvent("unsupports"))

        eventually(5.seconds) {
            redisAssertions.pendingMessageCountShouldBe(0)
        }
    }

    "noForRetry에 설정되지 않은 예외가 발생하면, ack를 할 수 없다." {
        transactionManager.syncStart(UNDO, NoSuchElementExceptionEvent("noSuchElement"))

        eventually(5.seconds) {
            redisAssertions.pendingMessageCountShouldBe(1)
        }
    }
}) {

    class IllegalArgumentExceptionEvent(val illegalArgumentException: String)
    class UnSupportedOperationExceptionEvent(val unsupportedOperationException: String)
    class NoSuchElementExceptionEvent(val noSuchElementException: String)

    private companion object {
        private const val UNDO = "UNDO"
    }
}
