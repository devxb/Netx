package org.rooftop.netx.api

import kotlin.reflect.KClass

class TransactionRollbackEvent(
    transactionId: String,
    nodeName: String,
    group: String,
    val cause: String?,
    private val codec: Codec,
    private val undo: String,
) : TransactionEvent(transactionId, nodeName, group) {

    fun <T : Any> decodeUndo(type: KClass<T>): T = codec.decode(undo, type)
}
