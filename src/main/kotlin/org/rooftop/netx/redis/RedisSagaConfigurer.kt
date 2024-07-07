package org.rooftop.netx.redis

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import org.rooftop.netx.api.SagaManager
import org.rooftop.netx.engine.*
import org.rooftop.netx.engine.core.Saga
import org.rooftop.netx.engine.logging.LoggerFactory
import org.rooftop.netx.engine.logging.info
import org.rooftop.netx.engine.logging.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.connection.RedisPassword
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer


@Configuration
class RedisSagaConfigurer(
    @Value("\${netx.host}") private val host: String,
    @Value("\${netx.port}") private val port: String,
    @Value("\${netx.password:0000}") private val password: String,
    @Value("\${netx.group}") private val nodeGroup: String,
    @Value("\${netx.node-id}") private val nodeId: Int,
    @Value("\${netx.node-name}") private val nodeName: String,
    @Value("\${netx.recovery-milli:1000}") private val recoveryMilli: Long,
    @Value("\${netx.orphan-milli:60000}") private val orphanMilli: Long,
    @Value("\${netx.backpressure:40}") private val backpressureSize: Int,
    @Value("\${netx.logging.level:off}") loggingLevel: String,
    @Value("\${netx.pool-size:10}") private val poolSize: Int,
    private val applicationContext: ApplicationContext,
) {

    init {
        logger = LoggerFactory.getLogger(loggingLevel)
    }

    @Bean
    @ConditionalOnProperty(prefix = "netx", name = ["mode"], havingValue = "redis")
    internal fun redisStreamOrchestratorFactory(): OrchestratorFactory = OrchestratorFactory(
        sagaManager = redisStreamSagaManager(),
        sagaDispatcher = redisStreamSagaDispatcher(),
        codec = jsonCodec(),
        resultHolder = redisResultHolder(),
        requestHolder = redisRequestHolder(),
    ).apply { org.rooftop.netx.api.OrchestratorFactory.orchestratorFactory = this }

    @Bean
    @ConditionalOnProperty(prefix = "netx", name = ["mode"], havingValue = "redis")
    internal fun redisStreamSagaManager(): SagaManager =
        RedisStreamSagaManager(
            nodeName = nodeName,
            nodeGroup = nodeGroup,
            reactiveRedisTemplate = sagaReactiveRedisTemplate(),
            codec = jsonCodec(),
            sagaIdGenerator = tsidSagaIdGenerator(),
            objectMapper = netxObjectMapper(),
        ).also {
            info("RedisStreamSagaManager connect to host : \"$host\" port : \"$port\" nodeName : \"$nodeName\" nodeGroup : \"$nodeGroup\"")
        }

    @Bean
    @ConditionalOnProperty(prefix = "netx", name = ["mode"], havingValue = "redis")
    internal fun tsidSagaIdGenerator(): SagaIdGenerator = SagaIdGenerator(nodeId)

    @Bean
    @ConditionalOnProperty(prefix = "netx", name = ["mode"], havingValue = "redis")
    internal fun redisStreamSagaListener(): RedisStreamSagaListener =
        RedisStreamSagaListener(
            backpressureSize = backpressureSize,
            sagaDispatcher = redisStreamSagaDispatcher(),
            connectionFactory = reactiveRedisConnectionFactory(),
            nodeGroup = nodeGroup,
            nodeName = nodeName,
            reactiveRedisTemplate = sagaReactiveRedisTemplate(),
            objectMapper = netxObjectMapper(),
        ).also {
            info("RedisStreamSagaListener connect to host : \"$host\" port : \"$port\" nodeName : \"$nodeName\" nodeGroup : \"$nodeGroup\" backpressureSize : \"$backpressureSize\"")
            it.subscribeStream()
        }

    @Bean
    @ConditionalOnProperty(prefix = "netx", name = ["mode"], havingValue = "redis")
    internal fun redisResultHolder(): ResultHolder =
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
    internal fun jsonCodec(): JsonCodec = JsonCodec(netxObjectMapper())

    @Bean
    @ConditionalOnProperty(prefix = "netx", name = ["mode"], havingValue = "redis")
    internal fun netxObjectMapper(): ObjectMapper =
        ObjectMapper()
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            .registerModule(ParameterNamesModule(JsonCreator.Mode.PROPERTIES))
            .registerModule(KotlinModule.Builder().build())
            .registerModule(JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
            .configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, true)
            .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true)

    @Bean
    @ConditionalOnProperty(prefix = "netx", name = ["mode"], havingValue = "redis")
    internal fun redisSagaRetrySupporter(): RedisSagaRetrySupporter =
        RedisSagaRetrySupporter(
            nodeGroup = nodeGroup,
            nodeName = nodeName,
            reactiveRedisTemplate = sagaReactiveRedisTemplate(),
            sagaDispatcher = redisStreamSagaDispatcher(),
            orphanMilli = orphanMilli,
            recoveryMilli = recoveryMilli,
            backpressureSize = backpressureSize,
            objectMapper = netxObjectMapper(),
        ).also {
            info("RedisSagaRetrySupporter connect to host : \"$host\" port : \"$port\" nodeName : \"$nodeName\" nodeGroup : \"$nodeGroup\" orphanMilli : \"$orphanMilli\" recoveryMilli : \"$recoveryMilli\" backpressureSize : \"$backpressureSize\"")
            it.watchOrphanSaga()
        }

    @Bean
    @ConditionalOnProperty(prefix = "netx", name = ["mode"], havingValue = "redis")
    internal fun redisStreamSagaDispatcher(): RedisStreamSagaDispatcher =
        RedisStreamSagaDispatcher(
            applicationContext = applicationContext,
            reactiveRedisTemplate = sagaReactiveRedisTemplate(),
            nodeGroup = nodeGroup,
            codec = jsonCodec(),
            sagaManager = redisStreamSagaManager(),
        ).also {
            info("RedisStreamSagaDispatcher connect to host : \"$host\" port : \"$port\" nodeName : \"$nodeName\" nodeGroup : \"$nodeGroup\"")
        }

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "netx", name = ["mode"], havingValue = "redis")
    internal fun sagaReactiveRedisTemplate(): ReactiveRedisTemplate<String, Saga> {
        val keySerializer = StringRedisSerializer()
        val valueSerializer =
            Jackson2JsonRedisSerializer(netxObjectMapper(), Saga::class.java)

        val builder: RedisSerializationContext.RedisSerializationContextBuilder<String, Saga> =
            RedisSerializationContext.newSerializationContext(keySerializer)

        val context = builder.value(valueSerializer).build()

        return ReactiveRedisTemplate(reactiveRedisConnectionFactory(), context);
    }

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "netx", name = ["mode"], havingValue = "redis")
    internal fun reactiveRedisTemplate(): ReactiveRedisTemplate<String, String> {
        val keySerializer = StringRedisSerializer()
        val valueSerializer = StringRedisSerializer()

        val builder: RedisSerializationContext.RedisSerializationContextBuilder<String, String> =
            RedisSerializationContext.newSerializationContext(keySerializer)

        val context = builder.value(valueSerializer).build()

        return ReactiveRedisTemplate(reactiveRedisConnectionFactory(), context);
    }

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "netx", name = ["mode"], havingValue = "redis")
    internal fun reactiveRedisConnectionFactory(): ReactiveRedisConnectionFactory {
        val port: String = System.getProperty("netx.port") ?: port

        val redisStandaloneConfiguration = RedisStandaloneConfiguration()
        redisStandaloneConfiguration.hostName = host
        redisStandaloneConfiguration.port = port.toInt()
        redisStandaloneConfiguration.password = RedisPassword.of(password)

        return LettuceConnectionFactory(redisStandaloneConfiguration)
    }
}
