package org.rooftop.netx.api

class EncodeException(message: String, throwable: Throwable) : IllegalArgumentException(message, throwable)

class DecodeException(message: String, throwable: Throwable) :
    IllegalArgumentException(message, throwable)
