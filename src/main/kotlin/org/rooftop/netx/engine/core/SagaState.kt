package org.rooftop.netx.engine.core

internal enum class SagaState {
    JOIN,
    COMMIT,
    ROLLBACK,
    START,
    ;
}
