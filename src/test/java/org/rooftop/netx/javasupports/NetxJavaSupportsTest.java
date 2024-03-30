package org.rooftop.netx.javasupports;

import java.util.concurrent.TimeUnit;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.rooftop.netx.api.Orchestrator;
import org.rooftop.netx.api.SagaManager;
import org.rooftop.netx.meta.EnableSaga;
import org.rooftop.netx.redis.RedisContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@EnableSaga
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    RedisContainer.class,
    NetxJavaSupportsTest.class,
    SagaEventListeners.class,
    OrchestratorConfigurer.class,
})
@DisplayName("NetxJavaSupportsTest")
@TestPropertySource("classpath:fast-recover-mode.properties")
class NetxJavaSupportsTest {

    private static final Event NEGATIVE_EVENT = new Event(-1L);
    private static final Event POSITIVE_EVENT = new Event(1L);

    @Autowired
    private SagaManager sagaManager;

    @Autowired
    private SagaEventListeners sagaEventListeners;

    @Autowired
    private Orchestrator<Integer, Integer> orchestrator;

    @BeforeEach
    void clear() {
        sagaEventListeners.clear();
    }

    @Test
    @DisplayName("Scenario-1. Start -> Join -> Commit")
    void Scenario1_Start_Join_Commit() {
        String id = sagaManager.syncStart(POSITIVE_EVENT);

        Awaitility.waitAtMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                sagaEventListeners.assertSagaCount("START", 1);
                sagaEventListeners.assertSagaCount("JOIN", 1);
                sagaEventListeners.assertSagaCount("COMMIT", 1);
            });
    }

    @Test
    @DisplayName("Scenario-2. Start -> Join -> Rollback")
    void Scenario2_Start_Join_Rollback() {
        String id = sagaManager.syncStart(NEGATIVE_EVENT);

        Awaitility.waitAtMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                sagaEventListeners.assertSagaCount("START", 1);
                sagaEventListeners.assertSagaCount("JOIN", 1);
                sagaEventListeners.assertSagaCount("ROLLBACK", 1);
            });
    }

    @Test
    @DisplayName("Scenario-3. Orchestrator add 3 number")
    void Orchestrator_Add_Three_Number() {
        var result = orchestrator.sagaSync(0);

        Assertions.assertThat(result.isSuccess()).isTrue();
        Assertions.assertThat(result.decodeResult(Integer.class)).isEqualTo(3);
    }
}
