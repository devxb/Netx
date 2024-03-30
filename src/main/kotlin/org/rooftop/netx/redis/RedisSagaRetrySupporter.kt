package org.rooftop.netx.redis

import com.fasterxml.jackson.databind.ObjectMapper
import org.rooftop.netx.engine.AbstractSagaDispatcher
import org.rooftop.netx.engine.AbstractSagaRetrySupporter
import org.rooftop.netx.engine.core.Saga
import org.springframework.data.domain.Range
import org.springframework.data.redis.connection.RedisStreamCommands.XClaimOptions
import org.springframework.data.redis.core.ReactiveRedisTemplate
import reactor.core.publisher.Flux

internal class RedisSagaRetrySupporter(
    recoveryMilli: Long,
    backpressureSize: Int,
    sagaDispatcher: AbstractSagaDispatcher,
    private val nodeGroup: String,
    private val nodeName: String,
    private val reactiveRedisTemplate: ReactiveRedisTemplate<String, Saga>,
    private val orphanMilli: Long,
    private val objectMapper: ObjectMapper,
) : AbstractSagaRetrySupporter(backpressureSize, recoveryMilli, sagaDispatcher) {

    override fun claimOrphanSaga(backpressureSize: Int): Flux<Pair<Saga, String>> {
        return reactiveRedisTemplate.opsForStream<String, String>()
            .pending(STREAM_KEY, nodeGroup, Range.closed("-", "+"), backpressureSize.toLong())
            .filter { it.get().toList().isNotEmpty() }
            .flatMapMany {
                reactiveRedisTemplate.opsForStream<String, String>()
                    .claim(
                        STREAM_KEY,
                        nodeGroup,
                        nodeName,
                        XClaimOptions.minIdleMs(orphanMilli)
                            .ids(it.get().map { eachMessage -> eachMessage.id.value }.toList())
                    )
            }
            .map {
                objectMapper.readValue(
                    it.value["data"],
                    Saga::class.java
                ) to it.id.toString()
            }
    }

    private companion object {
        private const val STREAM_KEY = "NETX_STREAM"
    }
}
