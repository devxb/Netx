package org.rooftop.netx.redis

import org.rooftop.netx.engine.UndoManager
import org.springframework.data.redis.core.ReactiveRedisTemplate
import reactor.core.publisher.Mono

class RedisUndoManager(
    private val group: String,
    private val reactiveRedisTemplate: ReactiveRedisTemplate<String, String>,
) : UndoManager {

    override fun find(transactionId: String): Mono<String> {
        return reactiveRedisTemplate.opsForValue()["$group:$transactionId"]
            .switchIfEmpty(
                Mono.error {
                    throw IllegalStateException("Cannot find undo state \"$group:$transactionId\"")
                }
            )
    }

    override fun delete(transactionId: String): Mono<Boolean> {
        return reactiveRedisTemplate.opsForValue().delete("$group:$transactionId")
    }

    override fun save(transactionId: String, undo: String): Mono<String> {
        return reactiveRedisTemplate.opsForValue()
            .set("$group:$transactionId", undo)
            .flatMap {
                when (it) {
                    true -> Mono.just(it)
                    false -> Mono.error { throw IllegalStateException("Error occurred during the undo process.") }
                }
            }
            .map { transactionId }
    }
}
