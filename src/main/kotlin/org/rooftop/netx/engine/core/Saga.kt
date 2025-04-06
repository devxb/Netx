package org.rooftop.netx.engine.core

import org.rooftop.netx.api.*
import org.rooftop.netx.core.Codec

internal data class Saga(
    val id: String,
    val serverId: String,
    val group: String,
    val state: SagaState,
    val cause: String? = null,
    val event: String? = null,
) {

    fun toEvent(codec: Codec): SagaEvent {
        return when (state) {
            SagaState.START -> SagaStartEvent(
                id = id,
                nodeName = serverId,
                group = group,
                event = extractEvent(),
                codec = codec,
            )

            SagaState.COMMIT -> SagaCommitEvent(
                id = id,
                nodeName = serverId,
                group = group,
                event = extractEvent(),
                codec = codec
            )

            SagaState.JOIN -> SagaJoinEvent(
                id = id,
                nodeName = serverId,
                group = group,
                event = extractEvent(),
                codec = codec,
            )

            SagaState.ROLLBACK -> SagaRollbackEvent(
                id = id,
                nodeName = serverId,
                group = group,
                event = extractEvent(),
                cause = cause
                    ?: throw NullPointerException("Null value on SagaRollbackEvent's cause field"),
                codec = codec,
            )
        }
    }

    private fun extractEvent(): String? {
        return when (event != null) {
            true -> event
            false -> null
        }
    }

    companion object {
        internal fun of(sagaState: SagaState, sagaEvent: SagaEvent): Saga {
            return Saga(
                id = sagaEvent.id,
                serverId = sagaEvent.nodeName,
                group = sagaEvent.group,
                state = sagaState,
                cause = if (sagaState == SagaState.ROLLBACK) {
                    (sagaEvent as SagaRollbackEvent).cause
                } else {
                    null
                },
                event = sagaEvent.event
            )
        }
    }
}
