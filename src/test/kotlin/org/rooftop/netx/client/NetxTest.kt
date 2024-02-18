package org.rooftop.netx.client

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import org.rooftop.netx.meta.EnableDistributedTransaction
import org.rooftop.netx.redis.RedisContainer
import org.springframework.boot.test.context.SpringBootTest
import reactor.core.publisher.Hooks
import kotlin.time.Duration.Companion.minutes

@DisplayName("Netx 테스트의")
@SpringBootTest(
    classes = [
        RedisContainer::class,
        LoadRunner::class,
        NetxClient::class,
        TransactionReceiveStorage::class,
    ]
)
@EnableDistributedTransaction
internal class NetxTest(
    private val netxClient: NetxClient,
    private val loadRunner: LoadRunner,
    private val transactionReceiveStorage: TransactionReceiveStorage,
) : FunSpec({

    test("Netx는 부하가 가중되어도, 결과적 일관성을 보장한다.") {
        forAll(
            row(1, 1),
            row(10, 10),
            row(100, 100),
            row(1_000, 1_000),
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
                transactionReceiveStorage.startCountShouldBe(commitLoadCount + rollbackLoadCount)
                transactionReceiveStorage.joinCountShouldBe(commitLoadCount + rollbackLoadCount)
                transactionReceiveStorage.commitCountShouldBe(commitLoadCount)
                transactionReceiveStorage.rollbackCountShouldBe(rollbackLoadCount)
            }
        }
    }
})
