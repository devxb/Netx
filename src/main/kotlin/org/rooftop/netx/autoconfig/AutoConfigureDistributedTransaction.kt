package org.rooftop.netx.autoconfig

import org.rooftop.netx.redis.RedisTransactionConfigurer
import org.springframework.boot.autoconfigure.ImportAutoConfiguration

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ImportAutoConfiguration(RedisTransactionConfigurer::class)
annotation class AutoConfigureDistributedTransaction
