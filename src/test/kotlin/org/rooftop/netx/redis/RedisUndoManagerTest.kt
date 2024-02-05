package org.rooftop.netx.redis

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.rooftop.netx.engine.UndoManager
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import reactor.test.StepVerifier

@AutoConfigureRedisUndo
@ContextConfiguration(classes = [RedisContainer::class])
@TestPropertySource("classpath:application.properties")
internal class RedisUndoManagerTest(
    private val undoManager: UndoManager,
) : DescribeSpec({

    describe("find 메소드는") {
        context("transactionId를 받으면,") {

            val transactionId = "TX-1"
            val undoState = "id:1"
            undoManager.save(transactionId, undoState).block()

            it("저장된 UndoState를 반환한다.") {
                val result = undoManager.find(transactionId)

                StepVerifier.create(result)
                    .assertNext {
                        it shouldBe undoState
                    }
                    .verifyComplete()
            }
        }

        context("존재하지 않는 transactionId를 받으면,") {
            val transactionId = "UNKNOWN_TX"

            it("IllegalStateException 을 던진다.") {
                val result = undoManager.find(transactionId)

                StepVerifier.create(result)
                    .verifyErrorMessage("Cannot find undo state \"netx-group:UNKNOWN_TX\"")
            }
        }
    }

    describe("delete 메소드는") {
        context("transactionId를 받으면,") {

            val transactionId = "TX-2"
            val undoState = "id:2"
            undoManager.save(transactionId, undoState).block()

            it("undoState 를 삭제하고 true를 반환한다.") {
                val result = undoManager.delete(transactionId)

                StepVerifier.create(result)
                    .expectNext(true)
                    .verifyComplete()
            }
        }

        context("존재하지 않는 transactionId를 받으면") {

            val transactionId = "UNKNOWN_TX"

            it("false 를 반환한다.") {
                val result = undoManager.delete(transactionId)

                StepVerifier.create(result)
                    .expectNext(false)
                    .verifyComplete()
            }
        }
    }
})
