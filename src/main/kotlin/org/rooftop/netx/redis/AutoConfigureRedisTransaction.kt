package org.rooftop.netx.redis

import org.springframework.boot.autoconfigure.ImportAutoConfiguration

@ImportAutoConfiguration
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AutoConfigureRedisTransaction
