package org.rooftop.netx.engine

import org.rooftop.netx.api.AlreadyCommittedSagaException
import org.rooftop.netx.api.Codec
import org.rooftop.netx.api.SagaException
import org.rooftop.netx.api.SagaManager
import org.rooftop.netx.engine.core.Saga
import org.rooftop.netx.engine.core.SagaState
import org.rooftop.netx.engine.logging.info
import org.rooftop.netx.engine.logging.infoOnError
import org.rooftop.netx.engine.logging.warningOnError
import reactor.core.publisher.Mono

internal abstract class AbstractSagaManager(
    private val codec: Codec,
    private val nodeGroup: String,
    private val nodeName: String,
    private val sagaIdGenerator: SagaIdGenerator,
) : SagaManager {

    override fun syncStart(): String {
        return start().block()
            ?: throw SagaException("Cannot start saga")
    }

    final override fun <T : Any> syncStart(event: T): String {
        return start(event).block()
            ?: throw SagaException("Cannot start saga \"$event\"")
    }

    override fun syncJoin(id: String): String {
        return join(id).block()
            ?: throw SagaException("Cannot join saga \"$id\"")
    }

    final override fun <T : Any> syncJoin(id: String, event: T): String {
        return join(id, event).block()
            ?: throw SagaException("Cannot join saga \"$id\", \"$event\"")
    }

    final override fun syncExists(id: String): String {
        return exists(id).block()
            ?: throw SagaException("Cannot exists saga \"$id\"")
    }

    final override fun syncCommit(id: String): String {
        return commit(id).block()
            ?: throw SagaException("Cannot commit saga \"$id\"")
    }

    override fun <T : Any> syncCommit(id: String, event: T): String {
        return commit(id, event).block()
            ?: throw SagaException("Cannot commit saga \"$id\" \"$event\"")
    }

    final override fun syncRollback(id: String, cause: String): String {
        return rollback(id, cause).block()
            ?: throw SagaException("Cannot rollback saga \"$id\", \"$cause\"")
    }

    override fun <T : Any> syncRollback(id: String, cause: String, event: T): String {
        return rollback(id, cause, event).block()
            ?: throw SagaException("Cannot rollback saga \"$id\", \"$cause\" \"$event\"")
    }

    override fun start(): Mono<String> {
        return startSaga(null)
            .info("Start saga")
            .contextWrite { it.put(CONTEXT_TX_KEY, sagaIdGenerator.generate()) }
    }

    final override fun <T : Any> start(event: T): Mono<String> {
        return Mono.fromCallable { codec.encode(event) }
            .flatMap { encodedEvent ->
                startSaga(encodedEvent)
                    .info("Start saga event \"$event\"")
            }
            .contextWrite { it.put(CONTEXT_TX_KEY, sagaIdGenerator.generate()) }
    }

    private fun startSaga(event: String?): Mono<String> {
        return Mono.deferContextual<String> { Mono.just(it[CONTEXT_TX_KEY]) }
            .flatMap { id ->
                publishSaga(
                    id, Saga(
                        id = id,
                        serverId = nodeName,
                        group = nodeGroup,
                        state = SagaState.START,
                        event = event,
                    )
                )
            }
    }

    override fun join(id: String): Mono<String> {
        return getAnySaga(id)
            .map {
                if (it == SagaState.COMMIT) {
                    throw AlreadyCommittedSagaException(id, it.name)
                }
                id
            }
            .warningOnError("Cannot join saga")
            .flatMap {
                joinSaga(id, null)
                    .info("Join saga id \"$id\"")
            }
    }

    override fun <T : Any> join(id: String, event: T): Mono<String> {
        return getAnySaga(id)
            .map {
                if (it == SagaState.COMMIT) {
                    throw AlreadyCommittedSagaException(id, it.name)
                }
                id
            }
            .warningOnError("Cannot join saga")
            .map { codec.encode(event) }
            .flatMap {
                joinSaga(id, it)
                    .info("Join saga id \"$id\", event \"$event\"")
            }
    }

    private fun joinSaga(id: String, event: String?): Mono<String> {
        return publishSaga(
            id, Saga(
                id = id,
                serverId = nodeName,
                group = nodeGroup,
                state = SagaState.JOIN,
                event = event,
            )
        )
    }

    final override fun rollback(id: String, cause: String): Mono<String> {
        return exists(id)
            .infoOnError("Cannot rollback saga cause, saga \"$id\" is not exists")
            .flatMap {
                rollbackSaga(id, cause, null)
            }
            .info("Rollback saga \"$id\"")
            .contextWrite { it.put(CONTEXT_TX_KEY, id) }
    }

    override fun <T : Any> rollback(id: String, cause: String, event: T): Mono<String> {
        return exists(id)
            .infoOnError("Cannot rollback saga cause, saga \"$id\" is not exists")
            .map { codec.encode(event) }
            .flatMap { encodedEvent ->
                rollbackSaga(id, cause, encodedEvent)
            }
            .info("Rollback saga \"$id\"")
            .contextWrite { it.put(CONTEXT_TX_KEY, id) }
    }

    private fun rollbackSaga(
        id: String,
        cause: String,
        event: String?
    ): Mono<String> {
        return publishSaga(
            id, Saga(
                id = id,
                serverId = nodeName,
                group = nodeGroup,
                state = SagaState.ROLLBACK,
                cause = cause,
                event = event
            )
        )
    }

    final override fun commit(id: String): Mono<String> {
        return exists(id)
            .infoOnError("Cannot commit saga cause, saga \"$id\" is not exists")
            .flatMap { commitSaga(id, null) }
            .info("Commit saga \"$id\"")
            .contextWrite { it.put(CONTEXT_TX_KEY, id) }
    }

    override fun <T : Any> commit(id: String, event: T): Mono<String> {
        return exists(id)
            .infoOnError("Cannot commit saga cause, saga \"$id\" is not exists")
            .map { codec.encode(event) }
            .flatMap { encodedEvent ->
                commitSaga(id, encodedEvent)
            }
            .info("Commit saga \"$id\"")
            .contextWrite { it.put(CONTEXT_TX_KEY, id) }
    }

    private fun commitSaga(id: String, event: String?): Mono<String> {
        return publishSaga(
            id, Saga(
                id = id,
                serverId = nodeName,
                group = nodeGroup,
                state = SagaState.COMMIT,
                event = event,
            )
        )
    }

    final override fun exists(id: String): Mono<String> {
        return getAnySaga(id)
            .infoOnError("There is no saga corresponding to id \"$id\"")
            .mapSagaId()
            .contextWrite { it.put(CONTEXT_TX_KEY, id) }
    }

    protected abstract fun getAnySaga(id: String): Mono<SagaState>

    private fun Mono<*>.mapSagaId(): Mono<String> {
        return this.flatMap {
            Mono.deferContextual { Mono.just(it[CONTEXT_TX_KEY]) }
        }
    }

    protected abstract fun publishSaga(
        id: String,
        saga: Saga,
    ): Mono<String>

    private companion object {
        private const val CONTEXT_TX_KEY = "sagaId"
    }
}
