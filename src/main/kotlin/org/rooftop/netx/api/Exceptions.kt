package org.rooftop.netx.api

import org.rooftop.netx.engine.core.TransactionState

class EncodeException(message: String, throwable: Throwable) : RuntimeException(message, throwable)

class DecodeException(message: String, throwable: Throwable) : RuntimeException(message, throwable)

open class TransactionException(message: String) : RuntimeException(message)

class AlreadyCommittedTransactionException(transactionId: String, state: TransactionState) :
    TransactionException("Cannot join transaction cause, transaction \"$transactionId\" already \"$state\"")

class NotFoundDispatchFunctionException(message: String) : RuntimeException(message)

class FailedAckTransactionException(message: String) : RuntimeException(message)
