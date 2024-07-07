package org.rooftop.netx.ground

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.spring.SpringExtension
import org.rooftop.netx.meta.EnableSaga
import org.rooftop.netx.redis.RedisContainer
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource

@EnableSaga
@TestPropertySource("classpath:application.properties")
@ContextConfiguration(classes = [RedisContainer::class])
class GroudTest(
    private val netxObjectMapper: ObjectMapper,
) : StringSpec({

    extension(SpringExtension)

    "ObjectMapper UnknownProperty Test" {
        val request = "{ \"name\": \"xb\" }"

        shouldThrowAny {
            netxObjectMapper.readValue(request, Target::class.java)
        }
    }
}) {

    data class Target(
        val id: Long,
        val name: String,
        val isSuccess: Boolean,
    )
}
