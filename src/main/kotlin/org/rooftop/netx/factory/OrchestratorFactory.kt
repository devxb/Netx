package org.rooftop.netx.factory

import org.rooftop.netx.api.Codec
import org.rooftop.netx.api.TransactionManager
import org.rooftop.netx.engine.AbstractTransactionDispatcher
import org.rooftop.netx.engine.OrchestrateChain
import org.rooftop.netx.engine.OrchestrateResultHolder

class OrchestratorFactory internal constructor(
    private val transactionManager: TransactionManager,
    private val transactionDispatcher: AbstractTransactionDispatcher,
    private val codec: Codec,
    private val orchestrateResultHolder: OrchestrateResultHolder,
) {

    fun <T : Any> create(orchestratorId: String): OrchestrateChain.Pre<T> {
        return OrchestrateChain.Pre(
            orchestratorId = orchestratorId,
            transactionManager = transactionManager,
            transactionDispatcher = transactionDispatcher,
            codec = codec,
            orchestrateResultHolder = orchestrateResultHolder,
        )
    }
}
