package org.rooftop.netx.engine

import com.fasterxml.jackson.databind.ObjectMapper
import org.rooftop.netx.api.Codec
import org.rooftop.netx.api.DecodeException
import org.rooftop.netx.api.EncodeException
import org.rooftop.netx.api.TypeReference
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

    override fun <T : Any> decode(data: String, type: TypeReference<T>): T {
        return runCatching {
            val javaType = objectMapper.typeFactory.constructType(type.type)
            objectMapper.readValue<T>(data, javaType)
        }.getOrElse {
            throw DecodeException("Cannot decode \"$data\" to \"${type.type}\"", it)
        }
    }
}
