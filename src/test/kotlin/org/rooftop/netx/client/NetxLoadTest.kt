package org.rooftop.netx.client

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import org.rooftop.netx.api.SagaManager
import org.rooftop.netx.meta.EnableSaga
import org.rooftop.netx.redis.RedisContainer
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import kotlin.time.Duration.Companion.minutes

@DisplayName("Netx LoadTest")
@SpringBootTest(
    classes = [
        RedisContainer::class,
        LoadRunner::class,
        OrchestratorConfigurer::class,
        SagaReceiveStorage::class,
    ]
)
@EnableSaga
@TestPropertySource("classpath:fast-recover-mode.properties")
internal class NetxLoadTest(
    private val sagaManager: SagaManager,
    private val loadRunner: LoadRunner,
    private val sagaReceiveStorage: SagaReceiveStorage,
) : FunSpec({

    test("with 77,777 transactions.") {
        forAll(
            row(1, 1),
            row(10, 10),
            row(100, 100),
            row(1_000, 1_000),
            row(10_000, 10_000),
        ) { commitLoadCount, rollbackLoadCount ->
            sagaReceiveStorage.clear()

            loadRunner.load(commitLoadCount) {
                sagaManager.start(LoadTestEvent(NO_ROLLBACK)).block()!!
            }

            loadRunner.load(rollbackLoadCount) {
                sagaManager.start(LoadTestEvent(ROLLBACK)).block()!!
            }

            eventually(3.minutes) {
                sagaReceiveStorage.startCountShouldBeGreaterThanOrEqual(commitLoadCount + rollbackLoadCount)
                sagaReceiveStorage.joinCountShouldBeGreaterThanOrEqual(commitLoadCount + rollbackLoadCount)
                sagaReceiveStorage.commitCountShouldBeGreaterThanOrEqual(commitLoadCount + rollbackLoadCount)
                sagaReceiveStorage.rollbackCountShouldBeGreaterThanOrEqual(rollbackLoadCount)
            }
        }
    }

}) {
    data class LoadTestEvent(val load: String)

    private companion object {
        private const val ROLLBACK = "-"
        private const val NO_ROLLBACK = "+"
    }
}
