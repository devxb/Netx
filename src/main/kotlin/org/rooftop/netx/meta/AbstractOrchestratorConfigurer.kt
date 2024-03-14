package org.rooftop.netx.meta

import org.rooftop.netx.api.Codec
import org.rooftop.netx.api.TransactionManager
import org.rooftop.netx.engine.AbstractTransactionDispatcher
import org.rooftop.netx.engine.OrchestrateResultHolder
import org.rooftop.netx.engine.OrchestratorBuilder
import org.rooftop.netx.engine.TransactionIdGenerator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration

@Configuration
abstract class AbstractOrchestratorConfigurer {

    @Autowired
    private lateinit var transactionIdGenerator: TransactionIdGenerator

    @Autowired
    private lateinit var transactionManager: TransactionManager

    @Autowired
    private lateinit var transactionDispatcher: AbstractTransactionDispatcher

    @Autowired
    private lateinit var codec: Codec

    @Autowired
    private lateinit var orchestrateResultHolder: OrchestrateResultHolder

    protected fun newOrchestrator(): OrchestratorBuilder.OrchestratorPreBuilder =
        OrchestratorBuilder.preBuilder(
            transactionIdGenerator,
            transactionManager,
            transactionDispatcher,
            codec,
            orchestrateResultHolder,
        )
}
