package org.rooftop.netx.engine

import org.rooftop.netx.api.*
import org.rooftop.netx.engine.listen.*
import reactor.core.publisher.Mono

class DefaultOrchestrateChain<OriginReq : Any, T : Any, V : Any> private constructor(
    private val orchestratorId: String,
    private val orchestrateSequence: Int,
    private val chainContainer: ChainContainer,
    private val orchestrateListener: AbstractOrchestrateListener<T, V>,
    private val rollbackOrchestrateListener: AbstractOrchestrateListener<T, *>?,
    private val beforeDefaultOrchestrateChain: DefaultOrchestrateChain<OriginReq, out Any, T>? = null,
): OrchestrateChain<OriginReq, T, V> {

    private var nextDefaultOrchestrateChain: DefaultOrchestrateChain<OriginReq, V, out Any>? = null

    override fun <S : Any> join(
        function: OrchestrateFunction<V, S>,
        rollback: RollbackFunction<V, *>?,
    ): DefaultOrchestrateChain<OriginReq, V, S> {
        val nextJoinOrchestrateListener = getJoinOrchestrateListener(function)
        val nextRollbackOrchestrateListener = getRollbackOrchestrateListener<V, S>(rollback)

        val nextDefaultOrchestrateChain = DefaultOrchestrateChain(
            orchestratorId,
            orchestrateSequence + 1,
            chainContainer,
            nextJoinOrchestrateListener,
            nextRollbackOrchestrateListener,
            this,
        )
        this.nextDefaultOrchestrateChain = nextDefaultOrchestrateChain

        return nextDefaultOrchestrateChain
    }

    private fun <T : Any, V : Any> getJoinOrchestrateListener(function: OrchestrateFunction<T, V>) =
        JoinOrchestrateListener(
            codec = chainContainer.codec,
            transactionManager = chainContainer.transactionManager,
            orchestratorId = orchestratorId,
            orchestrateSequence = orchestrateSequence + 1,
            orchestrateFunction = function,
            requestHolder = chainContainer.requestHolder,
            resultHolder = chainContainer.resultHolder,
        )

    override fun <S : Any> joinReactive(
        function: OrchestrateFunction<V, Mono<S>>,
        rollback: RollbackFunction<V, Mono<*>>?,
    ): DefaultOrchestrateChain<OriginReq, V, S> {
        val nextJoinOrchestrateListener = getMonoJoinOrchestrateListener(function)
        val nextRollbackOrchestrateListener = getMonoRollbackOrchestrateListener<V, S>(rollback)

        val nextDefaultOrchestrateChain = DefaultOrchestrateChain(
            orchestratorId,
            orchestrateSequence + 1,
            chainContainer,
            nextJoinOrchestrateListener,
            nextRollbackOrchestrateListener,
            this,
        )
        this.nextDefaultOrchestrateChain = nextDefaultOrchestrateChain

        return nextDefaultOrchestrateChain
    }

    private fun <T : Any, V : Any> getMonoJoinOrchestrateListener(function: OrchestrateFunction<T, Mono<V>>) =
        MonoJoinOrchestrateListener(
            codec = chainContainer.codec,
            transactionManager = chainContainer.transactionManager,
            orchestratorId = orchestratorId,
            orchestrateSequence = orchestrateSequence + 1,
            orchestrateFunction = function,
            requestHolder = chainContainer.requestHolder,
            resultHolder = chainContainer.resultHolder,
        )

    override fun <S : Any> commit(
        function: OrchestrateFunction<V, S>,
        rollback: RollbackFunction<V, *>?,
    ): Orchestrator<OriginReq, S> {
        val nextCommitOrchestrateListener = getCommitOrchestrateListener(function)
        val nextRollbackOrchestrateListener = getRollbackOrchestrateListener<V, S>(rollback)

        return OrchestratorCache.cache(orchestratorId) {
            val nextDefaultOrchestrateChain = DefaultOrchestrateChain(
                orchestratorId,
                orchestrateSequence + 1,
                chainContainer,
                nextCommitOrchestrateListener,
                nextRollbackOrchestrateListener,
                this,
            )
            this.nextDefaultOrchestrateChain = nextDefaultOrchestrateChain
            val firstOrchestrateChain = nextDefaultOrchestrateChain.initOrchestrateListeners()

            return@cache OrchestratorManager<OriginReq, S>(
                transactionManager = chainContainer.transactionManager,
                codec = chainContainer.codec,
                orchestratorId = orchestratorId,
                resultHolder = chainContainer.resultHolder,
                orchestrateListener = firstOrchestrateChain.orchestrateListener,
                rollbackOrchestrateListener = firstOrchestrateChain.rollbackOrchestrateListener,
            )
        }
    }

    private fun <T : Any, V : Any> getCommitOrchestrateListener(function: OrchestrateFunction<T, V>) =
        CommitOrchestrateListener(
            codec = chainContainer.codec,
            transactionManager = chainContainer.transactionManager,
            orchestratorId = orchestratorId,
            orchestrateSequence = orchestrateSequence + 1,
            orchestrateFunction = function,
            resultHolder = chainContainer.resultHolder,
            requestHolder = chainContainer.requestHolder,
        )

    private fun <T : Any, V : Any> getRollbackOrchestrateListener(rollback: RollbackFunction<T, *>?) =
        rollback?.let {
            RollbackOrchestrateListener<T, V>(
                codec = chainContainer.codec,
                transactionManager = chainContainer.transactionManager,
                orchestratorId = orchestratorId,
                orchestrateSequence = orchestrateSequence + 1,
                rollbackFunction = it,
                requestHolder = chainContainer.requestHolder,
                resultHolder = chainContainer.resultHolder,
            )
        }

    override fun <S : Any> commitReactive(
        function: OrchestrateFunction<V, Mono<S>>,
        rollback: RollbackFunction<V, Mono<*>>?,
    ): Orchestrator<OriginReq, S> {
        val nextJoinOrchestrateListener = getMonoCommitOrchestrateListener(function)
        val nextRollbackOrchestrateListener = getMonoRollbackOrchestrateListener<V, S>(rollback)

        return OrchestratorCache.cache(orchestratorId) {
            val nextDefaultOrchestrateChain = DefaultOrchestrateChain(
                orchestratorId,
                orchestrateSequence + 1,
                chainContainer,
                nextJoinOrchestrateListener,
                nextRollbackOrchestrateListener,
                this,
            )
            this.nextDefaultOrchestrateChain = nextDefaultOrchestrateChain

            val firstOrchestrateChain = nextDefaultOrchestrateChain.initOrchestrateListeners()

            return@cache OrchestratorManager<OriginReq, S>(
                transactionManager = chainContainer.transactionManager,
                codec = chainContainer.codec,
                orchestratorId = orchestratorId,
                resultHolder = chainContainer.resultHolder,
                orchestrateListener = firstOrchestrateChain.orchestrateListener,
                rollbackOrchestrateListener = firstOrchestrateChain.rollbackOrchestrateListener,
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun initOrchestrateListeners(): DefaultOrchestrateChain<OriginReq, OriginReq, out Any> {
        val cursorAndOrchestrateListener = getAllOrchestrateListeners()

        chainOrchestrateListeners(cursorAndOrchestrateListener.second)
        chainRollbackListeners(cursorAndOrchestrateListener.second)

        addDispatcher(cursorAndOrchestrateListener.second)

        return cursorAndOrchestrateListener.first as DefaultOrchestrateChain<OriginReq, OriginReq, out Any>
    }

    private fun getAllOrchestrateListeners(): Pair<DefaultOrchestrateChain<OriginReq, out Any, out Any>?, MutableList<Pair<AbstractOrchestrateListener<out Any, out Any>, AbstractOrchestrateListener<out Any, out Any>?>>> {
        val orchestrateListeners = mutableListOf<
                Pair<AbstractOrchestrateListener<out Any, out Any>, AbstractOrchestrateListener<out Any, out Any>?>>()

        var defaultOrchestrateChainCursor: DefaultOrchestrateChain<OriginReq, out Any, out Any>? = this
        while (defaultOrchestrateChainCursor != null) {
            orchestrateListeners.add(
                defaultOrchestrateChainCursor.orchestrateListener
                        to defaultOrchestrateChainCursor.rollbackOrchestrateListener
            )
            if (defaultOrchestrateChainCursor.beforeDefaultOrchestrateChain == null) {
                break
            }
            defaultOrchestrateChainCursor = defaultOrchestrateChainCursor.beforeDefaultOrchestrateChain
        }

        orchestrateListeners.reverse()

        return defaultOrchestrateChainCursor to orchestrateListeners
    }

    private fun chainOrchestrateListeners(orchestrateListeners: List<Pair<AbstractOrchestrateListener<out Any, out Any>, AbstractOrchestrateListener<out Any, out Any>?>>) {
        for (listenerWithIdx in orchestrateListeners.withIndex()) {
            val isFirst = listenerWithIdx.index == 0
            val isLast =
                listenerWithIdx.index == (orchestrateListeners.size - 1 - COMMIT_LISTENER_PREFIX)

            val listener = listenerWithIdx.value.first
            listenerWithIdx.value.second?.let { listener.isRollbackable = true }

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
            chainContainer.transactionDispatcher.addHandler(listener)
            rollbackListener?.let { chainContainer.transactionDispatcher.addHandler(it) }
        }
    }

    private fun <T : Any, V : Any> getMonoCommitOrchestrateListener(function: OrchestrateFunction<T, Mono<V>>) =
        MonoCommitOrchestrateListener(
            codec = chainContainer.codec,
            transactionManager = chainContainer.transactionManager,
            orchestratorId = orchestratorId,
            orchestrateSequence = orchestrateSequence + 1,
            orchestrateFunction = function,
            resultHolder = chainContainer.resultHolder,
            requestHolder = chainContainer.requestHolder,
        )

    private fun <T : Any, V : Any> getMonoRollbackOrchestrateListener(rollback: RollbackFunction<T, Mono<*>>?) =
        rollback?.let {
            MonoRollbackOrchestrateListener<T, V>(
                codec = chainContainer.codec,
                transactionManager = chainContainer.transactionManager,
                orchestratorId = orchestratorId,
                orchestrateSequence = orchestrateSequence + 1,
                rollbackFunction = it,
                requestHolder = chainContainer.requestHolder,
                resultHolder = chainContainer.resultHolder,
            )
        }

    internal class Pre<T : Any> internal constructor(
        private val orchestratorId: String,
        private val transactionManager: TransactionManager,
        private val transactionDispatcher: AbstractTransactionDispatcher,
        private val codec: Codec,
        private val resultHolder: ResultHolder,
        private val requestHolder: RequestHolder,
    ): OrchestrateChain.Pre<T> {

        override fun <V : Any> start(
            function: OrchestrateFunction<T, V>,
            rollback: RollbackFunction<T, *>?,
        ): DefaultOrchestrateChain<T, T, V> {
            val startOrchestrateListener = getStartOrchestrateListener(function)
            val rollbackOrchestrateListener = getRollbackOrchestrateListener<V>(rollback)

            return DefaultOrchestrateChain(
                orchestratorId = orchestratorId,
                orchestrateSequence = 0,
                chainContainer = getStreamContainer(),
                orchestrateListener = startOrchestrateListener,
                rollbackOrchestrateListener = rollbackOrchestrateListener,
            )
        }

        private fun <V : Any> getStartOrchestrateListener(function: OrchestrateFunction<T, V>) =
            StartOrchestrateListener(
                codec = codec,
                transactionManager = transactionManager,
                orchestratorId = orchestratorId,
                orchestrateSequence = 0,
                orchestrateFunction = function,
                requestHolder = requestHolder,
                resultHolder = resultHolder,
            )

        private fun <V : Any> getRollbackOrchestrateListener(rollback: RollbackFunction<T, *>?) =
            rollback?.let {
                RollbackOrchestrateListener<T, V>(
                    codec = codec,
                    transactionManager = transactionManager,
                    orchestratorId = orchestratorId,
                    orchestrateSequence = 0,
                    rollbackFunction = it,
                    requestHolder = requestHolder,
                    resultHolder = resultHolder,
                )
            }

        override fun <V : Any> startReactive(
            function: OrchestrateFunction<T, Mono<V>>,
            rollback: RollbackFunction<T, Mono<*>>?,
        ): DefaultOrchestrateChain<T, T, V> {
            val startOrchestrateListener = getMonoStartOrchestrateListener(function)
            val rollbackOrchestrateListener = getMonoRollbackOrchestrateListener<V>(rollback)

            return DefaultOrchestrateChain(
                orchestratorId = orchestratorId,
                orchestrateSequence = 0,
                chainContainer = getStreamContainer(),
                orchestrateListener = startOrchestrateListener,
                rollbackOrchestrateListener = rollbackOrchestrateListener,
            )
        }

        private fun <V : Any> getMonoStartOrchestrateListener(function: OrchestrateFunction<T, Mono<V>>) =
            MonoStartOrchestrateListener(
                codec = codec,
                transactionManager = transactionManager,
                orchestratorId = orchestratorId,
                orchestrateSequence = 0,
                orchestrateFunction = function,
                requestHolder = requestHolder,
                resultHolder = resultHolder,
            )

        private fun <V : Any> getMonoRollbackOrchestrateListener(rollback: RollbackFunction<T, Mono<*>>?) =
            rollback?.let {
                MonoRollbackOrchestrateListener<T, V>(
                    codec = codec,
                    transactionManager = transactionManager,
                    orchestratorId = orchestratorId,
                    orchestrateSequence = 0,
                    rollbackFunction = it,
                    requestHolder = requestHolder,
                    resultHolder = resultHolder,
                )
            }

        private fun getStreamContainer(): ChainContainer = ChainContainer(
            transactionManager,
            transactionDispatcher,
            codec,
            resultHolder,
            requestHolder,
        )
    }

    companion object {
        private const val COMMIT_LISTENER_PREFIX = 1
    }

    private data class ChainContainer(
        val transactionManager: TransactionManager,
        val transactionDispatcher: AbstractTransactionDispatcher,
        val codec: Codec,
        val resultHolder: ResultHolder,
        val requestHolder: RequestHolder,
    )
}
