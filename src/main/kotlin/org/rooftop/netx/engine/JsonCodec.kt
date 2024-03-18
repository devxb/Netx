package org.rooftop.netx.engine

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.type.TypeFactory
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

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> decode(data: String, typeReference: TypeReference<T>): T {
        val javaType = TypeFactory.rawClass(typeReference.type)
        return objectMapper.readValue(data, javaType) as T
    }
}
