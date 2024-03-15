package org.rooftop.netx.redis

import com.fasterxml.jackson.databind.ObjectMapper
import org.rooftop.netx.api.Codec
import org.rooftop.netx.api.OrchestrateResult
import org.rooftop.netx.api.ResultTimeoutException
import org.rooftop.netx.engine.OrchestrateResultHolder
import org.rooftop.netx.engine.core.Transaction
import org.rooftop.netx.engine.core.TransactionState
import org.rooftop.netx.engine.logging.info
import org.springframework.data.redis.core.ReactiveRedisTemplate
import reactor.core.publisher.Mono
import reactor.pool.PoolBuilder
import java.util.concurrent.TimeoutException
import kotlin.time.Duration
import kotlin.time.toJavaDuration


class RedisOrchestrateResultHolder(
    private val codec: Codec,
    private val serverId: String,
    private val group: String,
    private val objectMapper: ObjectMapper,
    private val reactiveRedisTemplate: ReactiveRedisTemplate<String, Transaction>,
) : OrchestrateResultHolder {

    private val pool = PoolBuilder.from(Mono.just(reactiveRedisTemplate.opsForList()))
        .sizeBetween(1, 10)
        .maxPendingAcquireUnbounded()
        .buildPool()

    override fun getResult(timeout: Duration, transactionId: String): Mono<OrchestrateResult> {
        return pool.withPoolable {
            it.leftPop("Result:$transactionId", timeout.toJavaDuration())
                .switchIfEmpty(Mono.error {
                    ResultTimeoutException(
                        "Cannot get result in \"$timeout\" time",
                        TimeoutException()
                    )
                })
        }.single().map {
            OrchestrateResult(
                isSuccess = it.state == TransactionState.COMMIT,
                codec = codec,
                result = it.event
                    ?: throw NullPointerException("OrchestrateResult message cannot be null")
            )
        }.doOnNext { info("Get result $it") }
    }

    override fun <T : Any> setResult(
        transactionId: String,
        state: TransactionState,
        result: T
    ): Mono<T> {
        return reactiveRedisTemplate.opsForList()
            .leftPush(
                "Result:$transactionId", Transaction(
                    id = transactionId,
                    serverId = serverId,
                    group = group,
                    state = state,
                    event = objectMapper.writeValueAsString(result)
                )
            ).map { result }
            .doOnNext { info("Set result $it") }
    }
}
