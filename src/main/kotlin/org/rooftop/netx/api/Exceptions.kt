package org.rooftop.netx.api

class EncodeException(message: String, throwable: Throwable) : RuntimeException(message, throwable)

class DecodeException(message: String, throwable: Throwable) : RuntimeException(message, throwable)

class TransactionException(message: String) : RuntimeException(message)

class NotFoundDispatchFunctionException(message: String) : RuntimeException(message)

class FailedAckTransactionException(message: String) : RuntimeException(message)
