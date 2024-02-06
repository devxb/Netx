package org.rooftop.netx.autoconfig

import org.rooftop.netx.redis.RedisTransactionConfigurer
import org.rooftop.netx.redis.RedisUndoConfigurer
import org.springframework.boot.autoconfigure.ImportAutoConfiguration

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ImportAutoConfiguration(RedisUndoConfigurer::class, RedisTransactionConfigurer::class)
annotation class AutoConfigureDistributedTransaction
