package org.rooftop.netx.redis

import io.kotest.matchers.shouldBe

abstract class AbstractSagaHandlerAssertions {

    private val methodInvocationCounts = mutableMapOf<String, Int>()

    fun clear() {
        methodInvocationCounts.clear()
    }

    fun joinCountShouldBe(count: Int) {
        (methodInvocationCounts["JOIN"] ?: 0) shouldBe count
    }

    fun startCountShouldBe(count: Int) {
        (methodInvocationCounts["START"] ?: 0) shouldBe count
    }

    fun commitCountShouldBe(count: Int) {
        (methodInvocationCounts["COMMIT"] ?: 0) shouldBe count
    }

    fun rollbackCountShouldBe(count: Int) {
        (methodInvocationCounts["ROLLBACK"] ?: 0) shouldBe count
    }

    protected fun put(key: String) {
        methodInvocationCounts[key] = methodInvocationCounts.getOrDefault(key, 0) + 1
    }
}
