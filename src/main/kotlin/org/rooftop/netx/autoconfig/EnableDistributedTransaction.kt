package org.rooftop.netx.autoconfig

import org.rooftop.netx.redis.RedisTransactionConfigurer
import org.springframework.context.annotation.Import

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(RedisTransactionConfigurer::class)
annotation class EnableDistributedTransaction
