package org.rooftop.netx.client

import org.rooftop.netx.api.TransactionManager
import org.springframework.stereotype.Service

@Service
class NetxClient(
    private val transactionManager: TransactionManager,
) {

    fun startTransaction(undo: String): String {
        return transactionManager.start(undo).block()!!
    }

    fun rollbackTransaction(transactionId: String, cause: String): String {
        return transactionManager.rollback(transactionId, cause).block()!!
    }

    fun joinTransaction(transactionId: String, undo: String): String {
        return transactionManager.join(transactionId, undo).block()!!
    }

    fun commitTransaction(transactionId: String): String {
        return transactionManager.commit(transactionId).block()!!
    }
}
