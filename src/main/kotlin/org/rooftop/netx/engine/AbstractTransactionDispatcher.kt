package org.rooftop.netx.engine

import jakarta.annotation.PostConstruct
import org.rooftop.netx.api.*
import org.rooftop.netx.engine.logging.info
import org.rooftop.netx.engine.logging.warningOnError
import org.rooftop.netx.idl.Transaction
import org.rooftop.netx.idl.TransactionState
import org.rooftop.netx.meta.TransactionHandler
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
            .info("Ack transaction \n{\n$transaction}\nmessageId \"$messageId\"")
            .flatMap {
                ack(transaction, messageId)
                    .warningOnError("Fail to ack transaction \n{\n$transaction}\nmessageId \"$messageId\"")
            }
            .subscribeOn(Schedulers.parallel())
            .subscribe()
    }

    private fun mapToTransactionEvent(transaction: Transaction): Mono<TransactionEvent> {
        return when (transaction.state) {
            TransactionState.TRANSACTION_STATE_START -> Mono.just(
                TransactionStartEvent(
                    transactionId = transaction.id,
                    nodeName = transaction.serverId,
                    group = transaction.group,
                    event = extractEvent(transaction),
                    codec = codec,
                )
            )

            TransactionState.TRANSACTION_STATE_COMMIT -> Mono.just(
                TransactionCommitEvent(
                    transactionId = transaction.id,
                    nodeName = transaction.serverId,
                    group = transaction.group,
                    event = extractEvent(transaction),
                    codec = codec
                )
            )

            TransactionState.TRANSACTION_STATE_JOIN -> Mono.just(
                TransactionJoinEvent(
                    transactionId = transaction.id,
                    nodeName = transaction.serverId,
                    group = transaction.group,
                    event = extractEvent(transaction),
                    codec = codec,
                )
            )

            TransactionState.TRANSACTION_STATE_ROLLBACK -> findOwnUndo(transaction)
                .warningOnError("Error occurred when findOwnUndo transaction \n{\n$transaction}")
                .map {
                    TransactionRollbackEvent(
                        transactionId = transaction.id,
                        nodeName = transaction.serverId,
                        group = transaction.group,
                        event = extractEvent(transaction),
                        cause = when (transaction.hasCause()) {
                            true -> transaction.cause
                            false -> null
                        },
                        undo = it,
                        codec = codec,
                    )
                }

            else -> throw cannotFindMatchedTransactionEventException
        }
    }

    private fun extractEvent(transaction: Transaction): String? {
        return when (transaction.hasEvent()) {
            true -> transaction.event
            false -> null
        }
    }

    protected abstract fun findOwnUndo(transaction: Transaction): Mono<String>

    @PostConstruct
    fun initHandler() {
        val transactionHandler = findHandlers(TransactionHandler::class)
        initMonoFunctions(transactionHandler)
        initNotPublisherFunctions(transactionHandler)
        functions.forEach { (_, notPublisherFunction) ->
            val notPublisherFunctionNames = notPublisherFunction.map { it.name() }.toList()
            info("Register functions names : \"${notPublisherFunctionNames}\"")
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

    protected abstract fun <T : Annotation> findHandlers(type: KClass<T>): List<Any>

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
            is TransactionStartListener -> TransactionState.TRANSACTION_STATE_START
            is TransactionCommitListener -> TransactionState.TRANSACTION_STATE_COMMIT
            is TransactionJoinListener -> TransactionState.TRANSACTION_STATE_JOIN
            is TransactionRollbackListener -> TransactionState.TRANSACTION_STATE_ROLLBACK
            else -> throw notMatchedTransactionHandlerException
        }
    }

    protected abstract fun ack(
        transaction: Transaction,
        messageId: String
    ): Mono<Pair<Transaction, String>>

    private companion object {
        private const val DISPATHCED = "dispatched"

        private val cannotFindMatchedTransactionEventException =
            java.lang.IllegalStateException("Cannot find matched transaction event")

        private val notMatchedTransactionHandlerException =
            IllegalStateException("Cannot find matched Transaction handler")
    }
}
