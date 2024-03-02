package org.rooftop.netx.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AsyncAppenderBase
import ch.qos.logback.core.ConsoleAppender
import org.slf4j.LoggerFactory

internal object LoggerFactory {

    internal fun getLogger(level: String): Logger {
        val loggerContext = LoggerContext()

        val consoleAppender = ConsoleAppender<ILoggingEvent>().apply {
            this.context = loggerContext
            this.name = "CONSOLE"
            this.encoder = PatternLayoutEncoder().apply {
                this.context = loggerContext
                this.pattern = "%d{yyyy-MM-dd'T'HH:mm:ss.SSS'Z'}  %-5level --- [%thread] %logger{36} : %msg%n"
                this.start()
            }
            this.start()
        }

        val asyncAppender = AsyncAppenderBase<ILoggingEvent>().apply {
            this.context = loggerContext
            this.name = "ASYNC"
            this.queueSize = 10000
            this.maxFlushTime = 5000
            this.addAppender(consoleAppender)
            this.start()
        }

        val logger = LoggerFactory.getLogger("org.rooftop.netx") as Logger
        logger.addAppender(asyncAppender)
        logger.isAdditive = false

        when (level.lowercase()) {
            "warn" -> logger.level = Level.WARN
            "info" -> logger.level = Level.INFO
            else -> logger.level = Level.OFF
        }

        return logger
    }
}
