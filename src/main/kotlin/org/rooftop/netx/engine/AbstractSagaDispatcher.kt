package org.rooftop.netx.engine

import jakarta.annotation.PostConstruct
import org.rooftop.netx.api.*
import org.rooftop.netx.core.Codec
import org.rooftop.netx.engine.core.Saga
import org.rooftop.netx.engine.core.SagaState
import org.rooftop.netx.engine.deadletter.AbstractDeadLetterManager
import org.rooftop.netx.engine.logging.info
import org.rooftop.netx.engine.logging.warningOnError
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredMemberFunctions

internal abstract class AbstractSagaDispatcher(
    private val codec: Codec,
    private val sagaManager: SagaManager,
    private val abstractDeadLetterManager: AbstractDeadLetterManager,
) {

    init {
        this.also { abstractDeadLetterManager.dispatcher = it }
    }

    private val functions =
        mutableMapOf<SagaState, MutableList<AbstractDispatchFunction<*>>>()

    fun dispatch(saga: Saga, messageId: String): Mono<String> {
        return Flux.fromIterable(functions[saga.state] ?: listOf())
            .flatMap { function ->
                when (function) {
                    is MonoDispatchFunction -> {
                        mapSagaEvent(saga.copy())
                            .callMono(function)
                            .warningOnError("Error occurred in SagaHandler function \"${function.name()}\" with saga id ${saga.id}")
                    }

                    is NotPublishDispatchFunction -> {
                        mapSagaEvent(saga.copy())
                            .callNotPublish(function)
                            .warningOnError("Error occurred in SagaHandler function \"${function.name()}\" with saga id ${saga.id}")
                    }

                    is OrchestrateDispatchFunction -> {
                        mapSagaEvent(saga.copy())
                            .callOrchestrate(function)
                            .warningOnError("Error occurred in SagaHandler function \"${function.name()}\" with saga id ${saga.id}")
                    }
                }
            }
            .subscribeOn(Schedulers.boundedElastic())
            .ackWhenComplete(saga, messageId)
            .then(Mono.just(DISPATCHED))
    }

    private fun Flux<*>.ackWhenComplete(
        saga: Saga,
        messageId: String
    ): Flux<*> = this.doOnComplete {
        Mono.just(saga to messageId)
            .filter { messageId != DEAD_LETTER }
            .info("Ack saga \"${saga.id}\" messageId: $messageId")
            .flatMap {
                ack(saga, messageId)
                    .warningOnError("Fail to ack saga \"${saga.id}\"")
            }
            .subscribeOn(Schedulers.parallel())
            .subscribe()
    }

    private fun mapSagaEvent(saga: Saga): Mono<SagaEvent> {
        return Mono.just(saga.toEvent(codec))
    }

    internal fun addOrchestrate(handler: Any) {
        addOrchestrateFunctions(handler)
        info("Add orchestrate fucntion : \"${handler}\"")
    }

    @Suppress("UNCHECKED_CAST")
    private fun addOrchestrateFunctions(handler: Any) {
        val returnTypeMatchedHandlers = handler::class.declaredMemberFunctions
            .filter { it.returnType.classifier == Mono::class }

        returnTypeMatchedHandlers.forEach { function ->
            function.annotations
                .forEach { annotation ->
                    runCatching {
                        val sagaState = getMatchedSagaState(annotation)
                        val eventType = getEventType(annotation)
                        val noRollbackFor = getNoRollbackFor(annotation)
                        val nextState = getNextSagaState(annotation)
                        functions.putIfAbsent(sagaState, mutableListOf())
                        functions[sagaState]?.add(
                            OrchestrateDispatchFunction(
                                eventType,
                                function as KFunction<Mono<*>>,
                                handler,
                                noRollbackFor,
                                nextState,
                                sagaManager,
                                abstractDeadLetterManager,
                            )
                        )
                    }.onFailure {
                        throw IllegalStateException("Cannot add Mono SagaHandler", it)
                    }
                }
        }
    }

    @PostConstruct
    fun initHandler() {
        val sagaHandlers = findHandlers()
        initMonoFunctions(sagaHandlers)
        initNotPublisherFunctions(sagaHandlers)
        functions.forEach { (_, function) ->
            val functionName = function.map { it.name() }.toList()
            info("Register functions names : \"${functionName}\"")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun initMonoFunctions(
        foundHandlers: List<Any>,
    ) {
        for (handler in foundHandlers) {
            val returnTypeMatchedHandlers = handler::class.declaredMemberFunctions
                .filter { it.returnType.classifier == Mono::class }

            returnTypeMatchedHandlers.forEach { function ->
                function.annotations
                    .forEach { annotation ->
                        runCatching {
                            val sagaState = getMatchedSagaState(annotation)
                            val eventType = getEventType(annotation)
                            val noRollbackFor = getNoRollbackFor(annotation)
                            val nextState = getNextSagaState(annotation)
                            functions.putIfAbsent(sagaState, mutableListOf())
                            functions[sagaState]?.add(
                                MonoDispatchFunction(
                                    eventType,
                                    function as KFunction<Mono<*>>,
                                    handler,
                                    noRollbackFor,
                                    nextState,
                                    sagaManager,
                                    abstractDeadLetterManager,
                                )
                            )
                        }.onFailure {
                            throw IllegalStateException("Cannot add Mono SagaHandler", it)
                        }
                    }
            }
        }
    }

    private fun initNotPublisherFunctions(
        foundHandlers: List<Any>
    ) {

        for (handler in foundHandlers) {
            val returnTypeMatchedHandlers = handler::class.declaredMemberFunctions
                .filter { it.returnType.classifier != Mono::class && it.returnType.classifier != Flux::class }

            returnTypeMatchedHandlers.forEach { function ->
                function.annotations
                    .forEach { annotation ->
                        runCatching {
                            val sagaState = getMatchedSagaState(annotation)
                            val eventType = getEventType(annotation)
                            val noRollbackFor = getNoRollbackFor(annotation)
                            val nextState = getNextSagaState(annotation)
                            functions.putIfAbsent(sagaState, mutableListOf())
                            functions[sagaState]?.add(
                                NotPublishDispatchFunction(
                                    eventType,
                                    function,
                                    handler,
                                    noRollbackFor,
                                    nextState,
                                    sagaManager,
                                    abstractDeadLetterManager,
                                )
                            )
                        }.onFailure {
                            throw IllegalStateException("Cannot add SagaHandler", it)
                        }
                    }
            }
        }
    }

    protected abstract fun findHandlers(): List<Any>

    private fun getEventType(annotation: Annotation): KClass<*> {
        return when (annotation) {
            is SagaStartListener -> annotation.event
            is SagaCommitListener -> annotation.event
            is SagaJoinListener -> annotation.event
            is SagaRollbackListener -> annotation.event
            else -> throw notMatchedSagaHandlerException
        }
    }

    private fun getNoRollbackFor(annotation: Annotation): Array<KClass<out Throwable>> {
        return when (annotation) {
            is SagaStartListener -> annotation.noRollbackFor
            is SagaCommitListener -> annotation.noRollbackFor
            is SagaJoinListener -> annotation.noRollbackFor
            is SagaRollbackListener -> emptyArray()
            else -> throw notMatchedSagaHandlerException
        }
    }

    private fun getMatchedSagaState(annotation: Annotation): SagaState {
        return when (annotation) {
            is SagaStartListener -> SagaState.START
            is SagaCommitListener -> SagaState.COMMIT
            is SagaJoinListener -> SagaState.JOIN
            is SagaRollbackListener -> SagaState.ROLLBACK
            else -> throw notMatchedSagaHandlerException
        }
    }

    private fun getNextSagaState(annotation: Annotation): AbstractDispatchFunction.NextSagaState {
        return when (annotation) {
            is SagaStartListener -> annotation.successWith.toNextSagaState()
            is SagaJoinListener -> annotation.successWith.toNextSagaState()
            else -> AbstractDispatchFunction.NextSagaState.END
        }
    }

    private fun SuccessWith.toNextSagaState(): AbstractDispatchFunction.NextSagaState {
        return when (this) {
            SuccessWith.PUBLISH_JOIN -> AbstractDispatchFunction.NextSagaState.JOIN
            SuccessWith.PUBLISH_COMMIT -> AbstractDispatchFunction.NextSagaState.COMMIT
            SuccessWith.END -> AbstractDispatchFunction.NextSagaState.END
        }
    }

    protected abstract fun ack(
        saga: Saga,
        messageId: String
    ): Mono<Pair<Saga, String>>

    internal companion object {
        internal const val DEAD_LETTER = "dead letter"
        private const val DISPATCHED = "dispatched"

        private val notMatchedSagaHandlerException =
            NotFoundDispatchFunctionException("Cannot find matched Saga handler")
    }
}
