package org.rooftop.netx.redis

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.StringSpec
import org.rooftop.netx.api.SagaManager
import org.rooftop.netx.meta.EnableSaga
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import kotlin.time.Duration.Companion.seconds

@EnableSaga
@ContextConfiguration(
    classes = [
        RedisContainer::class,
        SagaNoRollbackForStorage::class,
        MonoSagaHandlerAssertions::class,
    ]
)
@DisplayName("RedisStreamSagaDispatcherNoRollbackForTest")
@TestPropertySource("classpath:application.properties")
internal class RedisStreamSagaDispatcherNoRollbackForTest(
    private val sagaAssertions: MonoSagaHandlerAssertions,
    private val sagaManager: SagaManager,
) : StringSpec({

    beforeEach {
        sagaAssertions.clear()
    }

    "noRollbackFor로 IllegalArgumentException이 걸려있으면, 해당 예외가 발생해도 rollback 하지 않는다." {
        sagaManager.startSync(IllegalArgumentExceptionEvent("illegal"))

        eventually(5.seconds) {
            sagaAssertions.startCountShouldBe(1)
            sagaAssertions.rollbackCountShouldBe(0)
        }
    }

    "noRollbackFor로 UnSupportedOperationException이 걸려있으면, 해당 예외가 발생해도 rollback 하지않는다." {
        sagaManager.startSync(UnSupportedOperationExceptionEvent("unsupports"))

        eventually(5.seconds) {
            sagaAssertions.startCountShouldBe(1)
            sagaAssertions.rollbackCountShouldBe(0)
        }
    }

    "noRollbackFor에 설정되지 않은 예외가 발생하면, rollback을 수행한다." {
        sagaManager.startSync(NoSuchElementExceptionEvent("noSuchElement"))

        eventually(5.seconds) {
            sagaAssertions.startCountShouldBe(1)
            sagaAssertions.rollbackCountShouldBe(1)
        }
    }
}) {

    class IllegalArgumentExceptionEvent(val illegalArgumentException: String)
    class UnSupportedOperationExceptionEvent(val unsupportedOperationException: String)
    class NoSuchElementExceptionEvent(val noSuchElementException: String)
}
