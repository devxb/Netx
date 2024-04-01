package org.rooftop.netx.engine

import org.rooftop.netx.api.*
import org.rooftop.netx.meta.SagaHandler
import java.util.*

@SagaHandler
internal class SagaReceiveStorage {

    private val startEvents: Queue<SagaStartEvent> = LinkedList()
    private val joinEvents: Queue<SagaJoinEvent> = LinkedList()
    private val rollbackEvents: Queue<SagaRollbackEvent> = LinkedList()
    private val commitEvents: Queue<SagaCommitEvent> = LinkedList()

    fun clear(){
        startEvents.clear()
        joinEvents.clear()
        rollbackEvents.clear()
        commitEvents.clear()
    }

    @SagaStartListener(successWith = SuccessWith.END)
    fun handleStart(sagaStartEvent: SagaStartEvent) {
        startEvents.add(sagaStartEvent)
    }

    @SagaJoinListener(successWith = SuccessWith.END)
    fun handleJoin(sagaJoinEvent: SagaJoinEvent) {
        joinEvents.add(sagaJoinEvent)
    }

    @SagaRollbackListener
    fun handleRollback(sagaRollbackEvent: SagaRollbackEvent) {
        rollbackEvents.add(sagaRollbackEvent)
    }

    @SagaCommitListener
    fun handleCommit(sagaCommitEvent: SagaCommitEvent) {
        commitEvents.add(sagaCommitEvent)
    }

    fun pollStart(): SagaStartEvent = startEvents.poll()

    fun pollJoin(): SagaJoinEvent = joinEvents.poll()

    fun pollRollback(): SagaRollbackEvent = rollbackEvents.poll()

    fun pollCommit(): SagaCommitEvent = commitEvents.poll()

}
