package org.rooftop.netx.engine

import jakarta.annotation.PostConstruct
import org.rooftop.netx.api.*
import org.rooftop.netx.engine.core.Transaction
import org.rooftop.netx.engine.core.TransactionState
import org.rooftop.netx.engine.logging.info
import org.rooftop.netx.engine.logging.warningOnError
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredMemberFunctions

abstract class AbstractTransactionDispatcher(
    private val codec: Codec,
) {

    private val functions =
        mutableMapOf<TransactionState, MutableList<AbstractDispatchFunction<*>>>()

    fun dispatch(transaction: Transaction, messageId: String): Mono<String> {
        return Flux.fromIterable(functions[transaction.state] ?: listOf())
            .flatMap { function ->
                when (function) {
                    is MonoDispatchFunction -> {
                        mapToTransactionEvent(transaction)
                            .callMono(function)
                            .warningOnError("Error occurred in TransactionHandler function \"${function.name()}\" with transaction id ${transaction.id}")
                    }

                    is NotPublishDispatchFunction -> {
                        mapToTransactionEvent(transaction)
                            .callNotPublish(function)
                            .warningOnError("Error occurred in TransactionHandler function \"${function.name()}\" with transaction id ${transaction.id}")
                    }
                }
            }
            .subscribeOn(Schedulers.boundedElastic())
            .ackWhenComplete(transaction, messageId)
            .then(Mono.just(DISPATHCED))
    }

    private fun Flux<*>.ackWhenComplete(
        transaction: Transaction,
        messageId: String
    ): Flux<*> = this.doOnComplete {
        Mono.just(transaction to messageId)
            .info("Ack transaction \"${transaction.id}\"")
            .flatMap {
                ack(transaction, messageId)
                    .warningOnError("Fail to ack transaction \"${transaction.id}\"")
            }
            .subscribeOn(Schedulers.parallel())
            .subscribe()
    }

    private fun mapToTransactionEvent(transaction: Transaction): Mono<TransactionEvent> {
        return when (transaction.state) {
            TransactionState.START -> Mono.just(
                TransactionStartEvent(
                    transactionId = transaction.id,
                    nodeName = transaction.serverId,
                    group = transaction.group,
                    event = extractEvent(transaction),
                    codec = codec,
                )
            )

            TransactionState.COMMIT -> Mono.just(
                TransactionCommitEvent(
                    transactionId = transaction.id,
                    nodeName = transaction.serverId,
                    group = transaction.group,
                    event = extractEvent(transaction),
                    codec = codec
                )
            )

            TransactionState.JOIN -> Mono.just(
                TransactionJoinEvent(
                    transactionId = transaction.id,
                    nodeName = transaction.serverId,
                    group = transaction.group,
                    event = extractEvent(transaction),
                    codec = codec,
                )
            )

            TransactionState.ROLLBACK -> findOwnUndo(transaction)
                .warningOnError("Error occurred when findOwnUndo transaction ${transaction.id}")
                .map {
                    TransactionRollbackEvent(
                        transactionId = transaction.id,
                        nodeName = transaction.serverId,
                        group = transaction.group,
                        event = extractEvent(transaction),
                        cause = transaction.cause
                            ?: throw NullPointerException("Null value on TransactionRollbackEvent's cause field"),
                        undo = it,
                        codec = codec,
                    )
                }
        }
    }

    private fun extractEvent(transaction: Transaction): String? {
        return when (transaction.event != null) {
            true -> transaction.event
            false -> null
        }
    }

    protected abstract fun findOwnUndo(transaction: Transaction): Mono<String>

    internal fun addHandler(handler: Any) {
        initMonoFunctions(listOf(handler))
        initNotPublisherFunctions(listOf(handler))
        info("Add functions : \"${handler}\"")
    }

    @PostConstruct
    fun initHandler() {
        val transactionHandler = findHandlers()
        initMonoFunctions(transactionHandler)
        initNotPublisherFunctions(transactionHandler)
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
                            val transactionState = getMatchedTransactionState(annotation)
                            val eventType = getEventType(annotation)
                            val noRetryFor = getNoRetryFor(annotation)
                            functions.putIfAbsent(transactionState, mutableListOf())
                            functions[transactionState]?.add(
                                MonoDispatchFunction(
                                    eventType,
                                    function as KFunction<Mono<*>>,
                                    handler,
                                    noRetryFor,
                                )
                            )
                        }.onFailure {
                            throw IllegalStateException("Cannot add Mono TransactionHandler", it)
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
                            val transactionState = getMatchedTransactionState(annotation)
                            val eventType = getEventType(annotation)
                            val noRetryFor = getNoRetryFor(annotation)
                            functions.putIfAbsent(transactionState, mutableListOf())
                            functions[transactionState]?.add(
                                NotPublishDispatchFunction(eventType, function, handler, noRetryFor)
                            )
                        }.onFailure {
                            throw IllegalStateException("Cannot add TransactionHandler", it)
                        }
                    }
            }
        }
    }

    protected abstract fun findHandlers(): List<Any>

    private fun getEventType(annotation: Annotation): KClass<*> {
        return when (annotation) {
            is TransactionStartListener -> annotation.event
            is TransactionCommitListener -> annotation.event
            is TransactionJoinListener -> annotation.event
            is TransactionRollbackListener -> annotation.event
            else -> throw notMatchedTransactionHandlerException
        }
    }

    private fun getNoRetryFor(annotation: Annotation): Array<KClass<out Throwable>> {
        return when (annotation) {
            is TransactionStartListener -> annotation.noRetryFor
            is TransactionCommitListener -> annotation.noRetryFor
            is TransactionJoinListener -> annotation.noRetryFor
            is TransactionRollbackListener -> annotation.noRetryFor
            else -> throw notMatchedTransactionHandlerException
        }
    }

    private fun getMatchedTransactionState(annotation: Annotation): TransactionState {
        return when (annotation) {
            is TransactionStartListener -> TransactionState.START
            is TransactionCommitListener -> TransactionState.COMMIT
            is TransactionJoinListener -> TransactionState.JOIN
            is TransactionRollbackListener -> TransactionState.ROLLBACK
            else -> throw notMatchedTransactionHandlerException
        }
    }

    protected abstract fun ack(
        transaction: Transaction,
        messageId: String
    ): Mono<Pair<Transaction, String>>

    private companion object {
        private const val DISPATHCED = "dispatched"

        private val notMatchedTransactionHandlerException =
            NotFoundDispatchFunctionException("Cannot find matched Transaction handler")
    }
}
