package org.rooftop.netx.redis

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.throwables.shouldThrowMessage
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.rooftop.netx.api.*
import org.rooftop.netx.meta.EnableSaga
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import reactor.test.StepVerifier
import kotlin.time.Duration.Companion.seconds

@EnableSaga
@ContextConfiguration(
    classes = [
        RedisContainer::class,
        MonoSagaHandlerAssertions::class,
        NoPublisherSagaHandlerAssertions::class,
    ]
)
@DisplayName("RedisStreamSagaManager 클래스의")
@TestPropertySource("classpath:application.properties")
internal class RedisStreamSagaManagerTest(
    private val sagaManager: SagaManager,
    private val monoSagaHandlerAssertions: MonoSagaHandlerAssertions,
    private val noPublisherSagaHandlerAssertions: NoPublisherSagaHandlerAssertions,
) : DescribeSpec({

    beforeEach {
        monoSagaHandlerAssertions.clear()
        noPublisherSagaHandlerAssertions.clear()
    }

    describe("start 메소드는") {
        context("어떤 event도 없이 호출되면,") {
            it("Saga 를 시작하고 saga-id를 반환한다.") {
                sagaManager.start().subscribe()

                eventually(5.seconds) {
                    monoSagaHandlerAssertions.startCountShouldBe(1)
                    noPublisherSagaHandlerAssertions.startCountShouldBe(1)
                }
            }
        }

        context("서로 다른 id의 사가가 여러번 시작되어도") {
            it("모두 읽을 수 있다.") {
                sagaManager.start().block()
                sagaManager.start().block()

                eventually(5.seconds) {
                    monoSagaHandlerAssertions.startCountShouldBe(2)
                    noPublisherSagaHandlerAssertions.startCountShouldBe(2)
                }
            }
        }
    }

    describe("syncStart 메소드는") {
        context("어떤 event도 없이 호출되면,") {
            it("Saga 를 시작하고 saga-id를 반환한다.") {
                sagaManager.syncStart()

                eventually(5.seconds) {
                    monoSagaHandlerAssertions.startCountShouldBe(1)
                    noPublisherSagaHandlerAssertions.startCountShouldBe(1)
                }
            }
        }

        context("서로 다른 id의 사가가 여러번 시작되어도") {
            it("모두 읽을 수 있다.") {
                sagaManager.syncStart()
                sagaManager.syncStart()

                eventually(5.seconds) {
                    monoSagaHandlerAssertions.startCountShouldBe(2)
                    noPublisherSagaHandlerAssertions.startCountShouldBe(2)
                }
            }
        }
    }

    describe("join 메소드는") {
        context("존재하는 sagaId를 입력받으면,") {
            val sagaId = sagaManager.start().block()!!

            it("Saga 에 참여한다.") {
                sagaManager.join(sagaId).subscribe()

                eventually(5.seconds) {
                    monoSagaHandlerAssertions.joinCountShouldBe(1)
                    noPublisherSagaHandlerAssertions.joinCountShouldBe(1)
                }
            }
        }

        context("존재하지 않는 sagaId를 입력받으면,") {
            it("SagaException 을 던진다.") {
                val result = sagaManager.join(NOT_EXIST_TX_ID)

                StepVerifier.create(result)
                    .verifyErrorMessage("Cannot find exists saga by id \"$NOT_EXIST_TX_ID\"")
            }
        }
    }

    describe("syncJoin 메소드는") {
        context("존재하는 sagaId를 입력받으면,") {
            val sagaId = sagaManager.syncStart()

            it("Saga 에 참여한다.") {
                sagaManager.syncJoin(sagaId)

                eventually(5.seconds) {
                    monoSagaHandlerAssertions.joinCountShouldBe(1)
                    noPublisherSagaHandlerAssertions.joinCountShouldBe(1)
                }
            }
        }

        context("존재하지 않는 sagaId를 입력받으면,") {
            it("SagaException 을 던진다.") {
                shouldThrowMessage("Cannot find exists saga by id \"$NOT_EXIST_TX_ID\"") {
                    sagaManager.syncJoin(NOT_EXIST_TX_ID)
                }
            }
        }
    }

    describe("exists 메소드는") {
        context("존재하는 sagaId를 입력받으면,") {
            val sagaId = sagaManager.start().block()!!

            it("saga id를 반환한다.") {
                val result = sagaManager.exists(sagaId)

                StepVerifier.create(result)
                    .expectNext(sagaId)
                    .verifyComplete()
            }
        }

        context("존재하지 않는 sagaId를 입력받으면,") {
            it("SagaException 을 던진다.") {
                val result = sagaManager.exists(NOT_EXIST_TX_ID)

                StepVerifier.create(result)
                    .verifyErrorMessage("Cannot find exists saga by id \"$NOT_EXIST_TX_ID\"")
            }
        }
    }

    describe("syncExists 메소드는") {
        context("존재하는 sagaId를 입력받으면,") {
            val sagaId = sagaManager.syncStart()

            it("saga id를 반환한다.") {
                val result = sagaManager.syncExists(sagaId)

                result shouldBe sagaId
            }
        }

        context("존재하지 않는 sagaId를 입력받으면,") {
            it("SagaException 을 던진다.") {
                shouldThrowMessage("Cannot find exists saga by id \"$NOT_EXIST_TX_ID\"") {
                    sagaManager.syncExists(NOT_EXIST_TX_ID)
                }
            }
        }
    }

    describe("commit 메소드는") {
        context("존재하는 sagaId를 입력받으면,") {
            val sagaId = sagaManager.start().block()!!

            it("commit 메시지를 publish 한다") {
                sagaManager.commit(sagaId).block()

                eventually(5.seconds) {
                    monoSagaHandlerAssertions.commitCountShouldBe(1)
                    noPublisherSagaHandlerAssertions.commitCountShouldBe(1)
                }
            }
        }

        context("존재하지 않는 sagaId를 입력받으면,") {
            it("SagaException 을 던진다.") {
                val result = sagaManager.commit(NOT_EXIST_TX_ID)

                StepVerifier.create(result)
                    .verifyErrorMessage("Cannot find exists saga by id \"$NOT_EXIST_TX_ID\"")
            }
        }
    }

    describe("syncCommit 메소드는") {
        context("존재하는 sagaId를 입력받으면,") {
            val sagaId = sagaManager.syncStart()

            it("commit 메시지를 publish 한다") {
                sagaManager.syncCommit(sagaId)

                eventually(5.seconds) {
                    monoSagaHandlerAssertions.commitCountShouldBe(1)
                    noPublisherSagaHandlerAssertions.commitCountShouldBe(1)
                }
            }
        }

        context("존재하지 않는 sagaId를 입력받으면,") {
            it("SagaException 을 던진다.") {
                shouldThrowMessage("Cannot find exists saga by id \"$NOT_EXIST_TX_ID\"") {
                    sagaManager.syncCommit(NOT_EXIST_TX_ID)
                }
            }
        }
    }

    describe("rollback 메소드는") {
        context("존재하는 sagaId를 입력받으면,") {
            val sagaId = sagaManager.start().block()!!

            it("rollback 메시지를 publish 한다") {
                sagaManager.rollback(sagaId, "rollback for test").block()

                eventually(5.seconds) {
                    monoSagaHandlerAssertions.rollbackCountShouldBe(1)
                    noPublisherSagaHandlerAssertions.rollbackCountShouldBe(1)
                }
            }
        }

        context("존재하지 않는 sagaId를 입력받으면,") {
            it("SagaException 을 던진다.") {
                val result = sagaManager.rollback(NOT_EXIST_TX_ID, "rollback for test")

                StepVerifier.create(result)
                    .verifyErrorMessage("Cannot find exists saga by id \"$NOT_EXIST_TX_ID\"")
            }
        }
    }

    describe("syncRollback 메소드는") {
        context("존재하는 sagaId를 입력받으면,") {
            val sagaId = sagaManager.syncStart()

            it("rollback 메시지를 publish 한다") {
                sagaManager.syncRollback(sagaId, "rollback for test")

                eventually(5.seconds) {
                    monoSagaHandlerAssertions.rollbackCountShouldBe(1)
                    noPublisherSagaHandlerAssertions.rollbackCountShouldBe(1)
                }
            }
        }

        context("존재하지 않는 sagaId를 입력받으면,") {
            it("SagaException 을 던진다.") {
                shouldThrowMessage("Cannot find exists saga by id \"$NOT_EXIST_TX_ID\"") {
                    sagaManager.syncRollback(NOT_EXIST_TX_ID, "rollback for test")
                }
            }
        }
    }
}) {

    private companion object {
        private const val NOT_EXIST_TX_ID = "NOT_EXISTS_TX_ID"
    }
}
