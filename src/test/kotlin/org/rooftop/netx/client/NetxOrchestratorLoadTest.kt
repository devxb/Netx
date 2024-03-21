package org.rooftop.netx.client

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.equals.shouldBeEqual
import org.rooftop.netx.api.Orchestrator
import org.rooftop.netx.meta.EnableDistributedTransaction
import org.rooftop.netx.redis.RedisContainer
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.minutes

@DisplayName("Netx orchestartor 부하테스트")
@SpringBootTest(
    classes = [
        RedisContainer::class,
        LoadRunner::class,
        OrchestratorConfigurer::class,
    ]
)
@EnableDistributedTransaction
@TestPropertySource("classpath:fast-recover-mode.properties")
class NetxOrchestratorLoadTest(
    private val loadRunner: LoadRunner,
    private val orchestrator: Orchestrator<Int, Int>,
) : FunSpec({

    test("Netx의 Orcehstrator는 부하가 가중되어도, 결과적 일관성을 보장한다.") {
        forAll(
            row(1),
            row(10),
            row(100),
            row(1_000),
            row(10_000),
        ) { count ->
            val resultStorage = ConcurrentHashMap.newKeySet<Int>(count)

            val atomicInt = AtomicInteger(0)
            loadRunner.load(count) {
                orchestrator.transaction(THREE_MINUTES_MILLIS, atomicInt.getAndIncrement())
                    .map { resultStorage.add(it.decodeResult(Int::class)) }
                    .subscribe()
            }

            eventually(10.minutes) {
                resultStorage.size shouldBeEqual count
            }
        }
    }
}) {
    private companion object {
        private const val THREE_MINUTES_MILLIS = 1000 * 60 * 3L
    }
}
