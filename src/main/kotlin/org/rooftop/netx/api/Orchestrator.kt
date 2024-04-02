package org.rooftop.netx.api

import reactor.core.publisher.Mono

/**
 * Allows using saga with orchestration.
 *
 * If an exception occurs in any OrchestrateChain, rollback is performed starting from that chain upwards.
 * Additionally, you can obtain the response if it succeeds via Result or throw the failed exception.
 *
 * For more details, refer to the Example below.
 *
 * Example.
 *
 *      class OrderFacade {
 *
 *          private val orderOrchestrator: OrderOrchestrator<OrderRequest, Order>
 *              = OrderFactory.instance().get("orderOrchestrator")
 *
 *          fun order(token: String, orderRequest: OrderRequest): Order {
 *              return orderOrchestrator.sagaSync(
 *                  request = orderRequest,
 *                  context = mapOf("token", token),
 *              ).decodeResultOrThrow(Order::class)
 *          }
 *
 *      }
 *
 *      class OrderOrchestratorConfigurer {
 *
 *          private val orchestratorFactory = OrchestratorFactory.instance()
 *
 *          fun orderOrchestrator(): Orchestrator<OrderRequest, Order> {
 *              return orchestratorFactory.create<OrderRequest>("orderOrchestrator")
 *                  .startWithContext(
 *                      contextOrchestrate = { context, orderRequest ->
 *                          val token = context.decodeContext("token", String::class)
 *                          context.set("orderId", orderRequest.id)
 *                          payService.payment(token, orderRequest.id, orderRequest.totalPrice) // Return PayResponse class
 *                      },
 *                      rollback = { context, orderRequest ->
 *                          val token = context.decodeContext("token", String::class)
 *                          payWebClient.cancelPay(token, orderRequest.id)
 *                      }
 *                  )
 *                  .joinWithContext(
 *                      contextOrchestrate = { context, payResponse ->
 *                          val orderId = context.decodeContext("orderId", Long::class)
 *                          orderService.successOrder(orderId) // Return Order class
 *                      },
 *                      rollback = { context, payResponse ->
 *                          val orderId = context.decodeContext("orderId", Long::class)
 *                          orderService.failOrder(orderId)
 *                      }
 *                  )
 *                  .commit(
 *                    orchestrate = { order ->
 *                          // If a rollback occurs here, all the above rollback functions will be executed sequentially.
 *                          shopService.consumeStock(order.productId, order.productQuantity)
 *                          order
 *                      }
 *                  )
 *          }
 *      }
 *
 * @see OrchestratorFactory
 * @see OrchestrateChain
 * @see Orchestrate
 * @see ContextOrchestrate
 * @see Rollback
 * @see ContextRollback
 */
interface Orchestrator<T : Any, V : Any> {

    /**
     * Executes the saga.
     *
     * The default timeoutMillis is 10 seconds, and if the result is not returned within this time, ResultTimeoutException is thrown.
     *
     * However, even if an exception is thrown, the orchestration continues.
     *
     * @see Result
     * @param request
     * @return Result returns the result of the saga.
     * @throws ResultTimeoutException
     */
    fun saga(request: T): Mono<Result<V>>

    /**
     * @see saga
     *
     * @param timeoutMillis Waits for the result to be returned until timeoutMillis.
     */
    fun saga(timeoutMillis: Long, request: T): Mono<Result<V>>

    /**
     * @see saga
     *
     * @param context Starts the saga with the context.
     */
    fun saga(request: T, context: Map<String, Any>): Mono<Result<V>>

    /**
     * @see saga
     *
     * @param timeoutMillis Waits for the result to be returned until timeoutMillis.
     * @param context Starts the saga with the context.
     */
    fun saga(timeoutMillis: Long, request: T, context: Map<String, Any>): Mono<Result<V>>

    /**
     * @see saga
     */
    fun sagaSync(request: T): Result<V>

    /**
     * @see saga
     */
    fun sagaSync(timeoutMillis: Long, request: T): Result<V>

    /**
     * @see saga
     */
    fun sagaSync(request: T, context: Map<String, Any>): Result<V>

    /**
     * @see saga
     */
    fun sagaSync(timeoutMillis: Long, request: T, context: Map<String, Any>): Result<V>
}
