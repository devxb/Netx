package org.rooftop.netx.engine

import org.rooftop.netx.api.*
import org.rooftop.netx.core.Codec
import org.rooftop.netx.engine.listen.*
import reactor.core.publisher.Mono

internal class DefaultOrchestrateChain<OriginReq : Any, T : Any, V : Any> private constructor(
    private val group: String,
    private val orchestratorId: String,
    private val orchestrateSequence: Int,
    private val chainContainer: ChainContainer,
    private var orchestrateListener: AbstractOrchestrateListener<T, V>,
    private var rollbackOrchestrateListener: AbstractOrchestrateListener<T, *>?,
    private val beforeDefaultOrchestrateChain: DefaultOrchestrateChain<OriginReq, out Any, T>? = null,
) : OrchestrateChain<OriginReq, T, V> {

    private var nextDefaultOrchestrateChain: DefaultOrchestrateChain<OriginReq, V, out Any>? = null

    override fun <S : Any> join(
        orchestrate: Orchestrate<V, S>,
        rollback: Rollback<V, *>?,
    ): OrchestrateChain<OriginReq, V, S> {
        val nextJoinOrchestrateListener =
            getJoinOrchestrateListener<V, S>(CommandType.DEFAULT, orchestrate)
        val nextRollbackOrchestrateListener =
            getRollbackOrchestrateListener<V, S>(CommandType.DEFAULT, rollback)

        return nextOrchestrateChain(nextJoinOrchestrateListener, nextRollbackOrchestrateListener)
    }

    override fun <S : Any> joinWithContext(
        contextOrchestrate: ContextOrchestrate<V, S>,
        contextRollback: ContextRollback<V, *>?
    ): OrchestrateChain<OriginReq, V, S> {
        val nextJoinOrchestrateListener =
            getJoinOrchestrateListener<V, S>(CommandType.CONTEXT, contextOrchestrate)
        val nextRollbackOrchestrateListener =
            getRollbackOrchestrateListener<V, S>(CommandType.CONTEXT, contextRollback)

        return nextOrchestrateChain(nextJoinOrchestrateListener, nextRollbackOrchestrateListener)
    }

    private fun <T : Any, V : Any> getJoinOrchestrateListener(
        commandType: CommandType,
        function: TypeReified<T>,
    ) = JoinOrchestrateListener(
        codec = chainContainer.codec,
        sagaManager = chainContainer.sagaManager,
        orchestratorId = orchestratorId,
        orchestrateSequence = orchestrateSequence + 1,
        orchestrateCommand = OrchestrateCommand<T, V>(
            commandType,
            chainContainer.codec,
            function
        ),
        requestHolder = chainContainer.requestHolder,
        resultHolder = chainContainer.resultHolder,
        typeReference = function.reified(),
        group = group,
    )

    private fun <S : Any> nextOrchestrateChain(
        nextJoinOrchestrateListener: JoinOrchestrateListener<V, S>,
        nextRollbackOrchestrateListener: RollbackOrchestrateListener<V, S>?
    ): OrchestrateChain<OriginReq, V, S> {
        val nextDefaultOrchestrateChain = DefaultOrchestrateChain(
            group = group,
            orchestratorId = orchestratorId,
            orchestrateSequence = orchestrateSequence + 1,
            chainContainer = chainContainer,
            orchestrateListener = nextJoinOrchestrateListener,
            rollbackOrchestrateListener = nextRollbackOrchestrateListener,
            beforeDefaultOrchestrateChain = this,
        )
        this.nextDefaultOrchestrateChain = nextDefaultOrchestrateChain

        return nextDefaultOrchestrateChain
    }

    override fun <S : Any> joinReactive(
        orchestrate: Orchestrate<V, Mono<S>>,
        rollback: Rollback<V, Mono<*>>?,
    ): OrchestrateChain<OriginReq, V, S> {
        val nextJoinOrchestrateListener =
            getMonoJoinOrchestrateListener<V, S>(CommandType.DEFAULT, orchestrate)
        val nextRollbackOrchestrateListener =
            getMonoRollbackOrchestrateListener<V, S>(CommandType.DEFAULT, rollback)

        return nextOrchestrateChain(nextJoinOrchestrateListener, nextRollbackOrchestrateListener)
    }

    override fun <S : Any> joinReactiveWithContext(
        contextOrchestrate: ContextOrchestrate<V, Mono<S>>,
        contextRollback: ContextRollback<V, Mono<*>>?
    ): OrchestrateChain<OriginReq, V, S> {
        val nextJoinOrchestrateListener =
            getMonoJoinOrchestrateListener<V, S>(CommandType.CONTEXT, contextOrchestrate)
        val nextRollbackOrchestrateListener =
            getMonoRollbackOrchestrateListener<V, S>(CommandType.CONTEXT, contextRollback)

        val nextDefaultOrchestrateChain = DefaultOrchestrateChain(
            group = group,
            orchestratorId = orchestratorId,
            orchestrateSequence = orchestrateSequence + 1,
            chainContainer = chainContainer,
            orchestrateListener = nextJoinOrchestrateListener,
            rollbackOrchestrateListener = nextRollbackOrchestrateListener,
            beforeDefaultOrchestrateChain  = this,
        )
        this.nextDefaultOrchestrateChain = nextDefaultOrchestrateChain

        return nextDefaultOrchestrateChain
    }

    private fun <T : Any, V : Any> getMonoJoinOrchestrateListener(
        commandType: CommandType,
        function: TypeReified<T>,
    ) = MonoJoinOrchestrateListener(
        codec = chainContainer.codec,
        sagaManager = chainContainer.sagaManager,
        orchestratorId = orchestratorId,
        orchestrateSequence = orchestrateSequence + 1,
        monoOrchestrateCommand = MonoOrchestrateCommand<T, V>(
            commandType,
            chainContainer.codec,
            function
        ),
        requestHolder = chainContainer.requestHolder,
        resultHolder = chainContainer.resultHolder,
        typeReference = function.reified(),
        group = group,
    )

    private fun <S : Any> nextOrchestrateChain(
        nextJoinOrchestrateListener: MonoJoinOrchestrateListener<V, S>,
        nextRollbackOrchestrateListener: MonoRollbackOrchestrateListener<V, S>?
    ): OrchestrateChain<OriginReq, V, S> {
        val nextDefaultOrchestrateChain = DefaultOrchestrateChain(
            group = group,
            orchestratorId = orchestratorId,
            orchestrateSequence = orchestrateSequence + 1,
            chainContainer = chainContainer,
            orchestrateListener = nextJoinOrchestrateListener,
            rollbackOrchestrateListener = nextRollbackOrchestrateListener,
            beforeDefaultOrchestrateChain = this,
        )
        this.nextDefaultOrchestrateChain = nextDefaultOrchestrateChain

        return nextDefaultOrchestrateChain
    }

    override fun <S : Any> commit(
        orchestrate: Orchestrate<V, S>,
    ): Orchestrator<OriginReq, S> {
        val nextCommitOrchestrateListener =
            getCommitOrchestrateListener<V, S>(CommandType.DEFAULT, orchestrate)

        return createOrchestrator(nextCommitOrchestrateListener)
    }

    override fun <S : Any> commitWithContext(
        contextOrchestrate: ContextOrchestrate<V, S>,
    ): Orchestrator<OriginReq, S> {
        val nextCommitOrchestrateListener =
            getCommitOrchestrateListener<V, S>(CommandType.CONTEXT, contextOrchestrate)

        return createOrchestrator(nextCommitOrchestrateListener)
    }

    private fun <T : Any, V : Any> getCommitOrchestrateListener(
        commandType: CommandType,
        function: TypeReified<T>,
    ) = CommitOrchestrateListener(
        codec = chainContainer.codec,
        sagaManager = chainContainer.sagaManager,
        orchestratorId = orchestratorId,
        orchestrateSequence = orchestrateSequence + 1,
        orchestrateCommand = OrchestrateCommand<T, V>(commandType, chainContainer.codec, function),
        resultHolder = chainContainer.resultHolder,
        requestHolder = chainContainer.requestHolder,
        typeReference = function.reified(),
        group = group,
    )

    private fun <T : Any, V : Any> getRollbackOrchestrateListener(
        commandType: CommandType,
        rollback: TypeReified<T>?
    ) = rollback?.let {
        RollbackOrchestrateListener<T, V>(
            codec = chainContainer.codec,
            sagaManager = chainContainer.sagaManager,
            orchestratorId = orchestratorId,
            orchestrateSequence = orchestrateSequence + 1,
            rollbackCommand = RollbackCommand(commandType, chainContainer.codec, it),
            requestHolder = chainContainer.requestHolder,
            resultHolder = chainContainer.resultHolder,
            typeReference = it.reified(),
            group = group,
        )
    }

    private fun <S : Any> createOrchestrator(
        nextCommitOrchestrateListener: CommitOrchestrateListener<V, S>,
    ): Orchestrator<OriginReq, S> {
        return chainContainer.orchestratorCache.cache(orchestratorId) {
            val nextDefaultOrchestrateChain = DefaultOrchestrateChain(
                orchestratorId = orchestratorId,
                orchestrateSequence = orchestrateSequence + 1,
                chainContainer = chainContainer,
                orchestrateListener = nextCommitOrchestrateListener,
                rollbackOrchestrateListener = null,
                beforeDefaultOrchestrateChain = this,
                group = group,
            )
            this.nextDefaultOrchestrateChain = nextDefaultOrchestrateChain
            val firstOrchestrators = nextDefaultOrchestrateChain.initOrchestrateListeners()

            return@cache OrchestratorManager<OriginReq, S>(
                sagaManager = chainContainer.sagaManager,
                codec = chainContainer.codec,
                orchestratorId = orchestratorId,
                resultHolder = chainContainer.resultHolder,
                orchestrateListener = firstOrchestrators.first,
                rollbackOrchestrateListener = firstOrchestrators.second,
            )
        }
    }

    override fun <S : Any> commitReactive(
        orchestrate: Orchestrate<V, Mono<S>>,
    ): Orchestrator<OriginReq, S> {
        val nextJoinOrchestrateListener =
            getMonoCommitOrchestrateListener<V, S>(CommandType.DEFAULT, orchestrate)

        return createOrchestrator(nextJoinOrchestrateListener)
    }

    override fun <S : Any> commitReactiveWithContext(
        contextOrchestrate: ContextOrchestrate<V, Mono<S>>,
    ): Orchestrator<OriginReq, S> {
        val nextJoinOrchestrateListener =
            getMonoCommitOrchestrateListener<V, S>(CommandType.CONTEXT, contextOrchestrate)

        return createOrchestrator(nextJoinOrchestrateListener)
    }

    private fun <S : Any> createOrchestrator(
        nextJoinOrchestrateListener: MonoCommitOrchestrateListener<V, S>,
    ): Orchestrator<OriginReq, S> {
        return chainContainer.orchestratorCache.cache(orchestratorId) {
            val nextDefaultOrchestrateChain = DefaultOrchestrateChain(
                group = group,
                orchestratorId = orchestratorId,
                orchestrateSequence = orchestrateSequence + 1,
                chainContainer = chainContainer,
                orchestrateListener = nextJoinOrchestrateListener,
                rollbackOrchestrateListener = null,
                beforeDefaultOrchestrateChain = this,
            )
            this.nextDefaultOrchestrateChain = nextDefaultOrchestrateChain

            val firstOrchestrators = nextDefaultOrchestrateChain.initOrchestrateListeners()

            return@cache OrchestratorManager<OriginReq, S>(
                sagaManager = chainContainer.sagaManager,
                codec = chainContainer.codec,
                orchestratorId = orchestratorId,
                resultHolder = chainContainer.resultHolder,
                orchestrateListener = firstOrchestrators.first,
                rollbackOrchestrateListener = firstOrchestrators.second
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun initOrchestrateListeners(): Pair<AbstractOrchestrateListener<OriginReq, Any>, AbstractOrchestrateListener<OriginReq, Any>?> {
        val annotatedListeners = getAllOrchestrateListeners()
            .toAnnotatedListeners()

        chainOrchestrateListeners(annotatedListeners)
        chainRollbackListeners(annotatedListeners)

        addDispatcher(annotatedListeners)

        return annotatedListeners[0] as Pair<AbstractOrchestrateListener<OriginReq, Any>, AbstractOrchestrateListener<OriginReq, Any>?>
    }

    private fun getAllOrchestrateListeners(): MutableList<Pair<AbstractOrchestrateListener<out Any, out Any>, AbstractOrchestrateListener<out Any, out Any>?>> {
        val orchestrateListeners = mutableListOf<
                Pair<AbstractOrchestrateListener<out Any, out Any>, AbstractOrchestrateListener<out Any, out Any>?>>()

        var defaultOrchestrateChainCursor: DefaultOrchestrateChain<OriginReq, out Any, out Any>? =
            this
        while (defaultOrchestrateChainCursor != null) {
            orchestrateListeners.add(
                defaultOrchestrateChainCursor.orchestrateListener
                        to defaultOrchestrateChainCursor.rollbackOrchestrateListener
            )
            if (defaultOrchestrateChainCursor.beforeDefaultOrchestrateChain == null) {
                break
            }
            defaultOrchestrateChainCursor =
                defaultOrchestrateChainCursor.beforeDefaultOrchestrateChain
        }

        orchestrateListeners.reverse()

        return orchestrateListeners
    }

    private fun List<Pair<AbstractOrchestrateListener<out Any, out Any>, AbstractOrchestrateListener<out Any, out Any>?>>.toAnnotatedListeners()
            : MutableList<Pair<AbstractOrchestrateListener<out Any, out Any>, AbstractOrchestrateListener<out Any, out Any>?>> {
        return this.withIndex().map {
            val isFirst = it.index == 0
            val isLast =
                it.index == (this.size - 1 - COMMIT_LISTENER_PREFIX)

            val listener = it.value.first
            listener.isFirst = isFirst
            listener.isLast = isLast

            listener.withAnnotated() to it.value.second
        }.toMutableList()
    }

    private fun chainOrchestrateListeners(orchestrateListeners: List<Pair<AbstractOrchestrateListener<out Any, out Any>, AbstractOrchestrateListener<out Any, out Any>?>>) {
        var rollbackSequence = 0
        var beforeRollbackSequence = -1
        for (listenerWithIdx in orchestrateListeners.withIndex()) {
            val isFirst = listenerWithIdx.index == 0
            val isLast =
                listenerWithIdx.index == (orchestrateListeners.size - 1 - COMMIT_LISTENER_PREFIX)

            val listener = listenerWithIdx.value.first
            listenerWithIdx.value.second?.let {
                listener.isRollbackable = true
                rollbackSequence = it.orchestrateSequence
            }
            listener.rollbackSequence = rollbackSequence
            listener.beforeRollbackOrchestrateSequence = beforeRollbackSequence

            beforeRollbackSequence = rollbackSequence

            listener.isFirst = isFirst
            listener.isLast = isLast
            if (listenerWithIdx.index < orchestrateListeners.size - 1) {
                val nextListener = orchestrateListeners[listenerWithIdx.index + 1]
                listener.setNextOrchestrateListener(nextListener.first)
                nextListener.second?.let { listener.setNextRollbackOrchestrateListener(it) }
            }
        }
    }

    private fun chainRollbackListeners(orchestrateListeners: List<Pair<AbstractOrchestrateListener<out Any, out Any>, AbstractOrchestrateListener<out Any, out Any>?>>) {
        val rollbackListeners = orchestrateListeners.asSequence()
            .map { it.second }
            .filter { it != null }
            .toList()

        for (rollbackListenerWithIdx in rollbackListeners.withIndex()) {
            val rollbackListener = rollbackListenerWithIdx.value
                ?: throw IllegalStateException("Null Rollback listener occurred.")

            rollbackListener.isFirst = rollbackListenerWithIdx.index == 0
            rollbackListener.isLast = rollbackListenerWithIdx.index == (rollbackListeners.size - 1)

            if (rollbackListenerWithIdx.index > 0) {
                rollbackListener.beforeRollbackOrchestrateSequence =
                    rollbackListeners[rollbackListenerWithIdx.index - 1]!!.orchestrateSequence
            }
        }
    }

    private fun addDispatcher(orchestrateListeners: List<Pair<AbstractOrchestrateListener<out Any, out Any>, AbstractOrchestrateListener<out Any, out Any>?>>) {
        orchestrateListeners.forEach { (listener, rollbackListener) ->
            chainContainer.sagaDispatcher.addOrchestrate(listener)
            rollbackListener?.let { chainContainer.sagaDispatcher.addOrchestrate(it) }
        }
    }

    private fun <T : Any, V : Any> getMonoCommitOrchestrateListener(
        commandType: CommandType,
        function: TypeReified<T>,
    ) = MonoCommitOrchestrateListener(
        codec = chainContainer.codec,
        sagaManager = chainContainer.sagaManager,
        orchestratorId = orchestratorId,
        orchestrateSequence = orchestrateSequence + 1,
        monoOrchestrateCommand = MonoOrchestrateCommand<T, V>(
            commandType,
            chainContainer.codec,
            function
        ),
        resultHolder = chainContainer.resultHolder,
        requestHolder = chainContainer.requestHolder,
        typeReference = function.reified(),
        group = group,
    )

    private fun <T : Any, V : Any> getMonoRollbackOrchestrateListener(
        commandType: CommandType,
        rollback: TypeReified<T>?
    ) = rollback?.let {
        MonoRollbackOrchestrateListener<T, V>(
            codec = chainContainer.codec,
            sagaManager = chainContainer.sagaManager,
            orchestratorId = orchestratorId,
            orchestrateSequence = orchestrateSequence + 1,
            monoRollbackCommand = MonoRollbackCommand<T>(
                commandType,
                chainContainer.codec,
                it
            ),
            requestHolder = chainContainer.requestHolder,
            resultHolder = chainContainer.resultHolder,
            typeReference = it.reified(),
            group = group,
        )
    }

    internal class Pre<T : Any> internal constructor(
        private val group: String,
        private val orchestratorId: String,
        private val sagaManager: SagaManager,
        private val sagaDispatcher: AbstractSagaDispatcher,
        private val codec: Codec,
        private val resultHolder: ResultHolder,
        private val requestHolder: RequestHolder,
        private val orchestratorCache: OrchestratorCache,
    ) : OrchestrateChain.Pre<T> {

        override fun <V : Any> start(
            orchestrate: Orchestrate<T, V>,
            rollback: Rollback<T, *>?,
        ): OrchestrateChain<T, T, V> {
            val startOrchestrateListener =
                getStartOrchestrateListener<V>(CommandType.DEFAULT, orchestrate)
            val rollbackOrchestrateListener =
                getRollbackOrchestrateListener<V>(CommandType.DEFAULT, rollback)

            return DefaultOrchestrateChain(
                group = group,
                orchestratorId = orchestratorId,
                orchestrateSequence = 0,
                chainContainer = getStreamContainer(),
                orchestrateListener = startOrchestrateListener,
                rollbackOrchestrateListener = rollbackOrchestrateListener,
            )
        }

        override fun <V : Any> startWithContext(
            contextOrchestrate: ContextOrchestrate<T, V>,
            contextRollback: ContextRollback<T, *>?
        ): OrchestrateChain<T, T, V> {
            val startOrchestrateListener =
                getStartOrchestrateListener<V>(CommandType.CONTEXT, contextOrchestrate)
            val rollbackOrchestrateListener =
                getRollbackOrchestrateListener<V>(CommandType.CONTEXT, contextRollback)

            return DefaultOrchestrateChain(
                group = group,
                orchestratorId = orchestratorId,
                orchestrateSequence = 0,
                chainContainer = getStreamContainer(),
                orchestrateListener = startOrchestrateListener,
                rollbackOrchestrateListener = rollbackOrchestrateListener,
            )
        }

        private fun <V : Any> getStartOrchestrateListener(
            commandType: CommandType,
            function: TypeReified<T>,
        ) = StartOrchestrateListener(
            codec = codec,
            sagaManager = sagaManager,
            orchestratorId = orchestratorId,
            orchestrateSequence = 0,
            orchestrateCommand = OrchestrateCommand<T, V>(
                commandType,
                codec,
                function,
            ),
            requestHolder = requestHolder,
            resultHolder = resultHolder,
            typeReference = function.reified(),
            group = group,
        )

        private fun <V : Any> getRollbackOrchestrateListener(
            commandType: CommandType,
            rollback: TypeReified<T>?
        ) = rollback?.let {
            RollbackOrchestrateListener<T, V>(
                codec = codec,
                sagaManager = sagaManager,
                orchestratorId = orchestratorId,
                orchestrateSequence = 0,
                rollbackCommand = RollbackCommand<T>(
                    commandType,
                    codec,
                    it
                ),
                requestHolder = requestHolder,
                resultHolder = resultHolder,
                typeReference = it.reified(),
                group = group,
            )
        }

        override fun <V : Any> startReactive(
            orchestrate: Orchestrate<T, Mono<V>>,
            rollback: Rollback<T, Mono<*>>?,
        ): OrchestrateChain<T, T, V> {
            val startOrchestrateListener =
                getMonoStartOrchestrateListener<V>(CommandType.DEFAULT, orchestrate)
            val rollbackOrchestrateListener =
                getMonoRollbackOrchestrateListener<V>(CommandType.DEFAULT, rollback)

            return DefaultOrchestrateChain(
                group = group,
                orchestratorId = orchestratorId,
                orchestrateSequence = 0,
                chainContainer = getStreamContainer(),
                orchestrateListener = startOrchestrateListener,
                rollbackOrchestrateListener = rollbackOrchestrateListener,
            )
        }

        override fun <V : Any> startReactiveWithContext(
            contextOrchestrate: ContextOrchestrate<T, Mono<V>>,
            contextRollback: ContextRollback<T, Mono<*>>?
        ): OrchestrateChain<T, T, V> {
            val startOrchestrateListener =
                getMonoStartOrchestrateListener<V>(CommandType.CONTEXT, contextOrchestrate)
            val rollbackOrchestrateListener =
                getMonoRollbackOrchestrateListener<V>(CommandType.CONTEXT, contextRollback)

            return DefaultOrchestrateChain(
                group = group,
                orchestratorId = orchestratorId,
                orchestrateSequence = 0,
                chainContainer = getStreamContainer(),
                orchestrateListener = startOrchestrateListener,
                rollbackOrchestrateListener = rollbackOrchestrateListener,
            )
        }

        private fun <V : Any> getMonoStartOrchestrateListener(
            commandType: CommandType,
            function: TypeReified<T>,
        ) = MonoStartOrchestrateListener(
            codec = codec,
            sagaManager = sagaManager,
            orchestratorId = orchestratorId,
            orchestrateSequence = 0,
            monoOrchestrateCommand = MonoOrchestrateCommand<T, V>(
                commandType,
                codec,
                function,
            ),
            requestHolder = requestHolder,
            resultHolder = resultHolder,
            typeReference = function.reified(),
            group = group,
        )

        private fun <V : Any> getMonoRollbackOrchestrateListener(
            commandType: CommandType,
            rollback: TypeReified<T>?,
        ) = rollback?.let {
            MonoRollbackOrchestrateListener<T, V>(
                codec = codec,
                sagaManager = sagaManager,
                orchestratorId = orchestratorId,
                orchestrateSequence = 0,
                monoRollbackCommand = MonoRollbackCommand(
                    commandType,
                    codec,
                    it,
                ),
                requestHolder = requestHolder,
                resultHolder = resultHolder,
                typeReference = it.reified(),
                group = group,
            )
        }

        private fun getStreamContainer(): ChainContainer = ChainContainer(
            sagaManager,
            sagaDispatcher,
            codec,
            resultHolder,
            requestHolder,
            orchestratorCache,
        )
    }

    companion object {
        private const val COMMIT_LISTENER_PREFIX = 1
    }

    private data class ChainContainer(
        val sagaManager: SagaManager,
        val sagaDispatcher: AbstractSagaDispatcher,
        val codec: Codec,
        val resultHolder: ResultHolder,
        val requestHolder: RequestHolder,
        val orchestratorCache: OrchestratorCache,
    )
}
