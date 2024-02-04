package org.rooftop.pay.infra.transaction

import org.springframework.data.redis.serializer.RedisSerializer

class ByteArrayRedisSerializer : RedisSerializer<ByteArray> {

    override fun serialize(t: ByteArray?): ByteArray? = t

    override fun deserialize(bytes: ByteArray?): ByteArray? = bytes
}
