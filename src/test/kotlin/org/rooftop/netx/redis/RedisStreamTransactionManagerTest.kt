package org.rooftop.netx.redis

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import org.rooftop.netx.api.*
import org.rooftop.netx.meta.EnableDistributedTransaction
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import reactor.test.StepVerifier
import kotlin.time.Duration.Companion.minutes

@EnableDistributedTransaction
@ContextConfiguration(
    classes = [
        RedisContainer::class,
        TransactionHandlerAssertions::class,
    ]
)
@DisplayName("RedisStreamTransactionManager 클래스의")
@TestPropertySource("classpath:application.properties")
internal class RedisStreamTransactionManagerTest(
    private val transactionManager: TransactionManager,
    private val transactionHandlerAssertions: TransactionHandlerAssertions,
) : DescribeSpec({

    beforeEach {
        transactionHandlerAssertions.clear()
    }

    describe("start 메소드는") {
        context("replay 를 입력받으면,") {
            it("트랜잭션을 시작하고 transaction-id를 반환한다.") {
                transactionManager.start(REPLAY).subscribe()

                eventually(5.minutes) {
                    transactionHandlerAssertions.startCountShouldBe(1)
                }
            }
        }

        context("서로 다른 id의 트랜잭션이 여러번 시작되어도") {
            it("모두 읽을 수 있다.") {
                transactionManager.start(REPLAY).block()
                transactionManager.start(REPLAY).block()

                eventually(5.minutes) {
                    transactionHandlerAssertions.startCountShouldBe(2)
                }
            }
        }
    }

    describe("join 메소드는") {
        context("존재하는 transactionId를 입력받으면,") {
            val transactionId = transactionManager.start(REPLAY).block()!!

            it("트랜잭션에 참여한다.") {
                transactionManager.join(transactionId, REPLAY).subscribe()

                eventually(5.minutes) {
                    transactionHandlerAssertions.joinCountShouldBe(1)
                }
            }
        }

        context("존재하지 않는 transactionId를 입력받으면,") {
            it("IllegalStateException 을 던진다.") {
                val result = transactionManager.join(NOT_EXIST_TX_ID, REPLAY)

                StepVerifier.create(result)
                    .verifyErrorMessage("Cannot find exists transaction id \"$NOT_EXIST_TX_ID\"")
            }
        }
    }

    describe("exists 메소드는") {
        context("존재하는 transactionId를 입력받으면,") {
            val transactionId = transactionManager.start(REPLAY).block()!!

            it("트랜잭션 id를 반환한다.") {
                val result = transactionManager.exists(transactionId)

                StepVerifier.create(result)
                    .expectNext(transactionId)
                    .verifyComplete()
            }
        }

        context("존재하지 않는 transactionId를 입력받으면,") {
            it("IllegalStateException 을 던진다.") {
                val result = transactionManager.exists(NOT_EXIST_TX_ID)

                StepVerifier.create(result)
                    .verifyErrorMessage("Cannot find exists transaction id \"$NOT_EXIST_TX_ID\"")
            }
        }
    }

    describe("commit 메소드는") {
        context("존재하는 transactionId를 입력받으면,") {
            val transactionId = transactionManager.start(REPLAY).block()!!

            it("commit 메시지를 publish 한다") {
                transactionManager.commit(transactionId).block()

                eventually(5.minutes) {
                    transactionHandlerAssertions.commitCountShouldBe(1)
                }
            }
        }

        context("존재하지 않는 transactionId를 입력받으면,") {
            it("IllegalStateException 을 던진다.") {
                val result = transactionManager.commit(NOT_EXIST_TX_ID)

                StepVerifier.create(result)
                    .verifyErrorMessage("Cannot find exists transaction id \"$NOT_EXIST_TX_ID\"")
            }
        }
    }

    describe("rollback 메소드는") {
        context("존재하는 transactionId를 입력받으면,") {
            val transactionId = transactionManager.start(REPLAY).block()!!

            it("rollback 메시지를 publish 한다") {
                transactionManager.rollback(transactionId, "rollback occured for test").block()

                eventually(5.minutes) {
                    transactionHandlerAssertions.rollbackCountShouldBe(1)
                }
            }
        }

        context("존재하지 않는 transactionId를 입력받으면,") {
            it("IllegalStateException 을 던진다.") {
                val result = transactionManager.commit(NOT_EXIST_TX_ID)

                StepVerifier.create(result)
                    .verifyErrorMessage("Cannot find exists transaction id \"$NOT_EXIST_TX_ID\"")
            }
        }
    }
}) {

    private companion object {
        private const val REPLAY = "REPLAY"
        private const val NOT_EXIST_TX_ID = "NOT_EXISTS_TX_ID"
    }
}
