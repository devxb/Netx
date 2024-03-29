package org.rooftop.netx.engine.listen

import org.rooftop.netx.api.*
import org.rooftop.netx.engine.AbstractOrchestrateListener
import org.rooftop.netx.engine.OrchestrateEvent
import org.rooftop.netx.engine.RequestHolder
import org.rooftop.netx.engine.ResultHolder
import reactor.core.publisher.Mono

internal class MonoJoinOrchestrateListener<T : Any, V : Any>(
    private val codec: Codec,
    private val transactionManager: TransactionManager,
    private val orchestratorId: String,
    orchestrateSequence: Int,
    private val monoOrchestrateCommand: MonoOrchestrateCommand<T, V>,
    private val requestHolder: RequestHolder,
    private val resultHolder: ResultHolder,
    private val typeReference: TypeReference<T>?,
) : AbstractOrchestrateListener<T, V>(
    orchestratorId,
    orchestrateSequence,
    codec,
    transactionManager,
    requestHolder,
    resultHolder,
    typeReference,
) {

    override fun withAnnotated(): AbstractOrchestrateListener<T, V> {
        return when {
            isLast -> this.successWithCommit()
            !isLast -> this.successWithJoin()
            else -> error("Cannot annotated")
        }
    }

    private fun successWithJoin(): AbstractOrchestrateListener<T, V> {
        return object : AbstractOrchestrateListener<T, V>(
            orchestratorId,
            orchestrateSequence,
            codec,
            transactionManager,
            requestHolder,
            resultHolder,
            typeReference,
        ) {
            @TransactionJoinListener(
                event = OrchestrateEvent::class,
                successWith = SuccessWith.PUBLISH_JOIN
            )
            fun handleTransactionJoinEvent(transactionJoinEvent: TransactionJoinEvent): Mono<OrchestrateEvent> {
                return orchestrate(transactionJoinEvent)
            }

            override fun command(request: T, event: OrchestrateEvent): Mono<Pair<V, Context>> {
                return monoOrchestrateCommand.command(request, event.context)
            }
        }
    }

    private fun successWithCommit(): AbstractOrchestrateListener<T, V> {
        return object : AbstractOrchestrateListener<T, V>(
            orchestratorId,
            orchestrateSequence,
            codec,
            transactionManager,
            requestHolder,
            resultHolder,
            typeReference,
        ) {
            @TransactionJoinListener(
                event = OrchestrateEvent::class,
                successWith = SuccessWith.PUBLISH_COMMIT
            )
            fun handleTransactionJoinEvent(transactionJoinEvent: TransactionJoinEvent): Mono<OrchestrateEvent> {
                return orchestrate(transactionJoinEvent)
            }

            override fun command(request: T, event: OrchestrateEvent): Mono<Pair<V, Context>> {
                return monoOrchestrateCommand.command(request, event.context)
            }
        }
    }
}
