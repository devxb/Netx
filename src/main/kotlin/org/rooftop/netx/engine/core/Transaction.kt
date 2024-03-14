package org.rooftop.netx.engine.core

data class Transaction(
    val id: String,
    val serverId: String,
    val group: String,
    val state: TransactionState,
    val undo: String? = null,
    val cause: String? = null,
    val event: String? = null,
)
