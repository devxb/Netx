package org.rooftop.netx.redis

import org.redisson.Redisson
import org.redisson.api.RedissonReactiveClient
import org.redisson.config.Config
import org.rooftop.netx.api.TransactionManager
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer

@TestConfiguration
class NoAckRedisTransactionConfigurer(
    @Value("\${netx.host}") private val host: String,
    @Value("\${netx.port}") private val port: String,
    @Value("\${netx.group}") private val nodeGroup: String,
    @Value("\${netx.node-id}") private val nodeId: Int,
    @Value("\${netx.node-name}") private val nodeName: String,
    @Value("\${netx.recovery-milli:60000}") private val recoveryMilli: Long,
    @Value("\${netx.orphan-milli:10000}") private val orphanMilli: Long,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {

    @Bean
    @ConditionalOnProperty(prefix = "netx", name = ["mode"], havingValue = "redis")
    fun redisStreamTransactionManager(): TransactionManager =
        RedisStreamTransactionManager(
            nodeId = nodeId,
            nodeName = nodeName,
            nodeGroup = nodeGroup,
            transactionDispatcher = noAckRedisStreamTransactionDispatcher(),
            transactionRetrySupporter = redisTransactionRetrySupporter(),
            reactiveRedisTemplate = reactiveRedisTemplate(),
        )

    @Bean
    @ConditionalOnProperty(prefix = "netx", name = ["mode"], havingValue = "redis")
    fun redisStreamTransactionDispatcher(): RedisStreamTransactionDispatcher =
        RedisStreamTransactionDispatcher(
            eventPublisher = applicationEventPublisher,
            connectionFactory = reactiveRedisConnectionFactory(),
            nodeGroup = nodeGroup,
            nodeName = nodeName,
            reactiveRedisTemplate = reactiveRedisTemplate()
        )

    @Bean
    @ConditionalOnProperty(prefix = "netx", name = ["mode"], havingValue = "redis")
    fun noAckRedisStreamTransactionDispatcher(): NoAckRedisStreamTransactionDispatcher =
        NoAckRedisStreamTransactionDispatcher(
            eventPublisher = applicationEventPublisher,
            connectionFactory = reactiveRedisConnectionFactory(),
            nodeGroup = nodeGroup,
            nodeName = nodeName,
            reactiveRedisTemplate = reactiveRedisTemplate()
        )

    @Bean
    @ConditionalOnProperty(prefix = "netx", name = ["mode"], havingValue = "redis")
    fun redisTransactionRetrySupporter(): RedisTransactionRetrySupporter =
        RedisTransactionRetrySupporter(
            nodeGroup = nodeGroup,
            nodeName = nodeName,
            reactiveRedisTemplate = reactiveRedisTemplate(),
            redissonReactiveClient = redissonReactiveClient(),
            transactionDispatcher = redisStreamTransactionDispatcher(),
            orphanMilli = orphanMilli,
            recoveryMilli = recoveryMilli,
        )

    @Bean
    @ConditionalOnProperty(prefix = "netx", name = ["mode"], havingValue = "redis")
    fun reactiveRedisTemplate(): ReactiveRedisTemplate<String, ByteArray> {
        val builder = RedisSerializationContext.newSerializationContext<String, ByteArray>(
            StringRedisSerializer()
        )

        val context = builder.value(byteArrayRedisSerializer()).build()

        return ReactiveRedisTemplate(reactiveRedisConnectionFactory(), context)
    }

    @Bean
    @ConditionalOnProperty(prefix = "netx", name = ["mode"], havingValue = "redis")
    fun redissonReactiveClient(): RedissonReactiveClient {
        val port: String = System.getProperty("netx.port") ?: port

        return Redisson.create(
            Config()
                .also {
                    it.useSingleServer()
                        .setAddress("redis://$host:$port")
                }).reactive()
    }

    @Bean
    @ConditionalOnProperty(prefix = "netx", name = ["mode"], havingValue = "redis")
    fun byteArrayRedisSerializer(): ByteArrayRedisSerializer {
        return ByteArrayRedisSerializer()
    }

    @Bean
    @ConditionalOnProperty(prefix = "netx", name = ["mode"], havingValue = "redis")
    fun reactiveRedisConnectionFactory(): ReactiveRedisConnectionFactory {
        val port: String = System.getProperty("netx.port") ?: port

        return LettuceConnectionFactory(host, port.toInt())
    }
}

