package org.rooftop.netx.redis

import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestComponent
import org.springframework.data.domain.Range
import org.springframework.data.redis.core.ReactiveRedisOperations

@TestComponent
internal class RedisAssertions(
    private val reactiveRedisOperations: ReactiveRedisOperations<String, ByteArray>,
    @Value("\${netx.group}") private val nodeGroup: String,
) {

    fun pendingMessageCountShouldBe(transactionId: String, count: Long) {
        val pendingMessageCount = reactiveRedisOperations.opsForStream<String, String>()
            .pending(transactionId, nodeGroup, Range.closed("-", "+"), Long.MAX_VALUE)
            .map { it.get().toList().size }
            .block()

        pendingMessageCount shouldBe count
    }

    fun retryTransactionShouldBeNotExists(transactionId: String) {
        val retryTransaction = reactiveRedisOperations.opsForSet()
            .members(nodeGroup)
            .map { String(it) }
            .any {  it == transactionId  }
            .block()

        retryTransaction shouldBe false
    }
}
