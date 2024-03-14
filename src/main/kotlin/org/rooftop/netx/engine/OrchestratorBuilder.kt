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

    class OrchestratorPreBuilder(
        private val transactionIdGenerator: TransactionIdGenerator,
        private val transactionManager: TransactionManager,
        private val transactionDispatcher: AbstractTransactionDispatcher,
        private val codec: Codec,
        private val orchestrateResultHolder: OrchestrateResultHolder,
    ) {

        fun <T : Any> start(function: OrchestrateFunction<Mono<T>>): OrchestratorBuilder =
            start(listOf(), function)

        fun <T : Any> start(
            noRollbackFor: List<Class<out Throwable>>,
            function: OrchestrateFunction<Mono<T>>
        ): OrchestratorBuilder = OrchestratorBuilder(
            transactionIdGenerator,
            transactionManager,
            transactionDispatcher,
            codec,
            orchestrateResultHolder,
            StartOrchestrateFunctionHolder(
                SupportsOrchestrateFunctionType.MONO,
                noRollbackFor.map { it.kotlin }.toTypedArray(),
                function,
            )
        )

        fun <T : Any> start(
            vararg noRollbackFor: KClass<out Throwable>,
            function: OrchestrateFunction<Mono<T>>
        ): OrchestratorBuilder = OrchestratorBuilder(
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

        fun <T : Any> startSync(function: OrchestrateFunction<T>): OrchestratorBuilder =
            startSync(listOf(), function)

        fun <T : Any> startSync(
            noRollbackFor: List<Class<out Throwable>>,
            function: OrchestrateFunction<T>
        ): OrchestratorBuilder =
            OrchestratorBuilder(
                transactionIdGenerator,
                transactionManager,
                transactionDispatcher,
                codec,
                orchestrateResultHolder,
                StartOrchestrateFunctionHolder(
                    SupportsOrchestrateFunctionType.DEFAULT,
                    noRollbackFor.map { it.kotlin }.toTypedArray(),
                    function,
                )
            )

        fun <T : Any> startSync(
            vararg noRollbackFor: KClass<out Throwable>,
            function: OrchestrateFunction<T>
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

    fun <T : Any> join(function: OrchestrateFunction<Mono<T>>): OrchestratorBuilder =
        join(listOf(), function)

    fun <T : Any> join(
        noRollbackFor: List<Class<out Throwable>>,
        function: OrchestrateFunction<Mono<T>>
    ): OrchestratorBuilder {
        joinOrchestrateHolders.add(
            JoinOrchestrateFunctionHolder(
                SupportsOrchestrateFunctionType.MONO,
                noRollbackFor.map { it.kotlin }.toTypedArray(),
                function,
            )
        )
        return this
    }

    fun <T : Any> join(
        vararg noRollbackFor: KClass<out Throwable>,
        function: OrchestrateFunction<Mono<T>>
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

    fun <T : Any> joinSync(function: OrchestrateFunction<T>): OrchestratorBuilder =
        joinSync(listOf(), function)

    fun <T : Any> joinSync(
        noRollbackFor: List<Class<out Throwable>>,
        function: OrchestrateFunction<T>
    ): OrchestratorBuilder {
        joinOrchestrateHolders.add(
            JoinOrchestrateFunctionHolder(
                SupportsOrchestrateFunctionType.DEFAULT,
                noRollbackFor.map { it.kotlin }.toTypedArray(),
                function,
            )
        )
        return this
    }

    fun <T : Any> joinSync(
        vararg noRollbackFor: KClass<out Throwable>,
        function: OrchestrateFunction<T>
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

    fun <T : Any> commit(function: OrchestrateFunction<Mono<T>>): OrchestratorRestrictBuilder =
        commit(listOf(), function)

    fun <T : Any> commit(
        noRollbackFor: List<Class<out Throwable>>,
        function: OrchestrateFunction<Mono<T>>
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
                noRollbackFor.map { it.kotlin }.toTypedArray(),
                function,
            ),
            rollbackOrchestrateHolder = defaultRollbackOrchestrateHolder,
            orchestrateResultHolder = orchestrateResultHolder,
        )
    }

    fun <T : Any> commit(
        vararg noRollbackFor: KClass<out Throwable>,
        function: OrchestrateFunction<Mono<T>>
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

    fun <T : Any> commitSync(function: OrchestrateFunction<T>): OrchestratorRestrictBuilder =
        commitSync(listOf(), function)

    fun <T : Any> commitSync(
        noRollbackFor: List<Class<out Throwable>>,
        function: OrchestrateFunction<T>
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
                noRollbackFor.map { it.kotlin }.toTypedArray(),
                function,
            ),
            rollbackOrchestrateHolder = defaultRollbackOrchestrateHolder,
            orchestrateResultHolder = orchestrateResultHolder,
        )
    }

    fun <T : Any> commitSync(
        vararg noRollbackFor: KClass<out Throwable>,
        function: OrchestrateFunction<T>
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

    fun <T : Any> rollback(function: OrchestrateFunction<Mono<T>>): OrchestratorRestrictBuilder {
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

    fun <T : Any> rollbackSync(function: OrchestrateFunction<T>): OrchestratorRestrictBuilder {
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

        fun <T : Any> commit(function: OrchestrateFunction<Mono<T>>): OrchestratorRestrictBuilder =
            commit(listOf(), function)

        fun <T : Any> commit(
            noRollbackFor: List<Class<out Throwable>>,
            function: OrchestrateFunction<Mono<T>>
        ): OrchestratorRestrictBuilder {
            commitOrchestrateHolder =
                CommitOrchestrateFunctionHolder(
                    SupportsOrchestrateFunctionType.MONO,
                    noRollbackFor.map { it.kotlin }.toTypedArray(),
                    function,
                )
            return this
        }

        fun <T : Any> commit(
            vararg noRollbackFor: KClass<out Throwable>,
            function: OrchestrateFunction<Mono<T>>
        ): OrchestratorRestrictBuilder {
            commitOrchestrateHolder =
                CommitOrchestrateFunctionHolder(
                    SupportsOrchestrateFunctionType.MONO,
                    noRollbackFor,
                    function,
                )
            return this
        }

        fun <T : Any> commitSync(function: OrchestrateFunction<T>): OrchestratorRestrictBuilder =
            commitSync(listOf(), function)

        fun <T : Any> commitSync(
            noRollbackFor: List<Class<out Throwable>>,
            function: OrchestrateFunction<T>
        ): OrchestratorRestrictBuilder {
            commitOrchestrateHolder =
                CommitOrchestrateFunctionHolder(
                    SupportsOrchestrateFunctionType.DEFAULT,
                    noRollbackFor.map { it.kotlin }.toTypedArray(),
                    function,
                )
            return this
        }

        fun <T : Any> commitSync(
            vararg noRollbackFor: KClass<out Throwable>,
            function: OrchestrateFunction<T>
        ): OrchestratorRestrictBuilder {
            commitOrchestrateHolder =
                CommitOrchestrateFunctionHolder(
                    SupportsOrchestrateFunctionType.DEFAULT,
                    noRollbackFor,
                    function,
                )
            return this
        }

        fun <T : Any> rollback(function: OrchestrateFunction<Mono<T>>): OrchestratorRestrictBuilder {
            rollbackOrchestrateHolder =
                RollbackOrchestrateFunctionHolder(SupportsOrchestrateFunctionType.MONO, function)
            return this
        }

        fun <T : Any> rollbackSync(function: OrchestrateFunction<T>): OrchestratorRestrictBuilder {
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
