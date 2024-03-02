package org.rooftop.netx.api

import kotlin.reflect.KClass

class TransactionRollbackEvent(
    transactionId: String,
    nodeName: String,
    group: String,
    event: String?,
    val cause: String?,
    private val undo: String,
    private val codec: Codec,
) : TransactionEvent(transactionId, nodeName, group, event, codec) {

    fun <T : Any> decodeUndo(type: KClass<T>): T = codec.decode(undo, type)
}
