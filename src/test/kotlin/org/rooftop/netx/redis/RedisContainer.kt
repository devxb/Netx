package org.rooftop.netx.redis

import io.mockk.InternalPlatformDsl.toStr
import org.springframework.boot.test.context.TestConfiguration
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration
class RedisContainer {
    init {
        val redis: GenericContainer<*> = GenericContainer(DockerImageName.parse("redis:7.2.3"))
            .withExposedPorts(6379)

        redis.start()

        System.setProperty(
            "netx.port",
            redis.getMappedPort(6379).toString()
        )
        System.setProperty(
            "netx.undo.port",
            redis.getMappedPort(6379).toString()
        )
    }
}
