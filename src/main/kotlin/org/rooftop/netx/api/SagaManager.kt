package org.rooftop.netx.api

import reactor.core.publisher.Mono

/**
 * Interface for managing the state of a saga (Start, Join, Commit, Rollback) and publishing events.
 * Saga events can be received by corresponding Saga...Listener.
 *
 * Example.
 *
 *      fun startSaga(foo: Foo) {
 *          sagaManager.startSync(foo)
 *      }
 *
 *      fun joinSaga(sagaId: String, foo: Foo) {
 *          sagaManager.joinSync(sagaId, foo)
 *      }
 *
 *      fun commitSaga(sagaId: String, foo: Foo) {
 *          sagaManager.commitSync(sagaId, foo)
 *      }
 *
 *      fun rollbackSaga(sagaId: String, foo: Foo) {
 *          sagaManager.rollbackSync(sagaId, "Rollback cause invalid name", foo)
 *      }
 *
 */
interface SagaManager {

    /**
     * Start a saga and publish SagaStartEvent.
     *
     * @see SagaStartEvent
     * @see SagaStartListener
     * @return String | unique saga id
     */
    fun start(): Mono<String>

    /**
     * @see start
     * @param T event
     */
    fun <T : Any> start(event: T): Mono<String>

    /**
     * @see start
     */
    fun startSync(): String

    /**
     * @see start
     * @param T event
     */
    fun <T : Any> startSync(event: T): String

    /**
     * Join the saga identified by id and publish SagaJoinEvent.
     *
     * @see SagaJoinEvent
     * @see SagaJoinListener
     * @param id started saga id
     * @return String | unique saga id
     * @throws SagaException
     * @throws AlreadyCommittedSagaException
     */
    fun join(id: String): Mono<String>

    /**
     * @see join
     * @param T event
     */
    fun <T : Any> join(id: String, event: T): Mono<String>

    /**
     * @see join
     */
    fun joinSync(id: String): String

    /**
     * @see join
     * @param T event
     */
    fun <T : Any> joinSync(id: String, event: T): String

    /**
     * Check if the saga identified by id exists.
     *
     * @see SagaException
     * @param id started saga id
     * @return String | unique saga id
     */
    fun exists(id: String): Mono<String>

    /**
     * @see exists
     */
    fun existsSync(id: String): String

    /**
     * Commit the saga identified by id and publish SagaCommitEvent.
     *
     * @see SagaCommitEvent
     * @see SagaCommitListener
     * @param id started saga id
     * @return String | unique saga id
     * @throws SagaException
     */
    fun commit(id: String): Mono<String>

    /**
     * @see commit
     * @param T event
     */
    fun <T : Any> commit(id: String, event: T): Mono<String>

    /**
     * @see commit
     */
    fun commitSync(id: String): String

    /**
     * @see commit
     * @param T event
     */
    fun <T : Any> commitSync(id: String, event: T): String

    /**
     * Roll back the saga identified by id and publish SagaRollbackEvent.
     *
     * @see SagaRollbackEvent
     * @see SagaRollbackListener
     * @param id started saga id
     * @param cause the reason for the rollback
     * @return String | unique saga id
     * @throws SagaException
     */
    fun rollback(id: String, cause: String): Mono<String>

    /**
     * @see rollback
     * @param T event
     */
    fun <T : Any> rollback(id: String, cause: String, event: T): Mono<String>

    /**
     * @see rollback
     */
    fun rollbackSync(id: String, cause: String): String

    /**
     * @see rollback
     * @param T event
     */
    fun <T : Any> rollbackSync(id: String, cause: String, event: T): String

}
