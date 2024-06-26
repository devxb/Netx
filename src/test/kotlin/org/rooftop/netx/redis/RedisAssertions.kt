package org.rooftop.netx.redis

import io.kotest.matchers.shouldBe
import org.rooftop.netx.engine.core.Saga
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestComponent
import org.springframework.data.domain.Range
import org.springframework.data.redis.core.ReactiveRedisOperations

@TestComponent
internal class RedisAssertions(
    private val reactiveRedisOperations: ReactiveRedisOperations<String, Saga>,
    @Value("\${netx.group}") private val nodeGroup: String,
) {

    fun pendingMessageCountShouldBe(count: Long) {
        val pendingMessageCount = reactiveRedisOperations.opsForStream<String, String>()
            .pending("NETX_STREAM", nodeGroup, Range.closed("-", "+"), Long.MAX_VALUE)
            .map { it.get().toList().size }
            .block()

        pendingMessageCount shouldBe count
    }

    fun retrySagaShouldBeNotExists(sagaId: String) {
        val retrySaga = reactiveRedisOperations.opsForSet()
            .members(nodeGroup)
            .any {  it.id == sagaId  }
            .block()

        retrySaga shouldBe false
    }
}
