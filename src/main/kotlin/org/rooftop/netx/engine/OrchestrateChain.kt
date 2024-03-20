package org.rooftop.netx.engine

import org.rooftop.netx.api.*
import org.rooftop.netx.engine.listen.*
import reactor.core.publisher.Mono

class OrchestrateChain<OriginReq : Any, T : Any, V : Any> private constructor(
    private val orchestratorId: String,
    private val orchestrateSequence: Int,
    private val chainContainer: ChainContainer,
    private val orchestrateListener: AbstractOrchestrateListener<T, V>,
    private val rollbackOrchestrateListener: AbstractOrchestrateListener<T, V>?,
    private val beforeOrchestrateChain: OrchestrateChain<OriginReq, out Any, T>? = null,
) {

    private var nextOrchestrateChain: OrchestrateChain<OriginReq, V, out Any>? = null

    fun <S : Any> join(
        function: OrchestrateFunction<V, S>,
        rollback: RollbackFunction<V, S?>? = null,
    ): OrchestrateChain<OriginReq, V, S> {
        val nextJoinOrchestrateListener = getJoinOrchestrateListener(function)
        val nextRollbackOrchestrateListener = getRollbackOrchestrateListener(rollback)

        val nextOrchestrateChain = OrchestrateChain(
            orchestratorId,
            orchestrateSequence + 1,
            chainContainer,
            nextJoinOrchestrateListener,
            nextRollbackOrchestrateListener,
            this,
        )
        this.nextOrchestrateChain = nextOrchestrateChain

        return nextOrchestrateChain
    }

    private fun <T : Any, V : Any> getJoinOrchestrateListener(function: OrchestrateFunction<T, V>) =
        JoinOrchestrateListener(
            codec = chainContainer.codec,
            transactionManager = chainContainer.transactionManager,
            orchestratorId = orchestratorId,
            orchestrateSequence = orchestrateSequence + 1,
            orchestrateFunction = function
        )

    fun <S : Any> joinReactive(
        function: OrchestrateFunction<V, Mono<S>>,
        rollback: RollbackFunction<V, Mono<S?>>? = null,
    ): OrchestrateChain<OriginReq, V, S> {
        val nextJoinOrchestrateListener = getMonoJoinOrchestrateListener(function)
        val nextRollbackOrchestrateListener = getMonoRollbackOrchestrateListener(rollback)

        val nextOrchestrateChain = OrchestrateChain<OriginReq, V, S>(
            orchestratorId,
            orchestrateSequence + 1,
            chainContainer,
            nextJoinOrchestrateListener,
            nextRollbackOrchestrateListener,
            this,
        )
        this.nextOrchestrateChain = nextOrchestrateChain

        return nextOrchestrateChain
    }

    private fun <T : Any, V : Any> getMonoJoinOrchestrateListener(function: OrchestrateFunction<T, Mono<V>>) =
        MonoJoinOrchestrateListener(
            codec = chainContainer.codec,
            transactionManager = chainContainer.transactionManager,
            orchestratorId = orchestratorId,
            orchestrateSequence = orchestrateSequence + 1,
            orchestrateFunction = function
        )

    fun <S : Any> commit(
        function: OrchestrateFunction<V, S>,
        rollback: RollbackFunction<V, S?>? = null,
    ): Orchestrator<OriginReq, S> {
        val nextCommitOrchestrateListener = getCommitOrchestrateListener(function)
        val nextRollbackOrchestrateListener = getRollbackOrchestrateListener(rollback)

        val nextOrchestrateChain = OrchestrateChain(
            orchestratorId,
            orchestrateSequence + 1,
            chainContainer,
            nextCommitOrchestrateListener,
            nextRollbackOrchestrateListener,
            this,
        )
        this.nextOrchestrateChain = nextOrchestrateChain
        val firstOrchestrateChain = nextOrchestrateChain.chainOrchestrateListeners()

        return OrchestratorManager(
            transactionManager = chainContainer.transactionManager,
            codec = chainContainer.codec,
            orchestratorId = orchestratorId,
            orchestrateResultHolder = chainContainer.orchestrateResultHolder,
            orchestrateListener = firstOrchestrateChain.orchestrateListener,
            rollbackOrchestrateListener = firstOrchestrateChain.rollbackOrchestrateListener,
        )
    }

    private fun <T : Any, V : Any> getCommitOrchestrateListener(function: OrchestrateFunction<T, V>) =
        CommitOrchestrateListener(
            codec = chainContainer.codec,
            transactionManager = chainContainer.transactionManager,
            orchestratorId = orchestratorId,
            orchestrateSequence = orchestrateSequence + 1,
            orchestrateFunction = function,
            orchestrateResultHolder = chainContainer.orchestrateResultHolder,
        )

    private fun <T : Any, V : Any> getRollbackOrchestrateListener(rollback: RollbackFunction<T, V?>?) =
        rollback?.let {
            RollbackOrchestrateListener(
                codec = chainContainer.codec,
                transactionManager = chainContainer.transactionManager,
                orchestratorId = orchestratorId,
                orchestrateSequence = orchestrateSequence + 1,
                rollbackFunction = it,
            )
        }

    fun <S : Any> commitReactive(
        function: OrchestrateFunction<V, Mono<S>>,
        rollback: RollbackFunction<V, Mono<S?>>? = null,
    ): Orchestrator<OriginReq, S> {
        val nextJoinOrchestrateListener = getMonoCommitOrchestrateListener(function)
        val nextRollbackOrchestrateListener = getMonoRollbackOrchestrateListener(rollback)

        val nextOrchestrateChain = OrchestrateChain(
            orchestratorId,
            orchestrateSequence + 1,
            chainContainer,
            nextJoinOrchestrateListener,
            nextRollbackOrchestrateListener,
            this,
        )
        this.nextOrchestrateChain = nextOrchestrateChain

        val firstOrchestrateChain = nextOrchestrateChain.chainOrchestrateListeners()

        return OrchestratorManager(
            transactionManager = chainContainer.transactionManager,
            codec = chainContainer.codec,
            orchestratorId = orchestratorId,
            orchestrateResultHolder = chainContainer.orchestrateResultHolder,
            orchestrateListener = firstOrchestrateChain.orchestrateListener,
            rollbackOrchestrateListener = firstOrchestrateChain.rollbackOrchestrateListener,
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun chainOrchestrateListeners(): OrchestrateChain<OriginReq, OriginReq, out Any> {
        val orchestrateListeners = mutableListOf<
                Pair<AbstractOrchestrateListener<out Any, out Any>, AbstractOrchestrateListener<out Any, out Any>?>>()

        var orchestrateChainCursor: OrchestrateChain<OriginReq, out Any, out Any>? = this
        while (orchestrateChainCursor != null) {
            orchestrateListeners.add(
                orchestrateChainCursor.orchestrateListener
                        to orchestrateChainCursor.rollbackOrchestrateListener
            )
            if (orchestrateChainCursor.beforeOrchestrateChain == null) {
                break
            }
            orchestrateChainCursor = orchestrateChainCursor.beforeOrchestrateChain
        }

        orchestrateListeners.reverse()

        for (listenerWithIdx in orchestrateListeners.withIndex()) {
            val isFirst = listenerWithIdx.index == 0
            val isLast =
                listenerWithIdx.index == (orchestrateListeners.size - 1 - COMMIT_LISTENER_PREFIX)

            val listener = listenerWithIdx.value.first

            listener.isFirst = isFirst
            listener.isLast = isLast
            if (listenerWithIdx.index < orchestrateListeners.size - 1) {
                val nextListener = orchestrateListeners[listenerWithIdx.index + 1]
                listener.setNextOrchestrateListener(nextListener.first)
                nextListener.second?.let { listener.setNextRollbackOrchestrateListener(it) }
            }
        }

        val rollbackListeners = orchestrateListeners.asSequence()
            .map { it.second }
            .filter { it != null }
            .toList()

        for (rollbackListenerWithIdx in rollbackListeners.withIndex()) {
            val rollbackListener = rollbackListenerWithIdx.value
                ?: throw IllegalStateException("Null Rollback listener occurred.")

            rollbackListener.isFirst = rollbackListenerWithIdx.index == 0
            rollbackListener.isLast = rollbackListenerWithIdx.index == (rollbackListeners.size - 1)
        }

        orchestrateListeners.forEach { (listener, rollbackListener) ->
            chainContainer.transactionDispatcher.addHandler(listener)
            rollbackListener?.let { chainContainer.transactionDispatcher.addHandler(it) }
        }

        return orchestrateChainCursor as OrchestrateChain<OriginReq, OriginReq, out Any>
    }

    private fun <T : Any, V : Any> getMonoCommitOrchestrateListener(function: OrchestrateFunction<T, Mono<V>>) =
        MonoCommitOrchestrateListener(
            codec = chainContainer.codec,
            transactionManager = chainContainer.transactionManager,
            orchestratorId = orchestratorId,
            orchestrateSequence = orchestrateSequence + 1,
            orchestrateFunction = function,
            orchestrateResultHolder = chainContainer.orchestrateResultHolder,
        )

    private fun <T : Any, V : Any> getMonoRollbackOrchestrateListener(rollback: RollbackFunction<T, Mono<V?>>?) =
        rollback?.let {
            MonoRollbackOrchestrateListener(
                codec = chainContainer.codec,
                transactionManager = chainContainer.transactionManager,
                orchestratorId = orchestratorId,
                orchestrateSequence = orchestrateSequence + 1,
                rollbackFunction = it,
            )
        }

    class Pre<T : Any> internal constructor(
        private val orchestratorId: String,
        private val transactionManager: TransactionManager,
        private val transactionDispatcher: AbstractTransactionDispatcher,
        private val codec: Codec,
        private val orchestrateResultHolder: OrchestrateResultHolder,
    ) {

        fun <V : Any> start(
            function: OrchestrateFunction<T, V>,
            rollback: RollbackFunction<T, V?>? = null,
        ): OrchestrateChain<T, T, V> {
            val startOrchestrateListener = getStartOrchestrateListener(function)
            val rollbackOrchestrateListener = getRollbackOrchestrateListener(rollback)

            return OrchestrateChain(
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
                orchestrateFunction = function
            )

        private fun <V : Any> getRollbackOrchestrateListener(rollback: RollbackFunction<T, V?>?) =
            rollback?.let {
                RollbackOrchestrateListener(
                    codec = codec,
                    transactionManager = transactionManager,
                    orchestratorId = orchestratorId,
                    orchestrateSequence = 0,
                    rollbackFunction = it,
                )
            }

        fun <V : Any> startReactive(
            function: OrchestrateFunction<T, Mono<V>>,
            rollback: RollbackFunction<T, Mono<V?>>? = null,
        ): OrchestrateChain<T, T, V> {
            val startOrchestrateListener = getMonoStartOrchestrateListener(function)
            val rollbackOrchestrateListener = getMonoRollbackOrchestrateListener(rollback)

            return OrchestrateChain(
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
                orchestrateFunction = function
            )

        private fun <V : Any> getMonoRollbackOrchestrateListener(rollback: RollbackFunction<T, Mono<V?>>?) =
            rollback?.let {
                MonoRollbackOrchestrateListener(
                    codec = codec,
                    transactionManager = transactionManager,
                    orchestratorId = orchestratorId,
                    orchestrateSequence = 0,
                    rollbackFunction = it,
                )
            }

        private fun getStreamContainer(): ChainContainer = ChainContainer(
            transactionManager,
            transactionDispatcher,
            codec,
            orchestrateResultHolder
        )
    }

    companion object {
        private const val COMMIT_LISTENER_PREFIX = 1
    }

    private data class ChainContainer(
        val transactionManager: TransactionManager,
        val transactionDispatcher: AbstractTransactionDispatcher,
        val codec: Codec,
        val orchestrateResultHolder: OrchestrateResultHolder,
    )
}
