package org.rooftop.netx.redis

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class RedisUndoConfigurer(
    @Value("\${netx.group}") private val group: String,
    @Value("\${netx.undo.host}") private val netxUndoHost: String,
    @Value("\${netx.undo.port}") private val netxUndoPort: String,
) {

    @Bean
    fun redisUndoManager(): RedisUndoManager = RedisUndoManager(group, redisUndoServer())

    @Bean
    fun redisUndoServer(): ReactiveRedisTemplate<String, String> {
        val stringRedisSerializer = StringRedisSerializer()

        val context =
            RedisSerializationContext.newSerializationContext<String, String>(stringRedisSerializer)
                .value(stringRedisSerializer)
                .build()

        return ReactiveRedisTemplate(undoServerConnectionFactory(), context)
    }

    @Bean
    fun undoServerConnectionFactory(): ReactiveRedisConnectionFactory {
        val undoServerPort: String = System.getProperty("netx.undo.port") ?: netxUndoPort

        return LettuceConnectionFactory(netxUndoHost, undoServerPort.toInt())
    }
}
