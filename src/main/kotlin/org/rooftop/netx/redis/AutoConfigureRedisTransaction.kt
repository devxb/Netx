package org.rooftop.netx.redis

import org.springframework.boot.autoconfigure.ImportAutoConfiguration

@ImportAutoConfiguration(RedisTransactionConfigurer::class)
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AutoConfigureRedisTransaction
