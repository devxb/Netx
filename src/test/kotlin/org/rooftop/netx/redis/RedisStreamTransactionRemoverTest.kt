package org.rooftop.netx.redis

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.rooftop.netx.api.TransactionCommitEvent
import org.rooftop.netx.api.TransactionManager
import org.rooftop.netx.api.TransactionRollbackEvent
import org.rooftop.netx.autoconfig.EnableDistributedTransaction
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import reactor.core.scheduler.Schedulers
import kotlin.time.Duration.Companion.seconds

@EnableDistributedTransaction
@ContextConfiguration(
    classes = [
        RedisContainer::class,
        RedisAssertions::class,
        EventCapture::class,
    ]
)
@TestPropertySource("classpath:application.properties")
@DisplayName("RedisStreamTransactionRemover 클래스의")
internal class RedisStreamTransactionRemoverTest(
    private val redisAssertions: RedisAssertions,
    private val transactionManager: TransactionManager,
    private val eventCapture: EventCapture,
) : DescribeSpec({
    describe("handleTransactionCommitEvent 메소드는") {
        context("TransactionCommitEvent 가 발행되면,") {
            val transactionId = transactionManager.start("RedisStreamTransactionRemoverTest")
                .block()!!

            it("Transaction 을 retry watch 대기열에서 삭제한다.") {
                transactionManager.commit(transactionId)
                    .subscribeOn(Schedulers.parallel())
                    .subscribe()

                eventually(10.seconds) {
                    eventCapture.capturedCount(TransactionCommitEvent::class) shouldBe 1
                    redisAssertions.retryTransactionShouldBeNotExists(transactionId)
                }
            }
        }

        context("TransactionRollbackEvent 가 발행되면,") {
            val transactionId = transactionManager.start("RedisStreamTransactionRemoverTest")
                .block()!!

            it("Transaction 을 retry watch 대기열에서 삭제한다.") {
                transactionManager.rollback(transactionId, "rollback occured for test").block()

                eventually(10.seconds) {
                    eventCapture.capturedCount(TransactionRollbackEvent::class) shouldBe 1
                    redisAssertions.retryTransactionShouldBeNotExists(transactionId)
                }
            }
        }
    }
}
)
