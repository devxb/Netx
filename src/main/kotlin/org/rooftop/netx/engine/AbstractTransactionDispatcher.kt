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

    private val monoTransactionHandleFunctions =
        mutableMapOf<TransactionState, MutableList<MonoFunction>>()

    private val notPublisherTransactionHandlerFunctions =
        mutableMapOf<TransactionState, MutableList<NotPublisherFunction>>()

    fun dispatch(transaction: Transaction, messageId: String): Boolean {
        var isSuccess = true
        if (notPublisherTransactionHandlerFunctions.isEmpty()) {
            dispatchToMonoHandler(transaction)
                .subscribeOn(Schedulers.boundedElastic())
                .ackWhenComplete(transaction, messageId)
                .subscribe({ isSuccess = true }, { isSuccess = false })
            return isSuccess
        }
        if (monoTransactionHandleFunctions.isEmpty()) {
            dispatchToNotPublisherHandler(transaction)
                .subscribeOn(Schedulers.boundedElastic())
                .ackWhenComplete(transaction, messageId)
                .subscribe({ isSuccess = true }, { isSuccess = false })
            return isSuccess
        }
        dispatchToMonoHandler(transaction)
            .flatMap { dispatchToNotPublisherHandler(transaction) }
            .subscribeOn(Schedulers.boundedElastic())
            .ackWhenComplete(transaction, messageId)
            .subscribe({ isSuccess = true }, { isSuccess = false })
        return isSuccess
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

    private fun dispatchToMonoHandler(transaction: Transaction): Flux<Any> {
        return Mono.just(transaction.state)
            .flatMapMany { state ->
                Flux.fromIterable(monoTransactionHandleFunctions[state] ?: listOf())
            }
            .publishOn(Schedulers.boundedElastic())
            .flatMap { monoFunction ->
                mapToTransactionEvent(transaction)
                    .flatMap { monoFunction.call(it) }
                    .warningOnError("Error occurred in TransactionHandler function \"${monoFunction.name()}\" with transaction \n{\n$transaction}")
            }
    }

    private fun dispatchToNotPublisherHandler(transaction: Transaction): Flux<*> {
        return Mono.just(transaction.state)
            .flatMapMany { state ->
                Flux.fromIterable(notPublisherTransactionHandlerFunctions[state] ?: listOf())
            }
            .publishOn(Schedulers.boundedElastic())
            .flatMap { notPublisherFunction ->
                mapToTransactionEvent(transaction)
                    .map { notPublisherFunction.call(it) }
                    .warningOnError("Error occurred in TransactionHandler function \"${notPublisherFunction.name()}\" with transaction \n{\n$transaction}")
            }
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
        val monoFunctions = getMonoFunctions(transactionHandler)
        monoTransactionHandleFunctions.putAll(monoFunctions)
        val notPublisherFunctions = getNotPublisherFunctions(transactionHandler)
        notPublisherTransactionHandlerFunctions.putAll(notPublisherFunctions)
    }

    @Suppress("UNCHECKED_CAST")
    private fun getMonoFunctions(
        foundHandlers: List<Any>,
    ): MutableMap<TransactionState, MutableList<MonoFunction>> {
        val handlers =
            mutableMapOf<TransactionState, MutableList<MonoFunction>>()

        for (handler in foundHandlers) {
            val returnTypeMatchedHandlers = handler::class.declaredMemberFunctions
                .filter { it.returnType.classifier == Mono::class }

            returnTypeMatchedHandlers.forEach { function ->
                function.annotations
                    .forEach { annotation ->
                        runCatching {
                            val transactionState = getMatchedTransactionState(annotation)
                            val eventType = getEventType(annotation)
                            handlers.putIfAbsent(transactionState, mutableListOf())
                            handlers[transactionState]?.add(
                                MonoFunction(eventType, function as KFunction<Mono<*>>, handler)
                            )
                        }.onFailure {
                            throw IllegalStateException("Cannot add Mono TransactionHandler", it)
                        }
                    }
            }
        }

        return handlers
    }

    private fun getNotPublisherFunctions(
        foundHandlers: List<Any>
    ): MutableMap<TransactionState, MutableList<NotPublisherFunction>> {
        val handlers =
            mutableMapOf<TransactionState, MutableList<NotPublisherFunction>>()

        for (handler in foundHandlers) {
            val returnTypeMatchedHandlers = handler::class.declaredMemberFunctions
                .filter { it.returnType.classifier != Mono::class && it.returnType.classifier != Flux::class }

            returnTypeMatchedHandlers.forEach { function ->
                function.annotations
                    .forEach { annotation ->
                        runCatching {
                            val transactionState = getMatchedTransactionState(annotation)
                            val eventType = getEventType(annotation)
                            handlers.putIfAbsent(transactionState, mutableListOf())
                            handlers[transactionState]?.add(
                                NotPublisherFunction(eventType, function, handler)
                            )
                        }.onFailure {
                            throw IllegalStateException("Cannot add TransactionHandler", it)
                        }
                    }
            }
        }

        return handlers
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

    private fun getMatchedTransactionState(annotation: Annotation): TransactionState {
        return when (annotation) {
            is TransactionStartListener -> TransactionState.TRANSACTION_STATE_START
            is TransactionCommitListener -> TransactionState.TRANSACTION_STATE_COMMIT
            is TransactionJoinListener -> TransactionState.TRANSACTION_STATE_JOIN
            is TransactionRollbackListener -> TransactionState.TRANSACTION_STATE_ROLLBACK
            else -> throw notMatchedTransactionHandlerException
        }
    }

    private class MonoFunction(
        private val eventType: KClass<*>,
        private val function: KFunction<Mono<*>>,
        private val handler: Any,
    ) {

        fun name(): String = function.name
        fun call(transactionEvent: TransactionEvent): Mono<*> {
            runCatching { transactionEvent.decodeEvent(eventType) }
                .fold(
                    onSuccess = {
                        return function.call(handler, transactionEvent)
                            .info("Call Mono TransactionHandler \"${name()}\" with transactionId \"${transactionEvent.transactionId}\"")
                    },
                    onFailure = {
                        if (it is NullPointerException && eventType == Any::class) {
                            return function.call(handler, transactionEvent)
                                .info("Call Mono TransactionHandler \"${name()}\" with transactionId \"${transactionEvent.transactionId}\"")
                        }
                    }
                )
            return Mono.empty<String>()
        }
    }

    private class NotPublisherFunction(
        private val eventType: KClass<*>,
        private val function: KFunction<*>,
        private val handler: Any,
    ) {
        fun name(): String = function.name

        fun call(transactionEvent: TransactionEvent): Any? {
            runCatching {
                transactionEvent.decodeEvent(eventType)
            }.fold(
                onSuccess = {
                    val result = function.call(handler, transactionEvent)
                    info("Call NotPublisher TransactionHandler \"${name()}\" with transactionId \"${transactionEvent.transactionId}\"")
                    return result
                },
                onFailure = {
                    if (it is NullPointerException && eventType == Any::class) {
                        val result = function.call(handler, transactionEvent)
                        info("Call NotPublisher TransactionHandler \"${name()}\" with transactionId \"${transactionEvent.transactionId}\"")
                        return result
                    }
                }
            )
            return Unit
        }
    }

    protected abstract fun ack(
        transaction: Transaction,
        messageId: String
    ): Mono<Pair<Transaction, String>>

    private companion object {
        private val cannotFindMatchedTransactionEventException =
            java.lang.IllegalStateException("Cannot find matched transaction event")

        private val notMatchedTransactionHandlerException =
            IllegalStateException("Cannot find matched Transaction handler")
    }
}
