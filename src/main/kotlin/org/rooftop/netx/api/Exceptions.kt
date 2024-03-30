package org.rooftop.netx.api

class EncodeException(message: String, throwable: Throwable) : RuntimeException(message, throwable)

class DecodeException(message: String, throwable: Throwable) : RuntimeException(message, throwable)

open class SagaException(message: String) : RuntimeException(message)

class AlreadyCommittedSagaException(id: String, state: String) :
    SagaException("Cannot join saga cause, saga \"$id\" already \"$state\"")

class NotFoundDispatchFunctionException(message: String) : RuntimeException(message)

class FailedAckSagaException(message: String) : RuntimeException(message)

class ResultTimeoutException(message: String, throwable: Throwable) :
    RuntimeException(message, throwable)

class ResultException(message: String) : RuntimeException(message)
