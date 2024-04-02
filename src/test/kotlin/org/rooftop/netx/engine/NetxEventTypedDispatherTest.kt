package org.rooftop.netx.engine

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.StringSpec
import org.rooftop.netx.api.SagaManager
import org.rooftop.netx.meta.EnableSaga
import org.rooftop.netx.redis.RedisContainer
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import kotlin.time.Duration.Companion.seconds

@EnableSaga
@ContextConfiguration(
    classes = [
        RedisContainer::class,
        SagaTypedReceiveStorage::class,
    ]
)
@DisplayName("NetxEventTypedDispatherTest")
@TestPropertySource("classpath:application.properties")
internal class NetxEventTypedDispatherTest(
    private val sagaManager: SagaManager,
    private val sagaTypedReceiveStorage: SagaTypedReceiveStorage,
) : StringSpec({

    beforeEach {
        sagaTypedReceiveStorage.clear()
    }

    "event로 Foo 타입의 클래스가 주어지면, Any::class, Foo::class의 모든 핸들러에게 saga event가 전파된다." {
        sagaManager.startSync(Foo("xb"))

        eventually(5.seconds) {
            sagaTypedReceiveStorage.handlerShouldBeEqual(Any::class, 1)
            sagaTypedReceiveStorage.handlerShouldBeEqual(Foo::class, 1)
            sagaTypedReceiveStorage.handlerShouldBeEqual(String::class, 0)
            sagaTypedReceiveStorage.handlerShouldBeEqual(Long::class, 0)
            sagaTypedReceiveStorage.handlerShouldBeEqual(Unit::class, 0)
            sagaTypedReceiveStorage.handlerShouldBeEqual(Boolean::class, 0)
        }
    }

    "event로 String 타입의 클래스가 주어지면, Any::class, String::class의 모든 핸들러에게 saga event가 전파된다." {
        sagaManager.startSync("String")

        eventually(5.seconds) {
            sagaTypedReceiveStorage.handlerShouldBeEqual(Any::class, 1)
            sagaTypedReceiveStorage.handlerShouldBeEqual(Foo::class, 0)
            sagaTypedReceiveStorage.handlerShouldBeEqual(String::class, 1)
            sagaTypedReceiveStorage.handlerShouldBeEqual(Long::class, 0)
            sagaTypedReceiveStorage.handlerShouldBeEqual(Unit::class, 0)
            sagaTypedReceiveStorage.handlerShouldBeEqual(Boolean::class, 0)
        }
    }

    "event로 Long 타입의 클래스가 주어지면, Any::class, Long::class, String::class, Boolean::class 의 모든 핸들러에게 saga event가 전파된다." {
        sagaManager.startSync(1000L)

        eventually(5.seconds) {
            sagaTypedReceiveStorage.handlerShouldBeEqual(Any::class, 1)
            sagaTypedReceiveStorage.handlerShouldBeEqual(Foo::class, 0)
            sagaTypedReceiveStorage.handlerShouldBeEqual(String::class, 1)
            sagaTypedReceiveStorage.handlerShouldBeEqual(Long::class, 1)
            sagaTypedReceiveStorage.handlerShouldBeEqual(Unit::class, 0)
            sagaTypedReceiveStorage.handlerShouldBeEqual(Boolean::class, 1)
        }
    }

    "event로 Boolean 타입의 클래스가 주어지면, Any::class, Boolean::class, String::class 의 모든 핸들러에게 saga event가 전파된다." {
        sagaManager.startSync(true)

        eventually(5.seconds) {
            sagaTypedReceiveStorage.handlerShouldBeEqual(Any::class, 1)
            sagaTypedReceiveStorage.handlerShouldBeEqual(Foo::class, 0)
            sagaTypedReceiveStorage.handlerShouldBeEqual(String::class, 1)
            sagaTypedReceiveStorage.handlerShouldBeEqual(Long::class, 0)
            sagaTypedReceiveStorage.handlerShouldBeEqual(Unit::class, 0)
            sagaTypedReceiveStorage.handlerShouldBeEqual(Boolean::class, 1)
        }
    }

    "event로 어떠한것도 전달되지 않으면, Any::class의 모든 핸들러에게 saga event가 전파된다." {
        sagaManager.startSync()

        eventually(5.seconds) {
            sagaTypedReceiveStorage.handlerShouldBeEqual(Any::class, 1)
            sagaTypedReceiveStorage.handlerShouldBeEqual(Foo::class, 0)
            sagaTypedReceiveStorage.handlerShouldBeEqual(String::class, 0)
            sagaTypedReceiveStorage.handlerShouldBeEqual(Long::class, 0)
            sagaTypedReceiveStorage.handlerShouldBeEqual(Unit::class, 0)
            sagaTypedReceiveStorage.handlerShouldBeEqual(Boolean::class, 0)
        }
    }

    "event로 Unit이 주어지면, Unit::class의 핸들러에게 saga event가 전파된다." {
        sagaManager.startSync(Unit)

        eventually(5.seconds) {
            sagaTypedReceiveStorage.handlerShouldBeEqual(Any::class, 1)
            sagaTypedReceiveStorage.handlerShouldBeEqual(Foo::class, 0)
            sagaTypedReceiveStorage.handlerShouldBeEqual(String::class, 0)
            sagaTypedReceiveStorage.handlerShouldBeEqual(Long::class, 0)
            sagaTypedReceiveStorage.handlerShouldBeEqual(Unit::class, 1)
            sagaTypedReceiveStorage.handlerShouldBeEqual(Boolean::class, 0)
        }
    }

}) {

    class Foo(val name: String)
}
