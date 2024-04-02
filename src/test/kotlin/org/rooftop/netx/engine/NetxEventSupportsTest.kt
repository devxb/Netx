package org.rooftop.netx.engine

import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.equality.shouldBeEqualUsingFields
import io.kotest.matchers.equals.shouldBeEqual
import org.rooftop.netx.api.SagaManager
import org.rooftop.netx.meta.EnableSaga
import org.rooftop.netx.redis.RedisContainer
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource

@EnableSaga
@ContextConfiguration(
    classes = [
        RedisContainer::class,
        SagaReceiveStorage::class,
    ]
)
@DisplayName("NetxEventSupports")
@TestPropertySource("classpath:application.properties")
internal class NetxEventSupportsTest(
    private val sagaManager: SagaManager,
    private val sagaReceiveStorage: SagaReceiveStorage,
) : StringSpec({

    beforeEach {
        sagaReceiveStorage.clear()
    }

    "event로 객체가 주어지면, SagaRollbackEvent에서 해당 객체를 decode 할 수 있다." {
        // given
        val expected = Foo("hello", 1.1234567891234568)
        sagaManager.startSync(expected)

        Thread.sleep(1000)

        // when
        val startEvent = sagaReceiveStorage.pollStart()

        // then
        startEvent.decodeEvent(Foo::class) shouldBeEqualUsingFields expected
    }

    "event로 Map이 주어지면, SagaRollbackEvent에서 해당 객체를 decode할 수 있다." {
        // given
        val expected = mapOf("name" to "hello")
        val id = sagaManager.startSync()
        sagaManager.joinSync(id, expected)

        Thread.sleep(1000)

        // when
        val joinEvent = sagaReceiveStorage.pollJoin()
        val result = joinEvent.decodeEvent(Map::class)

        // then
        result["name"]!! shouldBeEqual expected["name"]!!
    }

    "event로 Int가 주어지면, SagaRollbackEvent에서 해당 객체를 decode할 수 있다." {
        // given
        val expected = 1
        val id = sagaManager.startSync()
        sagaManager.commitSync(id, expected)

        Thread.sleep(1000)

        // when
        val result = sagaReceiveStorage.pollCommit().decodeEvent(Int::class)

        // then
        result shouldBeEqual expected
    }

    "event로 Long이 주어지면, SagaRollbackEvent에서 해당 객체를 decode할 수 있다." {
        // given
        val expected = 1L
        val id = sagaManager.startSync()
        sagaManager.rollbackSync(id, "cause", expected)

        Thread.sleep(1000)

        // when
        val result = sagaReceiveStorage.pollRollback().decodeEvent(Long::class)

        // then
        result shouldBeEqual expected
    }

    "event로 String이 주어지면, SagaRollbackEvent에서 해당 객체를 decode할 수 있다." {
        // given
        val expected = "string"
        sagaManager.startSync(expected)

        Thread.sleep(1000)

        // when
        val result = sagaReceiveStorage.pollStart().decodeEvent(String::class)

        // then
        result shouldBeEqual expected
    }

    "event로 char이 주어지면, SagaRollbackEvent에서 해당 객체를 decode할 수 있다." {
        // given
        val expected = 'c'
        sagaManager.startSync(expected)

        Thread.sleep(1000)

        // when
        val result = sagaReceiveStorage.pollStart().decodeEvent(Char::class)

        // then
        result shouldBeEqual expected
    }

    "event로 Boolean이 주어지면, SagaRollbackEvent에서 해당 객체를 decode할 수 있다." {
        // given
        val expected = true
        sagaManager.startSync(expected)

        Thread.sleep(1000)

        // when
        val result = sagaReceiveStorage.pollStart().decodeEvent(Boolean::class)

        // then
        result shouldBeEqual expected
    }

    "event로 Unit이 주어지면, SagaRollbackEvent에서 해당 객체를 decode할 수 있다." {
        // given
        val expected = Unit
        sagaManager.startSync(expected)

        Thread.sleep(1000)

        // when
        val result = sagaReceiveStorage.pollStart().decodeEvent(Unit::class)

        // then
        result shouldBeEqual expected
    }
}) {

    class Foo(val name: String, val price: Double)
}
