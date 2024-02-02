package org.rooftop.netx.api

fun interface TransactionIdGenerator {

    fun generate(): String
}
