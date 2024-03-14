package org.rooftop.netx.engine

import org.rooftop.netx.api.Codec
import org.rooftop.netx.api.OrchestrateFunction
import org.rooftop.netx.api.TransactionManager
import org.rooftop.netx.engine.listen.*
import reactor.core.publisher.Mono
import kotlin.reflect.KClass

enum class SupportsOrchestrateFunctionType {
    DEFAULT,
    MONO,
}

class StartOrchestrateFunctionHolder<T : Any>(
    private val type: SupportsOrchestrateFunctionType,
    private val noRollbackFor: Array<out KClass<out Throwable>>,
    private val function: OrchestrateFunction<T>,
) {
    @Suppress("UNCHECKED_CAST")
    fun toOrchestrateOperator(
        isLast: Boolean,
        codec: Codec,
        transactionManager: TransactionManager,
        orchestrateId: String,
    ): Any {
        if (type == SupportsOrchestrateFunctionType.MONO) {
            return MonoStartOrchestrateListener(
                isLast = isLast,
                codec = codec,
                transactionManager = transactionManager,
                orchestrateId = orchestrateId,
                orchestrateSequence = 0,
                orchestrateFunction = function as OrchestrateFunction<Mono<Any>>,
                noRollbackFor = noRollbackFor,
            )
        }
        return StartOrchestrateListener(
            isLast = isLast,
            codec = codec,
            transactionManager = transactionManager,
            orchestrateId = orchestrateId,
            orchestrateSequence = 0,
            orchestrateFunction = function as OrchestrateFunction<Any>,
            noRollbackFor = noRollbackFor,
        )
    }
}


class JoinOrchestrateFunctionHolder<T : Any>(
    private val type: SupportsOrchestrateFunctionType,
    private val noRollbackFor: Array<out KClass<out Throwable>>,
    private val function: OrchestrateFunction<T>,
) {
    @Suppress("UNCHECKED_CAST")
    fun toOrchestrateOperator(
        isLast: Boolean,
        codec: Codec,
        transactionManager: TransactionManager,
        orchestrateId: String,
        orchestrateSequence: Int
    ): Any {
        if (type == SupportsOrchestrateFunctionType.MONO) {
            return MonoJoinOrchestrateListener(
                isLast = isLast,
                codec = codec,
                transactionManager = transactionManager,
                orchestrateId = orchestrateId,
                orchestrateSequence = orchestrateSequence,
                orchestrateFunction = function as OrchestrateFunction<Mono<Any>>,
                noRollbackFor = noRollbackFor,
            )
        }
        return JoinOrchestrateListener(
            isLast = isLast,
            codec = codec,
            transactionManager = transactionManager,
            orchestrateId = orchestrateId,
            orchestrateSequence = orchestrateSequence,
            orchestrateFunction = function as OrchestrateFunction<Any>,
            noRollbackFor = noRollbackFor,
        )
    }
}

class CommitOrchestrateFunctionHolder<T : Any>(
    private val type: SupportsOrchestrateFunctionType,
    private val noRollbackFor: Array<out KClass<out Throwable>>,
    private val function: OrchestrateFunction<T>,
) {
    @Suppress("UNCHECKED_CAST")
    fun toOrchestrateOperator(
        codec: Codec,
        transactionManager: TransactionManager,
        orchestrateId: String,
        orchestrateResultHolder: OrchestrateResultHolder,
    ): Any {
        if (type == SupportsOrchestrateFunctionType.MONO) {
            return MonoCommitOrchestrateListener(
                codec = codec,
                transactionManager = transactionManager,
                orchestrateId = orchestrateId,
                orchestrateFunction = function as OrchestrateFunction<Mono<Any>>,
                orchestrateResultHolder = orchestrateResultHolder,
                noRollbackFor = noRollbackFor,
            )
        }
        return CommitOrchestrateListener(
            codec = codec,
            transactionManager = transactionManager,
            orchestrateId = orchestrateId,
            orchestrateFunction = function as OrchestrateFunction<Any>,
            orchestrateResultHolder = orchestrateResultHolder,
            noRollbackFor = noRollbackFor,
        )
    }
}


class RollbackOrchestrateFunctionHolder<T : Any>(
    private val type: SupportsOrchestrateFunctionType,
    private val function: OrchestrateFunction<T>
) {
    @Suppress("UNCHECKED_CAST")
    fun toOrchestrateOperator(
        codec: Codec,
        orchestrateId: String,
        orchestrateResultHolder: OrchestrateResultHolder,
    ): Any {
        if (type == SupportsOrchestrateFunctionType.MONO) {
            return MonoRollbackOrchestrateListener(
                codec = codec,
                orchestrateId = orchestrateId,
                orchestrateFunction = function as OrchestrateFunction<Mono<Any>>,
                orchestrateResultHolder = orchestrateResultHolder,
            )
        }
        return RollbackOrchestrateListener(
            codec = codec,
            orchestrateId = orchestrateId,
            orchestrateFunction = function as OrchestrateFunction<Any>,
            orchestrateResultHolder = orchestrateResultHolder,
        )
    }
}
