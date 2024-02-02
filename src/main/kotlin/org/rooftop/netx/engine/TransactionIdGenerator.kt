package org.rooftop.netx.engine

import com.github.f4b6a3.tsid.TsidFactory

class TransactionIdGenerator : TransactionIdGenerator {

    override fun generate(): String = tsidFactory.create().toLong().toString()

    private companion object {
        private val tsidFactory = TsidFactory.newInstance256(110)
    }
}
