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
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer
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

    private val container =
        ReactiveRedisMessageListenerContainer(reactiveRedisTemplate.connectionFactory)

    override fun getResult(timeout: Duration, transactionId: String): Mono<OrchestrateResult> {
        return container.receiveLater(ChannelTopic.of(CHANNEL))
            .flatMap { message ->
                message.timeout(timeout.toJavaDuration())
                    .onErrorMap {
                        if (it::class == TimeoutException::class) {
                            return@onErrorMap ResultTimeoutException(
                                "Can't get result in \"$timeout\" time",
                                it,
                            )
                        }
                        it
                    }
                    .map { objectMapper.readValue(it.message, Transaction::class.java) }
                    .filter { it.id == transactionId }
                    .next()
                    .map {
                        OrchestrateResult(
                            isSuccess = it.state == TransactionState.COMMIT,
                            codec = codec,
                            result = it.event
                                ?: throw NullPointerException("OrchestrateResult message cannot be null")
                        )
                    }.doOnNext { info("Get result $it") }
            }
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
        ).map { result }.doOnNext { info("Set result $it") }
    }

    private companion object {
        private const val CHANNEL = "ORCHESTRATE_RESULT_CHANNEL"
    }
}
