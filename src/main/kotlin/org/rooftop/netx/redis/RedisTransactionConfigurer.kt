package org.rooftop.netx.redis

import org.rooftop.netx.api.TransactionManager
import org.rooftop.netx.engine.UndoManager
import org.rooftop.pay.infra.transaction.ByteArrayRedisSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class RedisTransactionConfigurer(
    @Value("\${netx.host}") private val host: String,
    @Value("\${netx.port}") private val port: String,
    @Value("\${netx.group}") private val group: String,
    @Value("\${netx.node-id}") private val nodeId: Int,
    @Value("\${netx.node-name}") private val nodeName: String,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val undoManager: UndoManager,
) {

    @Bean
    fun redisStreamTransactionManager(): TransactionManager =
        RedisStreamTransactionManager(
            nodeId,
            nodeName,
            applicationEventPublisher,
            undoManager,
            reactiveRedisTemplate()
        )

    @Bean
    fun redisStreamTransactionDispatcher(): RedisStreamTransactionDispatcher =
        RedisStreamTransactionDispatcher(
            applicationEventPublisher,
            reactiveRedisConnectionFactory(),
            undoManager,
            group,
            nodeName,
            reactiveRedisTemplate()
        )

    @Bean
    fun reactiveRedisTemplate(): ReactiveRedisTemplate<String, ByteArray> {
        val builder = RedisSerializationContext.newSerializationContext<String, ByteArray>(
            StringRedisSerializer()
        )

        val context = builder.value(byteArrayRedisSerializer()).build()

        return ReactiveRedisTemplate(reactiveRedisConnectionFactory(), context)
    }

    @Bean
    fun byteArrayRedisSerializer(): ByteArrayRedisSerializer {
        return ByteArrayRedisSerializer()
    }

    @Bean
    fun reactiveRedisConnectionFactory(): ReactiveRedisConnectionFactory {
        val port: String = System.getProperty("netx.port") ?: port

        return LettuceConnectionFactory(host, port.toInt())
    }
}
