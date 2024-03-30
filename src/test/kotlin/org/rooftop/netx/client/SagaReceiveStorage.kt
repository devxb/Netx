package org.rooftop.netx.client

import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import org.rooftop.netx.api.*
import org.rooftop.netx.meta.SagaHandler
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.CopyOnWriteArrayList

@SagaHandler
class SagaReceiveStorage(
    private val storage: ConcurrentMap<String, CopyOnWriteArrayList<SagaEvent>> = ConcurrentHashMap(),
) {

    fun clear() {
        storage.clear()
    }

    fun joinCountShouldBeGreaterThanOrEqual(count: Int) {
        (storage["JOIN"]?.size ?: 0) shouldBeGreaterThanOrEqual count
    }

    fun startCountShouldBeGreaterThanOrEqual(count: Int) {
        (storage["START"]?.size ?: 0) shouldBeGreaterThanOrEqual count
    }

    fun commitCountShouldBeGreaterThanOrEqual(count: Int) {
        (storage["COMMIT"]?.size ?: 0) shouldBeGreaterThanOrEqual count
    }

    fun rollbackCountShouldBeGreaterThanOrEqual(count: Int) {
        (storage["ROLLBACK"]?.size ?: 0) shouldBeGreaterThanOrEqual count
    }

    @SagaStartListener(
        event = NetxLoadTest.LoadTestEvent::class,
        successWith = SuccessWith.PUBLISH_JOIN
    )
    fun listenStart(sagaStartEvent: SagaStartEvent): Mono<Unit> {
        return Mono.fromCallable { saveSaga("START", sagaStartEvent) }
            .map { sagaStartEvent.decodeEvent(NetxLoadTest.LoadTestEvent::class) }
            .map { sagaStartEvent.setNextEvent(it) }
    }

    @SagaJoinListener(
        event = NetxLoadTest.LoadTestEvent::class,
        successWith = SuccessWith.PUBLISH_COMMIT
    )
    fun listenJoin(sagaJoinEvent: SagaJoinEvent): Mono<Unit> {
        return Mono.fromCallable { saveSaga("JOIN", sagaJoinEvent) }
            .map { sagaJoinEvent.decodeEvent(NetxLoadTest.LoadTestEvent::class) }
            .map { sagaJoinEvent.setNextEvent(it) }
    }

    @SagaRollbackListener(event = NetxLoadTest.LoadTestEvent::class)
    fun listenRollback(sagaRollbackEvent: SagaRollbackEvent): Mono<Unit> {
        return Mono.fromCallable { saveSaga("ROLLBACK", sagaRollbackEvent) }
    }

    @SagaCommitListener(event = NetxLoadTest.LoadTestEvent::class)
    fun listenCommit(sagaCommitEvent: SagaCommitEvent): Mono<Unit> {
        return Mono.fromCallable { saveSaga("COMMIT", sagaCommitEvent) }
            .map { sagaCommitEvent.decodeEvent(NetxLoadTest.LoadTestEvent::class) }
            .map {
                sagaCommitEvent.setNextEvent(it)
                if (it.load == "-") {
                    throw IllegalArgumentException("Rollback cause \"-\"")
                }
            }
    }

    private fun saveSaga(key: String, sagaEvent: SagaEvent) {
        storage.putIfAbsent(key, CopyOnWriteArrayList())
        storage[key]?.add(sagaEvent)
    }
}
