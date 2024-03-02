package org.rooftop.netx.engine

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import org.rooftop.netx.api.Codec
import org.rooftop.netx.api.DecodeException
import org.rooftop.netx.api.EncodeException
import kotlin.reflect.KClass

class JsonCodec : Codec {

    private val objectMapper: ObjectMapper by lazy {
        ObjectMapper().registerModule(ParameterNamesModule(JsonCreator.Mode.PROPERTIES))
            .registerModule(KotlinModule.Builder().build())
    }

    override fun <T> encode(data: T): String {
        requireNotNull(data) { "Data cannot be null" }
        require(data!!::class != Any::class) { "Data cannot be Any" }
        return runCatching { objectMapper.writeValueAsString(data) }
            .getOrElse {
                throw EncodeException("Cannot encode \"${data}\" to \"${String::class}\"", it)
            }
    }

    override fun <T : Any> decode(data: String, type: KClass<T>): T {
        return runCatching { objectMapper.readValue(data, type.java) }
            .getOrElse {
                throw DecodeException("Cannot decode \"$data\" to \"${type}\"", it)
            }
    }
}
