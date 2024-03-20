package org.rooftop.netx.javasupports;

import java.util.concurrent.TimeUnit;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.rooftop.netx.api.Orchestrator;
import org.rooftop.netx.api.TransactionManager;
import org.rooftop.netx.engine.core.TransactionState;
import org.rooftop.netx.meta.EnableDistributedTransaction;
import org.rooftop.netx.redis.RedisContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@EnableDistributedTransaction
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    RedisContainer.class,
    NetxJavaSupportsTest.class,
    TransactionEventListeners.class,
})
@DisplayName("NetxJavaSupportsTest")
@TestPropertySource("classpath:fast-recover-mode.properties")
class NetxJavaSupportsTest {

    private static final Undo NEGATIVE_UNDO = new Undo(-1L);
    private static final Undo POSITIVE_UNDO = new Undo(1L);
    private static final Event NEGATIVE_EVENT = new Event(-1L);
    private static final Event POSITIVE_EVENT = new Event(1L);

    @Autowired
    private TransactionManager transactionManager;

    @Autowired
    private TransactionEventListeners transactionEventListeners;

    @BeforeEach
    void clear() {
        transactionEventListeners.clear();
    }

    @Test
    @DisplayName("Scenario-1. Start -> Join -> Commit")
    void Scenario1_Start_Join_Commit() {
        String transactionId = transactionManager.syncStart(NEGATIVE_UNDO, NEGATIVE_EVENT);
        transactionManager.syncJoin(transactionId, NEGATIVE_UNDO, NEGATIVE_EVENT);
        transactionManager.syncCommit(transactionId);

        Awaitility.waitAtMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                transactionEventListeners.assertTransactionCount(TransactionState.START, 1);
                transactionEventListeners.assertTransactionCount(TransactionState.JOIN, 1);
                transactionEventListeners.assertTransactionCount(TransactionState.COMMIT, 1);
            });
    }

    @Test
    @DisplayName("Scenario-2. Start -> Join -> Rollback")
    void Transaction_Start_Join_Rollback() {
        String transactionId = transactionManager.syncStart(POSITIVE_UNDO, POSITIVE_EVENT);
        transactionManager.syncJoin(transactionId, POSITIVE_UNDO, POSITIVE_EVENT);
        transactionManager.syncRollback(transactionId, "Scenario-2. Start -> Join -> Rollback");

        Awaitility.waitAtMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                transactionEventListeners.assertTransactionCount(TransactionState.START, 1);
                transactionEventListeners.assertTransactionCount(TransactionState.JOIN, 1);
                transactionEventListeners.assertTransactionCount(TransactionState.ROLLBACK, 1);
            });
    }
}
