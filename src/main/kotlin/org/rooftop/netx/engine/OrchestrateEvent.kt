package org.rooftop.netx.engine

internal data class OrchestrateEvent(
    val orchestratorId: String,
    val orchestrateSequence: Int = 0,
    val clientEvent: String,
    val context: String,
)
