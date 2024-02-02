package org.rooftop.netx.redis

import org.rooftop.netx.api.TransactionManager
import org.rooftop.pay.infra.transaction.ByteArrayRedisSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer

@AutoConfiguration
class RedisTransactionAutoConfiguration(
    @Value("\${netx.host}") private val host: String,
    @Value("\${netx.port}") private val port: String,
    @Value("\${netx.group}") private val group: String,
    @Value("\${netx.node-name}") private val nodeName: String,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {

    @Bean
    fun redisStreamTransactionManager(): TransactionManager =
        RedisStreamTransactionManager(nodeName, applicationEventPublisher, transactionServer())

    @Bean
    fun redisStreamTransactionDispatcher(): RedisStreamTransactionDispatcher =
        RedisStreamTransactionDispatcher(
            applicationEventPublisher,
            group,
            nodeName,
            transactionServerConnectionFactory()
        )

    @Bean
    fun transactionServer(): ReactiveRedisTemplate<String, ByteArray> {
        val builder = RedisSerializationContext.newSerializationContext<String, ByteArray>(
            StringRedisSerializer()
        )

        val context = builder.value(byteArrayRedisSerializer()).build()

        return ReactiveRedisTemplate(transactionServerConnectionFactory(), context)
    }

    @Bean
    fun byteArrayRedisSerializer(): ByteArrayRedisSerializer {
        return ByteArrayRedisSerializer()
    }

    @Bean
    fun transactionServerConnectionFactory(): ReactiveRedisConnectionFactory {
        val port: String = System.getProperty("netx.port") ?: port

        return LettuceConnectionFactory(host, port.toInt())
    }
}
