package org.rooftop.netx.redis

import org.rooftop.netx.core.Codec
import org.rooftop.netx.api.FailedAckSagaException
import org.rooftop.netx.api.SagaManager
import org.rooftop.netx.engine.AbstractSagaDispatcher
import org.rooftop.netx.engine.core.Saga
import org.rooftop.netx.engine.deadletter.AbstractDeadLetterManager
import org.rooftop.netx.meta.SagaHandler
import org.springframework.context.ApplicationContext
import org.springframework.data.redis.core.ReactiveRedisTemplate
import reactor.core.publisher.Mono

internal class RedisStreamSagaDispatcher(
    codec: Codec,
    sagaManager: SagaManager,
    private val applicationContext: ApplicationContext,
    private val reactiveRedisTemplate: ReactiveRedisTemplate<String, Saga>,
    private val nodeGroup: String,
    abstractDeadLetterManager: AbstractDeadLetterManager,
) : AbstractSagaDispatcher(codec, sagaManager, abstractDeadLetterManager) {

    override fun findHandlers(): List<Any> {
        return applicationContext.getBeansWithAnnotation(SagaHandler::class.java)
            .entries.asSequence()
            .map { it.value }
            .toList()
    }

    override fun ack(saga: Saga, messageId: String): Mono<Pair<Saga, String>> {
        return reactiveRedisTemplate.opsForStream<String, ByteArray>()
            .acknowledge(STREAM_KEY, nodeGroup, messageId)
            .map { saga to messageId }
            .switchIfEmpty(
                Mono.error {
                    throw FailedAckSagaException("Fail to ack saga id \"${saga.id}\" messageId \"$messageId\"")
                }
            )
    }

    private companion object {
        private const val STREAM_KEY = "NETX_STREAM"
    }
}
