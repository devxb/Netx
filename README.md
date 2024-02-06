# Netx <img src="https://avatars.githubusercontent.com/u/149151221?s=200&v=4" height = 100 align = left>

> Distributed transaction library based on Choreography

<br>

![version 0.1.1](https://img.shields.io/badge/version-0.1.1-black?labelColor=black&style=flat-square) ![jdk 17](https://img.shields.io/badge/jdk-17-orange?labelColor=black&style=flat-square)

<img src = "https://github.com/rooftop-MSA/Netx/assets/62425964/5082ef20-10ad-4b6b-bff8-7e78a0f9e01f" width="500" align="right"/>

Choreography 방식으로 구현된 분산 트랜잭션 라이브러리 입니다.   
`Netx` 는 다음 기능을 제공합니다.

1. [Reactor](https://projectreactor.io/) 기반의 완전한 비동기 트랜잭션 관리
2. Redis-stream 기반의 트랜잭션 관리
3. 여러 노드가 중복 트랜잭션 이벤트를 수신하는 문제 방지
4. `At Least Once` 방식의 메시지 전달 보장

## How to use

Netx는 스프링 환경에서 사용할 수 있으며, 아래와 같이 `@AutoConfigureRedisTransaction` 어노테이션을 붙이는것으로 손쉽게 사용할 수 있습니다.

```kotlin
@SpringBootApplication
@AutoConfigureRedisTransaction
@EnableAutoConfiguration(exclude = [RedisReactiveAutoConfiguration::class])
class Application {

    companion object {
        @JvmStatic
        fun main(vararg args: String) {
            SpringApplication.run(Application::class.java, *args)
        }
    }
}
```

`@AutoconfigureRedisTransaction` 어노테이션으로 자동 구성할 경우 netx는 아래 프로퍼티를 사용해 메시지 큐와 커넥션을 맺습니다.

#### Properties

| key                | example   | description                                                                                                                        |
|--------------------|-----------|------------------------------------------------------------------------------------------------------------------------------------|
| **netx.mode**      | redis     | 트랜잭션 관리에 사용할 메시지 큐 구현체의 mode 입니다.                                                                                                  |
| **netx.host**      | localhost | 트랜잭션 관리에 사용할 메시지 큐 의 host url 입니다. (ex. redis host)                                                                                |
| **netx.port**      | 6379      | 트랜잭션 관리에 사용할 메시지 큐의 port 입니다.                                                                                                      |
| **netx.group**     | pay-group | 분산 노드의 그룹입니다. 트랜잭션 이벤트는 같은 그룹내 하나의 노드로만 전송됩니다.                                                                                     |
| **netx.node-id**   | 1         | id 생성에 사용될 식별자입니다. 모든 서버는 반드시 다른 id를 할당받아야 하며, 1~256 만큼의 id를 설정할 수 있습니다. _`중복된 id 생성을 방지하기위해 twitter snowflake 알고리즘으로 id를 생성합니다.`_ |
| **netx.node-name** | pay-1     | _`$netx.group`_ 에 참여할 서버의 이름입니다. 같은 그룹내에 중복된 이름이 존재하면 안됩니다.                                                                        |
| **netx.undo.mode** | redis     | 트랜잭션 undo 상태 저장에 사용할 저장소 구현체의 mode 입니다.                                                                                            |
| **netx.undo.host** | localhost | 트랜잭션 undo 상태 저장에 사용할 저장소의 host url 입니다.                                                                                            |
| **netx.undo.port** | 6380      | 트랜잭션 undo 상태 저장에 사용할 저장소의 port 입니다.                                                                                                |

### Usage example

#### Scenario1. Start pay transaction

```kotlin
fun pay(param: Any): Mono<Any> {
    return transactionManager.start("paid=1000") // Start distributed transaction and publish transaction start event
        .flatMap { transactionId ->
            service.pay(param)
                .doOnError { throwable ->
                    transactionManager.rollback(
                        transactionId,
                        throwable.message
                    ) // Publish rollback event to all transaction joined node
                }
        }.doOnSuccess { transactionId ->
            transactionManager.commit(transactionId) // Publish commit event to all transaction joined node
        }
}
```

#### Scenario2. Join order transaction

```kotlin
fun order(param: Any): Mono<Any> {
    return transactionManager.join(
        param.transactionId,
        "orderId=1:state=PENDING"
    ) // join exists distributed transaction and publish transaction join event
        .flatMap { transactionId ->
            service.order(param)
                .doOnError { throwable ->
                    transactionManager.rollback(transactionId, throwable.message)
                }
        }.doOnSuccess { transactionId ->
            transactionManager.commit(transactionId)
        }
}
```

#### Scenario3. Check exists transaction

```kotlin
fun exists(param: Any): Mono<Any> {
    return transactionManager.exists(param.transactionId) // Find any transaction has ever been started 
}
```

#### Scenario4. Handle transaction event

다른 분산서버가 (혹은 자기자신이) transactionManager를 통해서 트랜잭션을 시작하거나 트랜잭션 상태를 변경했을때, 호출한 메소드에 맞는 트랜잭션 이벤트를
발행합니다.   
이 이벤트들을 핸들링 함으로써, 다른서버에서 발생한 에러등을 수신하고 롤백할 수 있습니다.

```kotlin

@EventListener(TransactionStartEvent::class)
fun handleTransactionStartEvent(event: TransactionStartEvent) {
    // ...
}

@EventListener(TransactionJoinEvent::class)
fun handleTransactionJoinEvent(event: TransactionJoinEvent) {
    // ...
}

@EventListener(TransactionCommitEvent::class)
fun handleTransactionCommitEvent(event: TransactionCommitEvent) {
    // ...
}

@EventListener(TransactionRollbackEvent::class)
fun handleTransactionRollbackEvent(event: TransactionRollbackEvent) {
    // ...
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
