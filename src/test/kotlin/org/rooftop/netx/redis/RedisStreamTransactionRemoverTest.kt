package org.rooftop.netx.redis

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import org.rooftop.netx.api.TransactionManager
import org.rooftop.netx.meta.EnableDistributedTransaction
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import reactor.core.scheduler.Schedulers
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@EnableDistributedTransaction
@ContextConfiguration(
    classes = [
        RedisContainer::class,
        RedisAssertions::class,
        TransactionHandlerAssertions::class,
    ]
)
@TestPropertySource("classpath:application.properties")
@DisplayName("RedisStreamTransactionRemover 클래스의")
internal class RedisStreamTransactionRemoverTest(
    private val redisAssertions: RedisAssertions,
    private val transactionManager: TransactionManager,
    private val transactionHandlerAssertions: TransactionHandlerAssertions,
) : DescribeSpec({

    beforeEach {
        transactionHandlerAssertions.clear()
    }

    describe("handleTransactionCommitEvent 메소드는") {
        context("TransactionCommitEvent 가 발행되면,") {
            val transactionId = transactionManager.start("RedisStreamTransactionRemoverTest")
                .block()!!

            it("Transaction 을 retry watch 대기열에서 삭제한다.") {
                transactionManager.commit(transactionId)
                    .subscribeOn(Schedulers.parallel())
                    .subscribe()

                eventually(5.minutes) {
                    transactionHandlerAssertions.commitCountShouldBe(1)
                    redisAssertions.retryTransactionShouldBeNotExists(transactionId)
                }
            }
        }

        context("TransactionRollbackEvent 가 발행되면,") {
            val transactionId = transactionManager.start("RedisStreamTransactionRemoverTest")
                .block()!!

            it("Transaction 을 retry watch 대기열에서 삭제한다.") {
                transactionManager.rollback(transactionId, "rollback occured for test").block()

                eventually(5.minutes) {
                    transactionHandlerAssertions.rollbackCountShouldBe(1)
                    redisAssertions.retryTransactionShouldBeNotExists(transactionId)
                }
            }
        }
    }
}
)
