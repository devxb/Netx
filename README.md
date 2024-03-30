# Netx <img src="https://avatars.githubusercontent.com/u/149151221?s=200&v=4" height = 100 align = left>

> Saga framework / Supports redis-stream and reactive

<img src = "https://github.com/rooftop-MSA/Netx/assets/62425964/08ed9050-1923-42b5-803f-5b7ea37a263f" width="360" align="right"/>

<br>

![version 0.3.9](https://img.shields.io/badge/version-0.3.9-black?labelColor=black&style=flat-square) ![jdk 17](https://img.shields.io/badge/minimum_jdk-17-orange?labelColor=black&style=flat-square) ![load-test](https://img.shields.io/badge/load%20test%2010%2C000%2C000-success-brightgreen?labelColor=black&style=flat-square)    
![redis--stream](https://img.shields.io/badge/-redis--stream-da2020?style=flat-square&logo=Redis&logoColor=white)

Redis-Stream을 지원하는 Saga frame work 입니다.   
`Netx` 는 다음 기능을 제공합니다.

1. 동기 API와 비동기[Reactor](https://projectreactor.io/) API 지원
2. 함수형 Orchestrator 방식과 Event 기반 Choreograph 방식 지원
3. 설정한 주기 마다 처리되지 않은 이벤트를 찾아 자동으로 재실행
4. Backpressure 지원으로 노드별 처리가능한 이벤트 수 조절
5. 같은 그룹의 여러 노드가 이벤트를 중복 수신하는 문제 방지
6. `At Least Once` 방식의 메시지 전달 보장

## How to use

Netx는 스프링 환경에서 사용할 수 있으며, 아래와 같이 `@EnableSaga` 어노테이션을 붙이는것으로 손쉽게 구성할 수 있습니다.

```kotlin
@EnableSaga
@SpringBootApplication
class Application {

    companion object {
        @JvmStatic
        fun main(vararg args: String) {
            SpringApplication.run(Application::class.java, *args)
        }
    }
}
```

`@EnableSaga` 어노테이션으로 자동 구성할 경우 netx는 아래 프로퍼티를 사용해 이벤트 스트림 서비스와 커넥션을 맺습니다.

#### Properties

| KEY                     | EXAMPLE   | DESCRIPTION                                                                                                                                                                   | DEFAULT |
|-------------------------|-----------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------|
| **netx.mode**           | redis     | Saga 관리에 사용할 메시지 큐 구현체의 mode 입니다.                                                                                                                                             |         |
| **netx.host**           | localhost | Saga 관리에 사용할 메시지 큐 의 host url 입니다. (ex. redis host)                                                                                                                           |         |
| **netx.password**       | 0000      | Saga 관리에 사용할 메시지큐에 접속하는데 사용하는 password 입니다. 설정하지 않을시 0000이 비밀번호로 매핑됩니다.                                                                                                       | 0000    |
| **netx.port**           | 6379      | Saga 관리에 사용할 메시지 큐의 port 입니다.                                                                                                                                                 |         |
| **netx.group**          | pay-group | 분산 노드의 그룹입니다. Saga 이벤트는 같은 그룹내 하나의 노드로만 전송됩니다.                                                                                                                                |         |
| **netx.node-id**        | 1         | id 생성에 사용될 식별자입니다. 모든 서버는 반드시 다른 id를 할당받아야 하며, 1~256 만큼의 id를 설정할 수 있습니다. _`중복된 id 생성을 방지하기위해 twitter snowflake 알고리즘으로 id를 생성합니다.`_                                            |         |
| **netx.node-name**      | pay-1     | _`netx.group`_ 에 참여할 서버의 이름입니다. 같은 그룹내에 중복된 이름이 존재하면 안됩니다.                                                                                                                    |         |
| **netx.recovery-milli** | 1000      | _`netx.recovery-milli`_ 마다 _`netx.orphan-milli`_ 동안 처리 되지 않는 Saga를 찾아 재실행합니다.                                                                                                 | 1000    |
| **netx.orphan-milli**   | 60000     | PENDING 상태가된 이벤트 중, orphan-milli가 지나도 ACK 상태가 되지 않은 이벤트르  찾아 재시작합니다.                                                                                                          | 60000   |
| **netx.backpressure**   | 40        | 한번에 수신가능한 이벤트 수를 조절합니다. **너무 높게설정하면 서버에 부하가 올 수 있고, 낮게 설정하면 성능이 낮아질 수 있습니다.** 이 설정은 다른 서버가 발행한 이벤트 수신량과 처리에 실패한 이벤트 수신량에 영향을 미칩니다. 수신되지 못하거나, drop된 이벤트는 자동으로 재시도 대기열에 들어갑니다. | 40      |
| **netx.logging.level**  | info      | logging level을 지정합니다. 선택가능한 value는 다음과 같습니다. "info", "warn", "off"                                                                                                            | "off"   |
| **netx.pool-size**      | 40        | 커넥션을 계속해서 맺어야할때, 최대 커넥션 수를 조절하는데 사용됩니다.                                                                                                                                       | 10      |

### Usage example

#### Orchestrator-example.

> [!TIP]   
> Orchestrator 사용시, `Transactional messaging pattern` 이 자동 적용됩니다.   
> 이벤트 유실에대한 retry 단위는 Orchestrator의 각 연산(하나의 function) 단위이며, 모든 체인이 성공하거나 rollback이 호출됩니다.

```kotlin
// Use Orchestrator
@Service
class OrderService(private val orderOrchestrator: Orchestrator<Order, OrderResponse>) {

    fun order(orderRequest: Order): OrderResult {
        val result = orderOrchestrator.sagaSync(orderRequest)
        
        result.decodeResultOrThrow(OrderResult::class) // If success get result or else throw exception 
    }
}

// Register Orchestrator
@Configurer
class OrchestratorConfigurer(
    private val orchestratorFactory: OrchestratorFactory
) {

    @Bean
    fun orderOrchestartor(): Orchestrator<Order, OrderResponse> { // <First Request, Last Response>
        return orchestratorFactory.create("orderOrchestrator")
            .start(
                orchestrate = { order -> // its order type
                    // Do your bussiness logic 
                    // something like ... "Check valid seller"
                    return@start user
                },
                rollback = { order ->
                    // do rollback logic
                }
            )
            .joinReactive(
                orchestrate = { user -> // Before operations response type "User" flow here 
                    // Webflux supports, should return Mono type.
                },
                // Can skip rollback operation, if you want
            )
            .joinWithContext(
                contextOrchestrate = { context, request ->
                    context.set("key", request) // save data on context
                    context.decode("foo", Foo::class) // The context set in the upstream chain can be retrieved.
                },
            )
            .commit(
                orchestrate = { request ->
                    // When an error occurs, all rollbacks are called from the bottom up, 
                    // starting from the location where the error occurred.
                    throw IllegalArgumentException("Oops! Something went wrong..")
                },
                rollback = { request ->
                    ...
                }
            )
    }
}
```

#### Events-Scenario0. Handle saga event

다른 분산서버가 (혹은 자기자신이) sagaManager를 통해서 saga를 시작하거나 saga의 상태를 변경했을때, 상태에 맞는 핸들러를 호출합니다.
이 핸들러를 구현함으로써, saga 상태별 로직을 구현할 수 있습니다.
각 핸들러에서 에러가 던져지면, 자동으로 rollback 이 호출되며, 핸들러가 종료되면, 어노테이션에 설정된 successWith 상태가 자동으로 호출니다.

> [!WARNING]   
> Saga 핸들러는 반드시 핸들러에 맞는 `Saga...Event` **하나**만을 파라미터로 받아야 합니다.
> Event사용시 `Transactional messaging pattern` 을 직접 적용해줘야합니다.    
> 아래 예시와 같이, 비즈니스로직을 모두 @Saga...Listener 안으로 이동함으로써 손쉽게 적용할 수 있습니다.

```kotlin

@SagaHandler
class SagaHandler(
    private val sagaManager: SagaManager,
) {
    
    fun start() {
        val foo = Foo("...")
        sagaManager.startSync(foo) // it will call 
    }

    @SagaStartListener(event = Foo::class, successWith = SuccessWith.PUBLISH_JOIN) // Receive saga event when event can be mapped to Foo.class
    fun handleSagaStartEvent(event: SagaStartEvent) {
        val foo: Foo = event.decodeEvent(Foo::class) // Get event field to Foo.class
        // ...
        event.setNextEvent(nextFoo) // When this handler terminates and calls the next event or rollback, the event set here is published together.
    }

    @SagaJoinListener(successWith = SuccessWith.PUBLISH_COMMIT) // Receive all saga event when no type is defined. And, when terminated this function, publish commit state
    fun handleSagaJoinEvent(event: SagaJoinEvent) {
        // ...
    }

    @SagaCommitListener(
        event = Foo::class,
        noRollbackFor = [IllegalArgumentException::class] // Don't rollback when throw IllegalArgumentException. *Rollback if throw Throwable or IllegalArgumentException's super type* 
    )
    fun handleSagaCommitEvent(event: SagaCommitEvent): Mono<String> { // In Webflux framework, publisher must be returned.
        throw IllegalArgumentException("Ignore this exception")
        // ...
    }

    @SagaRollbackListener(Foo::class)
    fun handleSagaRollbackEvent(event: SagaRollbackEvent) { // In Mvc framework, publisher must not returned.
        val undo: Foo = event.decodeUndo(Foo::class) // Get event field to Foo.class
    }
}
```

#### Events-Scenario1. Start pay saga

```kotlin
// Sync
fun pay(param: Any): Any {
    val sagaId = sagaManager.syncStart(Pay(id = 1L, paid = 1000L)) // start saga

    runCatching {
        // Do your bussiness logic
    }.fold(
        onSuccess = { sagaManager.syncCommit(sagaId) }, // commit saga
        onFailure = {
            sagaManager.syncRollback(
                sagaId,
                it.message
            )
        } // rollback saga
    )
}

// Async
fun pay(param: Any): Mono<Any> {
    return sagaManager.start(
        Pay(
            id = 1L,
            paid = 1000L
        )
    ) // Start distributed saga and publish saga start event
        .flatMap { sagaId ->
            service.pay(param)
                .doOnError { throwable ->
                    sagaManager.rollback(
                        sagaId,
                        throwable.message
                    ) // Publish rollback event to all saga joined node
                }
        }.doOnSuccess { sagaId ->
            sagaManager.commit(sagaId) // Publish commit event to all saga joined node
        }
}
```

#### Events-Scenario2. Join order saga

```kotlin
//Sync
fun order(param: Any): Any {
    val sagaId = sagaManager.syncJoin(
        param.saganId,
        Order(id = 1L, state = PENDING)
    ) // join saga

    runCatching { // This is kotlin try catch, not netx library spec
        // Do your bussiness logic
    }.fold(
        onSuccess = { sagaManager.syncCommit(sagaId) }, // commit saga
        onFailure = {
            sagaManager.syncRollback(
                sagaId,
                it.message
            )
        } // rollback saga
    )
}

// Async
fun order(param: Any): Mono<Any> {
    return sagaManager.join(
        param.sagaId,
        Order(id = 1L, state = PENDING)
    ) // join exists distributed saga and publish saga join event
        .flatMap { sagaId ->
            service.order(param)
                .doOnError { throwable ->
                    sagaManager.rollback(sagaId, throwable.message)
                }
        }.doOnSuccess { sagaId ->
            sagaManager.commit(sagaId)
        }
}
```

#### Events-Scenario3. Check exists saga

```kotlin
// Sync
fun exists(param: Any): Any {
    return sagaManager.syncExists(param.sagaId)
}

// Async
fun exists(param: Any): Mono<Any> {
    return sagaManager.exists(param.sagaId) // Find any saga has ever been started 
}
```


## Download

```groovy
repositories {
    maven { url "https://jitpack.io" }
}

dependencies {
    implementation "com.github.rooftop-msa:netx:${version}"
}
```
