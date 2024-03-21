package org.rooftop.netx.redis

import org.rooftop.netx.engine.JsonCodec
import org.rooftop.netx.engine.RequestHolder
import org.springframework.data.redis.core.ReactiveRedisTemplate
import reactor.core.publisher.Mono
import kotlin.reflect.KClass

internal class RedisRequestHolder(
    private val codec: JsonCodec,
    private val reactiveRedisTemplate: ReactiveRedisTemplate<String, String>
) : RequestHolder {

    override fun <T : Any> getRequest(key: String, type: KClass<T>): Mono<T> {
        return reactiveRedisTemplate.opsForValue()
            .get(key)
            .map { codec.decode(it, type) }
            .switchIfEmpty(
                Mono.error { NullPointerException("Cannot find exists request by key \"$key\"") }
            )
    }

    override fun <T : Any> setRequest(key: String, request: T): Mono<T> {
        return reactiveRedisTemplate.opsForValue()
            .set(key, codec.encode(request))
            .map { request }
    }
}
