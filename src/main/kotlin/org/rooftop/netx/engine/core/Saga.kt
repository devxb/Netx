package org.rooftop.netx.engine.core

internal data class Saga(
    val id: String,
    val serverId: String,
    val group: String,
    val state: SagaState,
    val cause: String? = null,
    val event: String? = null,
)
