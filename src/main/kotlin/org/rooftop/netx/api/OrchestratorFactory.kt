package org.rooftop.netx.api

/**
 * Factory for creating Orchestrators.
 *
 * For detailed usage, refer to the Example of Orchestrator.
 *
 * @see Orchestrator
 */
interface OrchestratorFactory {

    /**
     * Returns the Orchestrator corresponding to orchestratorId.
     *
     * If the Orchestrator corresponding to orchestratorId cannot be found, it throws an IllegalArgumentException.
     *
     * @param T request of Orchestrator
     * @param V response of Orchestrator
     * @throws IllegalArgumentException
     */
    fun <T : Any, V : Any> get(orchestratorId: String): Orchestrator<T, V>

    /**
     * Creates a new Orchestrator.
     *
     * If an Orchestrator with the same orchestratorId has been created previously, it returns that Orchestrator.
     *
     * Therefore, Orchestrators with the same orchestratorId are always the same.
     *
     * @param T request of Orchestrator
     * @see OrchestrateChain
     */
    fun <T : Any> create(orchestratorId: String): OrchestrateChain.Pre<T>

    companion object Instance {
        internal lateinit var orchestratorFactory: OrchestratorFactory

        /**
         * Retrieves the OrchestratorFactory.
         */
        fun instance(): OrchestratorFactory = orchestratorFactory
    }
}
