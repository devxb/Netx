package org.rooftop.netx.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import org.rooftop.netx.api.Result
import org.rooftop.netx.api.ResultException
import org.rooftop.netx.api.ResultTimeoutException
import org.rooftop.netx.core.Codec
import org.rooftop.netx.engine.ResultHolder
import org.rooftop.netx.engine.logging.info
import org.springframework.data.redis.core.ReactiveRedisTemplate
import reactor.core.publisher.Mono
import reactor.pool.PoolBuilder
import java.util.concurrent.TimeoutException
import kotlin.time.Duration
import kotlin.time.toJavaDuration

internal class RedisResultHolder(
    poolSize: Int,
    private val codec: Codec,
    private val objectMapper: ObjectMapper,
    private val reactiveRedisTemplate: ReactiveRedisTemplate<String, String>,
) : ResultHolder {

    private val pool = PoolBuilder.from(Mono.just(reactiveRedisTemplate.opsForList()))
        .sizeBetween(1, poolSize)
        .maxPendingAcquireUnbounded()
        .buildPool()

    override fun <T : Any> getResult(
        timeout: Duration,
        id: String
    ): Mono<Result<T>> {
        return pool.withPoolable {
            it.leftPop("Netx:Result:$id", timeout.toJavaDuration())
                .switchIfEmpty(Mono.error {
                    ResultTimeoutException(
                        "Cannot get result in \"$timeout\" time",
                        TimeoutException()
                    )
                })
        }.single()
            .map { result ->
                val isSuccess = result.isSuccess()
                val value = result.value()
                if (isSuccess) {
                    return@map Result.success<T>(codec, value)
                }
                val encodedError = objectMapper.readValue(value, Error::class.java)
                val type =
                    objectMapper.readValue(encodedError.type, jacksonTypeRef<Class<Throwable>>())
                Result.fail(codec, encodedError.error, type.kotlin)
            }.doOnNext { info("Get result $it") }
    }

    private fun String.isSuccess(): Boolean {
        return this.split(":")[0] == "$SUCCESS"
    }

    private fun String.value(): String {
        return this.substring("$SUCCESS".length + 1)
    }

    override fun <T : Any> setSuccessResult(
        id: String,
        result: T
    ): Mono<T> {
        return reactiveRedisTemplate.opsForList()
            .leftPush(
                "Netx:Result:$id",
                "$SUCCESS:${objectMapper.writeValueAsString(result)}"
            ).map { result }
            .doOnNext { info("Set success result $it") }
    }

    override fun <T : Throwable> setFailResult(id: String, result: T): Mono<T> {
        val error = runCatching {
            Error(
                type = objectMapper.writeValueAsString(result::class.java),
                error = objectMapper.writeValueAsString(result),
            )
        }.getOrElse {
            Error(
                type = objectMapper.writeValueAsString(ResultException::class.java),
                error = objectMapper.writeValueAsString(
                    ResultException("Cannot encode fail result to json cause \"${it.message}\"")
                ),
            )
        }

        val encodedError = objectMapper.writeValueAsString(error)
        return reactiveRedisTemplate.opsForList()
            .leftPush(
                "Netx:Result:$id",
                "$FAIL:$encodedError"
            ).map { result }
            .doOnNext { info("Set fail result $it") }
    }

    private data class Error(
        val type: String,
        val error: String,
    )

    private companion object {
        private const val SUCCESS = 0
        private const val FAIL = 1
    }
}
