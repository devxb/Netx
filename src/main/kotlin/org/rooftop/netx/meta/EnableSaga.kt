package org.rooftop.netx.meta

import org.rooftop.netx.redis.RedisSagaConfigurer
import org.springframework.context.annotation.Import

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(RedisSagaConfigurer::class)
annotation class EnableSaga
