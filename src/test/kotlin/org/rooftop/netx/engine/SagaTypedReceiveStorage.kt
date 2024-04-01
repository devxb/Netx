package org.rooftop.netx.engine

import io.kotest.matchers.equals.shouldBeEqual
import org.rooftop.netx.api.SuccessWith
import org.rooftop.netx.api.SagaEvent
import org.rooftop.netx.api.SagaStartEvent
import org.rooftop.netx.api.SagaStartListener
import org.rooftop.netx.meta.SagaHandler
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KClass

@SagaHandler
internal class SagaTypedReceiveStorage {

    private val storage: ConcurrentMap<KClass<*>, CopyOnWriteArrayList<SagaEvent>> =
        ConcurrentHashMap()

    fun clear() {
        storage.clear()
    }

    fun handlerShouldBeEqual(key: KClass<*>, count: Int) {
        (storage[key]?.size ?: 0) shouldBeEqual count
    }

    @SagaStartListener(successWith = SuccessWith.END)
    fun any(sagaStartEvent: SagaStartEvent): Mono<Unit> {
        return Mono.fromCallable { log(Any::class, sagaStartEvent) }
    }

    @SagaStartListener(NetxEventTypedDispatherTest.Foo::class, successWith = SuccessWith.END)
    fun foo(sagaStartEvent: SagaStartEvent): Mono<Unit> {
        return Mono.fromCallable { log(NetxEventTypedDispatherTest.Foo::class, sagaStartEvent) }
    }

    @SagaStartListener(String::class, successWith = SuccessWith.END)
    fun string(sagaStartEvent: SagaStartEvent) {
        log(String::class, sagaStartEvent)
    }

    @SagaStartListener(Long::class, successWith = SuccessWith.END)
    fun long(sagaStartEvent: SagaStartEvent): Long {
        log(Long::class, sagaStartEvent)
        return 1L
    }

    @SagaStartListener(Unit::class, successWith = SuccessWith.END)
    fun unit(sagaStartEvent: SagaStartEvent) {
        log(Unit::class, sagaStartEvent)
    }

    @SagaStartListener(Boolean::class, successWith = SuccessWith.END)
    fun boolean(sagaStartEvent: SagaStartEvent) {
        log(Boolean::class, sagaStartEvent)
    }

    private fun log(key: KClass<*>, sagaEvent: SagaEvent) {
        storage.putIfAbsent(key, CopyOnWriteArrayList())
        storage[key]?.add(sagaEvent)
    }

}
