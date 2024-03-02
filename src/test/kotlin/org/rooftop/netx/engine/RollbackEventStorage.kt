package org.rooftop.netx.engine

import org.rooftop.netx.api.TransactionRollbackEvent
import org.rooftop.netx.api.TransactionRollbackHandler
import org.rooftop.netx.meta.TransactionHandler
import reactor.core.publisher.Mono
import java.util.*

@TransactionHandler
class RollbackEventStorage {

    private val rollbackEvents: Queue<TransactionRollbackEvent> = LinkedList()

    @TransactionRollbackHandler
    fun handleRollback(transactionRollbackEvent: TransactionRollbackEvent) {
        rollbackEvents.add(transactionRollbackEvent)
    }

    fun poll(): TransactionRollbackEvent = rollbackEvents.poll()
}
