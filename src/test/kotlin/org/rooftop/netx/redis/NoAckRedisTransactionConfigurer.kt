package org.rooftop.netx.redis

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import org.rooftop.netx.api.TransactionManager
import org.rooftop.netx.engine.JsonCodec
import org.rooftop.netx.engine.RequestHolder
import org.rooftop.netx.engine.ResultHolder
import org.rooftop.netx.engine.TransactionIdGenerator
import org.rooftop.netx.engine.core.Transaction
import org.rooftop.netx.engine.logging.logger
import org.rooftop.netx.engine.OrchestratorFactory
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
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
    @Value("\${netx.backpressure:10}") private val backpressureSize: Int,
    @Value("\${netx.logging.level:off}") loggingLevel: String,
    @Value("\${netx.pool-size:100}") private val poolSize: Int,
    private val applicationContext: ApplicationContext,
) {

    init {
        logger = LoggerFactory.getLogger("org.rooftop.netx.logger.$loggingLevel")
    }

    @Bean
    @ConditionalOnProperty(prefix = "netx", name = ["mode"], havingValue = "redis")
    fun redisStreamOrchestratorFactory(): OrchestratorFactory = OrchestratorFactory(
        transactionManager = redisStreamTransactionManager(),
        transactionDispatcher = redisStreamTransactionDispatcher(),
        codec = jsonCodec(),
        resultHolder = redisOrchestrateResultHolder(),
        requestHolder = redisRequestHolder(),
    )

    @Bean
    @ConditionalOnProperty(prefix = "netx", name = ["mode"], havingValue = "redis")
    fun redisStreamTransactionManager(): TransactionManager =
        RedisStreamTransactionManager(
            nodeName = nodeName,
            nodeGroup = nodeGroup,
            reactiveRedisTemplate = transactionReactiveRedisTemplate(),
            codec = jsonCodec(),
            transactionIdGenerator = tsidTransactionIdGenerator(),
            objectMapper = netxObjectMapper(),
        )

    @Bean
    @ConditionalOnProperty(prefix = "netx", name = ["mode"], havingValue = "redis")
    fun tsidTransactionIdGenerator(): TransactionIdGenerator = TransactionIdGenerator(nodeId)

    @Bean
    @ConditionalOnProperty(prefix = "netx", name = ["mode"], havingValue = "redis")
    fun redisStreamTransactionListener(): RedisStreamTransactionListener =
        RedisStreamTransactionListener(
            backpressureSize = backpressureSize,
            transactionDispatcher = noAckRedisStreamTransactionDispatcher(),
            connectionFactory = reactiveRedisConnectionFactory(),
            nodeGroup = nodeGroup,
            nodeName = nodeName,
            reactiveRedisTemplate = transactionReactiveRedisTemplate(),
            objectMapper = netxObjectMapper(),
        ).also { it.subscribeStream() }

    @Bean
    @ConditionalOnProperty(prefix = "netx", name = ["mode"], havingValue = "redis")
    fun jsonCodec(): JsonCodec = JsonCodec(netxObjectMapper())

    @Bean
    @ConditionalOnProperty(prefix = "netx", name = ["mode"], havingValue = "redis")
    fun netxObjectMapper(): ObjectMapper =
        ObjectMapper().registerModule(ParameterNamesModule(JsonCreator.Mode.PROPERTIES))
            .registerModule(KotlinModule.Builder().build())

    @Bean
    @ConditionalOnProperty(prefix = "netx", name = ["mode"], havingValue = "redis")
    internal fun redisOrchestrateResultHolder(): ResultHolder =
        RedisResultHolder(
            poolSize,
            jsonCodec(),
            netxObjectMapper(),
            reactiveRedisTemplate(),
        )

    @Bean
    @ConditionalOnProperty(prefix = "netx", name = ["mode"], havingValue = "redis")
    internal fun redisRequestHolder(): RequestHolder =
        RedisRequestHolder(
            jsonCodec(),
            reactiveRedisTemplate(),
        )

    @Bean
    @ConditionalOnProperty(prefix = "netx", name = ["mode"], havingValue = "redis")
    fun noAckRedisStreamTransactionDispatcher(): NoAckRedisStreamTransactionDispatcher =
        NoAckRedisStreamTransactionDispatcher(
            nodeGroup = nodeGroup,
            reactiveRedisTemplate = transactionReactiveRedisTemplate(),
            codec = jsonCodec(),
        )

    @Bean
    @ConditionalOnProperty(prefix = "netx", name = ["mode"], havingValue = "redis")
    fun redisTransactionRetrySupporter(): RedisTransactionRetrySupporter =
        RedisTransactionRetrySupporter(
            nodeGroup = nodeGroup,
            nodeName = nodeName,
            reactiveRedisTemplate = transactionReactiveRedisTemplate(),
            transactionDispatcher = redisStreamTransactionDispatcher(),
            orphanMilli = orphanMilli,
            recoveryMilli = recoveryMilli,
            backpressureSize = backpressureSize,
            objectMapper = netxObjectMapper(),
        ).also { it.watchOrphanTransaction() }

    @Bean
    @ConditionalOnProperty(prefix = "netx", name = ["mode"], havingValue = "redis")
    fun redisStreamTransactionDispatcher(): RedisStreamTransactionDispatcher =
        RedisStreamTransactionDispatcher(
            applicationContext = applicationContext,
            reactiveRedisTemplate = transactionReactiveRedisTemplate(),
            nodeGroup = nodeGroup,
            codec = jsonCodec(),
        )

    @Bean
    @ConditionalOnProperty(prefix = "netx", name = ["mode"], havingValue = "redis")
    fun transactionReactiveRedisTemplate(): ReactiveRedisTemplate<String, Transaction> {
        val keySerializer = StringRedisSerializer()
        val valueSerializer = Jackson2JsonRedisSerializer(Transaction::class.java)

        val builder: RedisSerializationContext.RedisSerializationContextBuilder<String, Transaction> =
            RedisSerializationContext.newSerializationContext(keySerializer)

        val context = builder.value(valueSerializer).build()

        return ReactiveRedisTemplate(reactiveRedisConnectionFactory(), context);
    }

    @Bean
    @ConditionalOnProperty(prefix = "netx", name = ["mode"], havingValue = "redis")
    fun reactiveRedisTemplate(): ReactiveRedisTemplate<String, String> {
        val keySerializer = StringRedisSerializer()
        val valueSerializer = StringRedisSerializer()

        val builder: RedisSerializationContext.RedisSerializationContextBuilder<String, String> =
            RedisSerializationContext.newSerializationContext(keySerializer)

        val context = builder.value(valueSerializer).build()

        return ReactiveRedisTemplate(reactiveRedisConnectionFactory(), context);
    }

    @Bean
    @ConditionalOnProperty(prefix = "netx", name = ["mode"], havingValue = "redis")
    fun reactiveRedisConnectionFactory(): ReactiveRedisConnectionFactory {
        val port: String = System.getProperty("netx.port") ?: port

        return LettuceConnectionFactory(host, port.toInt())
    }
}

