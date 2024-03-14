package org.rooftop.netx.redis

import com.fasterxml.jackson.databind.ObjectMapper
import org.rooftop.netx.api.Codec
import org.rooftop.netx.api.OrchestrateResult
import org.rooftop.netx.api.ResultTimeoutException
import org.rooftop.netx.engine.OrchestrateResultHolder
import org.rooftop.netx.engine.core.Transaction
import org.rooftop.netx.engine.core.TransactionState
import org.springframework.data.redis.core.ReactiveRedisTemplate
import reactor.core.publisher.Mono
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

    override fun getResult(timeout: Duration, transactionId: String): Mono<OrchestrateResult> {
        return reactiveRedisTemplate.listenToChannel(CHANNEL)
            .timeout(timeout.toJavaDuration())
            .onErrorMap {
                if (it::class == TimeoutException::class) {
                    return@onErrorMap ResultTimeoutException(
                        "Can't get result in \"$timeout\" time",
                        it,
                    )
                }
                it
            }
            .filter { it.message.id == transactionId }
            .map {
                OrchestrateResult(
                    isSuccess = it.message.state == TransactionState.COMMIT,
                    codec = codec,
                    result = it.message.event
                        ?: throw NullPointerException("OrchestrateResult message cannot be null")
                )
            }
            .next()
    }

    override fun <T> setResult(transactionId: String, state: TransactionState, result: T): Mono<T> {
        return reactiveRedisTemplate.convertAndSend(
            CHANNEL, Transaction(
                transactionId,
                serverId,
                group,
                state,
                event = objectMapper.writeValueAsString(result)
            )
        ).map { result }
    }

    private companion object {
        private const val CHANNEL = "ORCHESTRATE_RESULT_CHANNEL"
    }
}
