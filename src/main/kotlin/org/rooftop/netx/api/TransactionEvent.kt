package org.rooftop.netx.api

import kotlin.reflect.KClass

sealed class TransactionEvent(
    val transactionId: String,
    val nodeName: String,
    val group: String,
    private val event: String?,
    private val codec: Codec,
) {
    fun <T : Any> decodeEvent(type: KClass<T>): T =
        codec.decode(
            event ?: throw NullPointerException("Cannot decode event cause event is null"),
            type
        )
}
