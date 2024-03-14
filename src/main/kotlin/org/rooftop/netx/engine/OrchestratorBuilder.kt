package org.rooftop.netx.engine

import org.rooftop.netx.api.Codec
import org.rooftop.netx.api.OrchestrateFunction
import org.rooftop.netx.api.Orchestrator
import org.rooftop.netx.api.TransactionManager
import reactor.core.publisher.Mono
import kotlin.reflect.KClass

class OrchestratorBuilder(
    private val transactionIdGenerator: TransactionIdGenerator,
    private val transactionManager: TransactionManager,
    private val transactionDispatcher: AbstractTransactionDispatcher,
    private val codec: Codec,
    private val orchestrateResultHolder: OrchestrateResultHolder,
    private val startOrchestrateHolder: StartOrchestrateFunctionHolder<*>,
    private val joinOrchestrateHolders: MutableList<JoinOrchestrateFunctionHolder<*>> = mutableListOf(),
) {

    fun join(function: OrchestrateFunction<Mono<Any>>): OrchestratorBuilder {
        joinOrchestrateHolders.add(
            JoinOrchestrateFunctionHolder(
                SupportsOrchestrateFunctionType.MONO,
                arrayOf(),
                function,
            )
        )
        return this
    }

    fun join(
        vararg noRollbackFor: KClass<out Throwable>,
        function: OrchestrateFunction<Mono<Any>>
    ): OrchestratorBuilder {
        joinOrchestrateHolders.add(
            JoinOrchestrateFunctionHolder(
                SupportsOrchestrateFunctionType.MONO,
                noRollbackFor,
                function,
            )
        )
        return this
    }

    fun joinSync(function: OrchestrateFunction<Any>): OrchestratorBuilder {
        joinOrchestrateHolders.add(
            JoinOrchestrateFunctionHolder(
                SupportsOrchestrateFunctionType.DEFAULT,
                arrayOf(),
                function,
            )
        )
        return this
    }

    fun joinSync(
        vararg noRollbackFor: KClass<out Throwable>,
        function: OrchestrateFunction<Any>
    ): OrchestratorBuilder {
        joinOrchestrateHolders.add(
            JoinOrchestrateFunctionHolder(
                SupportsOrchestrateFunctionType.DEFAULT,
                noRollbackFor,
                function,
            )
        )
        return this
    }

    fun commit(function: OrchestrateFunction<Mono<Any>>): OrchestratorRestrictBuilder {
        return OrchestratorRestrictBuilder(
            transactionIdGenerator,
            transactionManager,
            transactionDispatcher,
            codec,
            startOrchestrateHolder = startOrchestrateHolder,
            joinOrchestrateHolders = joinOrchestrateHolders,
            commitOrchestrateHolder = CommitOrchestrateFunctionHolder(
                SupportsOrchestrateFunctionType.MONO,
                arrayOf(),
                function,
            ),
            rollbackOrchestrateHolder = defaultRollbackOrchestrateHolder,
            orchestrateResultHolder = orchestrateResultHolder,
        )
    }

    fun commit(
        vararg noRollbackFor: KClass<out Throwable>,
        function: OrchestrateFunction<Mono<Any>>
    ): OrchestratorRestrictBuilder {
        return OrchestratorRestrictBuilder(
            transactionIdGenerator,
            transactionManager,
            transactionDispatcher,
            codec,
            startOrchestrateHolder = startOrchestrateHolder,
            joinOrchestrateHolders = joinOrchestrateHolders,
            commitOrchestrateHolder = CommitOrchestrateFunctionHolder(
                SupportsOrchestrateFunctionType.MONO,
                noRollbackFor,
                function,
            ),
            rollbackOrchestrateHolder = defaultRollbackOrchestrateHolder,
            orchestrateResultHolder = orchestrateResultHolder,
        )
    }

    fun commitSync(function: OrchestrateFunction<Any>): OrchestratorRestrictBuilder {
        return OrchestratorRestrictBuilder(
            transactionIdGenerator,
            transactionManager,
            transactionDispatcher,
            codec,
            startOrchestrateHolder = startOrchestrateHolder,
            joinOrchestrateHolders = joinOrchestrateHolders,
            commitOrchestrateHolder = CommitOrchestrateFunctionHolder(
                SupportsOrchestrateFunctionType.DEFAULT,
                arrayOf(),
                function,
            ),
            rollbackOrchestrateHolder = defaultRollbackOrchestrateHolder,
            orchestrateResultHolder = orchestrateResultHolder,
        )
    }

    fun commitSync(
        vararg noRollbackFor: KClass<out Throwable>,
        function: OrchestrateFunction<Any>
    ): OrchestratorRestrictBuilder {
        return OrchestratorRestrictBuilder(
            transactionIdGenerator,
            transactionManager,
            transactionDispatcher,
            codec,
            startOrchestrateHolder = startOrchestrateHolder,
            joinOrchestrateHolders = joinOrchestrateHolders,
            commitOrchestrateHolder = CommitOrchestrateFunctionHolder(
                SupportsOrchestrateFunctionType.DEFAULT,
                noRollbackFor,
                function,
            ),
            rollbackOrchestrateHolder = defaultRollbackOrchestrateHolder,
            orchestrateResultHolder = orchestrateResultHolder,
        )
    }

    fun rollback(function: OrchestrateFunction<Mono<Any>>): OrchestratorRestrictBuilder {
        return OrchestratorRestrictBuilder(
            transactionIdGenerator,
            transactionManager,
            transactionDispatcher,
            codec,
            startOrchestrateHolder = startOrchestrateHolder,
            joinOrchestrateHolders = joinOrchestrateHolders,
            commitOrchestrateHolder = defaultCommitOrchestrateHolder,
            rollbackOrchestrateHolder = RollbackOrchestrateFunctionHolder(
                SupportsOrchestrateFunctionType.MONO,
                function
            ),
            orchestrateResultHolder = orchestrateResultHolder,
        )
    }

    fun rollbackSync(function: OrchestrateFunction<Any>): OrchestratorRestrictBuilder {
        return OrchestratorRestrictBuilder(
            transactionIdGenerator,
            transactionManager,
            transactionDispatcher,
            codec,
            startOrchestrateHolder = startOrchestrateHolder,
            joinOrchestrateHolders = joinOrchestrateHolders,
            commitOrchestrateHolder = defaultCommitOrchestrateHolder,
            rollbackOrchestrateHolder = RollbackOrchestrateFunctionHolder(
                SupportsOrchestrateFunctionType.DEFAULT,
                function
            ),
            orchestrateResultHolder = orchestrateResultHolder,
        )
    }

    fun <T : Any> build(): Orchestrator<T> {
        return OrchestratorRestrictBuilder(
            transactionIdGenerator,
            transactionManager,
            transactionDispatcher,
            codec,
            startOrchestrateHolder = startOrchestrateHolder,
            joinOrchestrateHolders = joinOrchestrateHolders,
            commitOrchestrateHolder = defaultCommitOrchestrateHolder,
            rollbackOrchestrateHolder = defaultRollbackOrchestrateHolder,
            orchestrateResultHolder = orchestrateResultHolder,
        ).build()
    }

    class OrchestratorPreBuilder(
        private val transactionIdGenerator: TransactionIdGenerator,
        private val transactionManager: TransactionManager,
        private val transactionDispatcher: AbstractTransactionDispatcher,
        private val codec: Codec,
        private val orchestrateResultHolder: OrchestrateResultHolder,
    ) {

        fun start(function: OrchestrateFunction<Mono<Any>>): OrchestratorBuilder =
            OrchestratorBuilder(
                transactionIdGenerator,
                transactionManager,
                transactionDispatcher,
                codec,
                orchestrateResultHolder,
                StartOrchestrateFunctionHolder(
                    SupportsOrchestrateFunctionType.MONO,
                    arrayOf(),
                    function,
                )
            )

        fun start(
            vararg noRollbackFor: KClass<out Throwable>,
            function: OrchestrateFunction<Mono<Any>>
        ): OrchestratorBuilder =
            OrchestratorBuilder(
                transactionIdGenerator,
                transactionManager,
                transactionDispatcher,
                codec,
                orchestrateResultHolder,
                StartOrchestrateFunctionHolder(
                    SupportsOrchestrateFunctionType.MONO,
                    noRollbackFor,
                    function,
                )
            )

        fun startSync(function: OrchestrateFunction<Any>): OrchestratorBuilder =
            OrchestratorBuilder(
                transactionIdGenerator,
                transactionManager,
                transactionDispatcher,
                codec,
                orchestrateResultHolder,
                StartOrchestrateFunctionHolder(
                    SupportsOrchestrateFunctionType.DEFAULT,
                    arrayOf(),
                    function,
                )
            )

        fun startSync(
            vararg noRollbackFor: KClass<out Throwable>,
            function: OrchestrateFunction<Any>
        ): OrchestratorBuilder =
            OrchestratorBuilder(
                transactionIdGenerator,
                transactionManager,
                transactionDispatcher,
                codec,
                orchestrateResultHolder,
                StartOrchestrateFunctionHolder(
                    SupportsOrchestrateFunctionType.DEFAULT,
                    noRollbackFor,
                    function,
                )
            )
    }

    class OrchestratorRestrictBuilder(
        private val transactionIdGenerator: TransactionIdGenerator,
        private val transactionManager: TransactionManager,
        private val transactionDispatcher: AbstractTransactionDispatcher,
        private val codec: Codec,
        private val orchestrateResultHolder: OrchestrateResultHolder,
        private val startOrchestrateHolder: StartOrchestrateFunctionHolder<*>,
        private val joinOrchestrateHolders: List<JoinOrchestrateFunctionHolder<*>>,
        private var commitOrchestrateHolder: CommitOrchestrateFunctionHolder<*>,
        private var rollbackOrchestrateHolder: RollbackOrchestrateFunctionHolder<*>,
    ) {

        fun commit(function: OrchestrateFunction<Mono<Any>>): OrchestratorRestrictBuilder {
            commitOrchestrateHolder =
                CommitOrchestrateFunctionHolder(
                    SupportsOrchestrateFunctionType.MONO,
                    arrayOf(),
                    function,
                )
            return this
        }

        fun commit(
            vararg noRollbackFor: KClass<out Throwable>,
            function: OrchestrateFunction<Mono<Any>>
        ): OrchestratorRestrictBuilder {
            commitOrchestrateHolder =
                CommitOrchestrateFunctionHolder(
                    SupportsOrchestrateFunctionType.MONO,
                    noRollbackFor,
                    function,
                )
            return this
        }

        fun commitSync(function: OrchestrateFunction<Any>): OrchestratorRestrictBuilder {
            commitOrchestrateHolder =
                CommitOrchestrateFunctionHolder(
                    SupportsOrchestrateFunctionType.DEFAULT,
                    arrayOf(),
                    function,
                )
            return this
        }

        fun commitSync(
            vararg noRollbackFor: KClass<out Throwable>,
            function: OrchestrateFunction<Any>
        ): OrchestratorRestrictBuilder {
            commitOrchestrateHolder =
                CommitOrchestrateFunctionHolder(
                    SupportsOrchestrateFunctionType.DEFAULT,
                    noRollbackFor,
                    function,
                )
            return this
        }

        fun rollback(function: OrchestrateFunction<Mono<Any>>): OrchestratorRestrictBuilder {
            rollbackOrchestrateHolder =
                RollbackOrchestrateFunctionHolder(SupportsOrchestrateFunctionType.MONO, function)
            return this
        }

        fun rollbackSync(function: OrchestrateFunction<Any>): OrchestratorRestrictBuilder {
            rollbackOrchestrateHolder =
                RollbackOrchestrateFunctionHolder(SupportsOrchestrateFunctionType.DEFAULT, function)
            return this
        }

        fun <T : Any> build(): Orchestrator<T> {
            val orchestrateId = transactionIdGenerator.generate()
            val startOrchestrateOperator = getStartOrchestrateListener(orchestrateId)
            val joinOrchestrateOperators = getJoinOrchestrateListeners(orchestrateId)
            val commitOrchestratorOperator = getCommitOrchestratorListener(orchestrateId)
            val rollbackOrchestratorOperator = getRollbackOrchestratorListener(orchestrateId)
            transactionDispatcher.addHandlers(
                listOf(
                    startOrchestrateOperator,
                    commitOrchestratorOperator,
                    rollbackOrchestratorOperator
                )
            )
            transactionDispatcher.addHandlers(joinOrchestrateOperators)
            return OrchestratorManager(
                transactionManager = transactionManager,
                codec = codec,
                orchestrateId = orchestrateId,
                orchestrateResultHolder = orchestrateResultHolder,
            )
        }

        private fun getStartOrchestrateListener(orchestrateId: String): Any {
            return startOrchestrateHolder.toOrchestrateOperator(
                joinOrchestrateHolders.isEmpty(),
                codec,
                transactionManager,
                orchestrateId,
            )
        }

        private fun getJoinOrchestrateListeners(orchestrateId: String): List<Any> =
            joinOrchestrateHolders.withIndex()
                .map {
                    it.value.toOrchestrateOperator(
                        it.index == joinOrchestrateHolders.size - 1,
                        codec,
                        transactionManager,
                        orchestrateId,
                        it.index + 1,
                    )
                }.toList()

        private fun getCommitOrchestratorListener(orchestrateId: String): Any {
            return commitOrchestrateHolder.toOrchestrateOperator(
                codec,
                transactionManager,
                orchestrateId,
                orchestrateResultHolder,
            )
        }

        private fun getRollbackOrchestratorListener(orchestrateId: String): Any {
            return rollbackOrchestrateHolder.toOrchestrateOperator(
                codec,
                orchestrateId,
                orchestrateResultHolder,
            )
        }
    }

    companion object {
        private val defaultCommitOrchestrateHolder = CommitOrchestrateFunctionHolder<Mono<Any>>(
            SupportsOrchestrateFunctionType.MONO,
            arrayOf(),
        ) { Mono.just(it) }

        private val defaultRollbackOrchestrateHolder = RollbackOrchestrateFunctionHolder<Mono<Any>>(
            SupportsOrchestrateFunctionType.MONO
        ) { Mono.just(it) }

        internal fun preBuilder(
            transactionIdGenerator: TransactionIdGenerator,
            transactionManager: TransactionManager,
            transactionDispatcher: AbstractTransactionDispatcher,
            codec: Codec,
            orchestrateResultHolder: OrchestrateResultHolder,
        ): OrchestratorPreBuilder {
            return OrchestratorPreBuilder(
                transactionIdGenerator,
                transactionManager,
                transactionDispatcher,
                codec,
                orchestrateResultHolder,
            )
        }
    }
}
