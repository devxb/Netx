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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.minutes

@DisplayName("Netx 부하테스트")
@SpringBootTest(
    classes = [
        RedisContainer::class,
        LoadRunner::class,
        NetxClient::class,
        TransactionReceiveStorage::class,
        OrchestratorConfigurer::class,
    ]
)
@EnableDistributedTransaction
internal class NetxLoadTest(
    private val netxClient: NetxClient,
    private val loadRunner: LoadRunner,
    private val transactionReceiveStorage: TransactionReceiveStorage,
    private val sum3Orchestrator: Orchestrator<Int>,
) : FunSpec({

    test("Netx의 Orchestrator는 부하가 가중되어도, 결과적 일관성을 보장한다.") {
        forAll(
            row(1),
            row(10),
            row(100),
            row(1_000),
        ) { count ->
            val set = ConcurrentHashMap.newKeySet<Int>();

            val i = AtomicInteger(0)
            loadRunner.load(count) {
                val result = sum3Orchestrator.transactionSync(i.getAndIncrement())
                set.add(result.decodeResult(Int::class))
            }

            eventually(30.minutes) {
                set.size shouldBeEqual count
            }
        }
    }

    test("Netx는 부하가 가중되어도, 결과적 일관성을 보장한다.") {
        forAll(
            row(1, 1),
            row(10, 10),
            row(100, 100),
            row(1_000, 1_000),
            row(10_000, 10_000),
        ) { commitLoadCount, rollbackLoadCount ->
            transactionReceiveStorage.clear()

            loadRunner.load(commitLoadCount) {
                val transactionId = netxClient.startTransaction("")
                netxClient.joinTransaction(transactionId, "")
                netxClient.commitTransaction(transactionId)
            }

            loadRunner.load(rollbackLoadCount) {
                val transactionId = netxClient.startTransaction("")
                netxClient.joinTransaction(transactionId, "")
                netxClient.rollbackTransaction(transactionId, "")
            }

            eventually(30.minutes) {
                transactionReceiveStorage.startCountShouldBeGreaterThanOrEqual(commitLoadCount + rollbackLoadCount)
                transactionReceiveStorage.joinCountShouldBeGreaterThanOrEqual(commitLoadCount + rollbackLoadCount)
                transactionReceiveStorage.commitCountShouldBeGreaterThanOrEqual(commitLoadCount)
                transactionReceiveStorage.rollbackCountShouldBeGreaterThanOrEqual(rollbackLoadCount)
            }
        }
    }

})
