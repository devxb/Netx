package org.rooftop.netx.engine

internal data class OrchestrateEvent(
    val orchestrateId: String,
    val orchestrateSequence: Int = 0,
    val clientEvent: String,
)
