package org.rooftop.netx.engine

import com.github.f4b6a3.tsid.TsidFactory

class TransactionIdGenerator(
    nodeId: Int,
    private val tsidFactory: TsidFactory = TsidFactory.newInstance256(nodeId),
) {

    fun generate(): String = tsidFactory.create().toLong().toString()
}

