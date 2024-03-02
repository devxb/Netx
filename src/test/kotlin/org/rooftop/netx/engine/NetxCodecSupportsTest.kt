package org.rooftop.netx.engine

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.equality.shouldBeEqualUsingFields
import io.kotest.matchers.equals.shouldBeEqual
import org.junit.jupiter.api.DisplayName
import org.rooftop.netx.api.TransactionManager
import org.rooftop.netx.meta.EnableDistributedTransaction
import org.rooftop.netx.redis.RedisContainer
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource

@EnableDistributedTransaction
@ContextConfiguration(
    classes = [
        RedisContainer::class,
        RollbackEventStorage::class,
    ]
)
@DisplayName("NetxCodecSupports")
@TestPropertySource("classpath:application.properties")
class NetxCodecSupportsTest(
    private val transactionManager: TransactionManager,
    private val rollbackEventStorage: RollbackEventStorage,
) : StringSpec({

    fun <T> startAndRollbackTransaction(undo: T) {
        val transactionId = transactionManager.syncStart(undo)
        transactionManager.syncRollback(transactionId, "for codec test")

        Thread.sleep(1000)
    }

    "undo로 객체가 주어지면, TransactionRollbackEvent에서 해당 객체를 decode 할 수 있다." {
        // given
        val expected = Foo("hello", 1.1234567891234568)
        startAndRollbackTransaction(expected)

        // when
        val rollbackEvent = rollbackEventStorage.poll()

        // then
        rollbackEvent.getUndo(Foo::class) shouldBeEqualUsingFields expected
    }

    "undo로 Map이 주어지면, TransactionRollbackEvent에서 해당 객체를 decode할 수 있다." {
        // given
        val expected = mapOf("name" to "hello")
        startAndRollbackTransaction(expected)

        // when
        val rollbackEvent = rollbackEventStorage.poll()
        val result = rollbackEvent.getUndo(Map::class)

        // then
        result["name"]!! shouldBeEqual expected["name"]!!
    }

    "undo로 Int가 주어지면, TransactionRollbackEvent에서 해당 객체를 decode할 수 있다." {
        // given
        val expected = 1
        startAndRollbackTransaction(expected)

        // when
        val result = rollbackEventStorage.poll().getUndo(Int::class)

        // then
        result shouldBeEqual expected
    }

    "undo로 Long이 주어지면, TransactionRollbackEvent에서 해당 객체를 decode할 수 있다." {
        // given
        val expected = 1L
        startAndRollbackTransaction(expected)

        // when
        val result = rollbackEventStorage.poll().getUndo(Long::class)

        // then
        result shouldBeEqual expected
    }

    "undo로 String이 주어지면, TransactionRollbackEvent에서 해당 객체를 decode할 수 있다." {
        // given
        val expected = "string"
        startAndRollbackTransaction(expected)

        // when
        val result = rollbackEventStorage.poll().getUndo(String::class)

        // then
        result shouldBeEqual expected
    }

    "undo로 char이 주어지면, TransactionRollbackEvent에서 해당 객체를 decode할 수 있다." {
        // given
        val expected = 'c'
        startAndRollbackTransaction(expected)

        // when
        val result = rollbackEventStorage.poll().getUndo(Char::class)

        // then
        result shouldBeEqual expected
    }

    "undo로 Boolean이 주어지면, TransactionRollbackEvent에서 해당 객체를 decode할 수 있다." {
        // given
        val expected = true
        startAndRollbackTransaction(expected)

        // when
        val result = rollbackEventStorage.poll().getUndo(Boolean::class)

        // then
        result shouldBeEqual expected
    }

    "undo로 Unit이 주어지면, TransactionRollbackEvent에서 해당 객체를 decode할 수 있다." {
        // given
        val expected = Unit
        startAndRollbackTransaction(expected)

        // when
        val result = rollbackEventStorage.poll().getUndo(Unit::class)

        // then
        result shouldBeEqual expected
    }
}) {
    class Foo(val name: String, val price: Double)
}

