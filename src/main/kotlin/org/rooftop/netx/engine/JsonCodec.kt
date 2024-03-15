package org.rooftop.netx.engine

import com.fasterxml.jackson.databind.ObjectMapper
import org.rooftop.netx.api.Codec
import org.rooftop.netx.api.DecodeException
import org.rooftop.netx.api.EncodeException
import kotlin.reflect.KClass

class JsonCodec(
    private val objectMapper: ObjectMapper,
) : Codec {

    override fun <T : Any> encode(data: T): String {
        require(data::class != Any::class) { "Data cannot be Any" }
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
