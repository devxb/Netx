package org.rooftop.netx.javasupports;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.assertj.core.api.Assertions;
import org.rooftop.netx.api.DecodeException;
import org.rooftop.netx.api.TransactionCommitEvent;
import org.rooftop.netx.api.TransactionCommitListener;
import org.rooftop.netx.api.TransactionJoinEvent;
import org.rooftop.netx.api.TransactionJoinListener;
import org.rooftop.netx.api.TransactionRollbackEvent;
import org.rooftop.netx.api.TransactionRollbackListener;
import org.rooftop.netx.api.TransactionStartEvent;
import org.rooftop.netx.api.TransactionStartListener;
import org.rooftop.netx.engine.core.TransactionState;
import org.rooftop.netx.meta.TransactionHandler;
import reactor.core.publisher.Mono;

@TransactionHandler
public class TransactionEventListeners {

    private final Map<TransactionState, Integer> receivedTransactions = new ConcurrentHashMap<>();

    public void clear() {
        receivedTransactions.clear();
    }

    public void assertTransactionCount(TransactionState transactionState, int count) {
        Assertions.assertThat(receivedTransactions.getOrDefault(transactionState, 0))
            .isEqualTo(count);
    }

    @TransactionStartListener(
        event = Event.class,
        noRetryFor = IllegalArgumentException.class
    )
    public void listenTransactionStartEvent(TransactionStartEvent transactionStartEvent) {
        incrementTransaction(TransactionState.START);
        Event event = transactionStartEvent.decodeEvent(Event.class);
        if (event.event() < 0) {
            throw new IllegalArgumentException();
        }
    }

    @TransactionJoinListener(
        event = Event.class,
        noRetryFor = IllegalArgumentException.class
    )
    public void listenTransactionJoinEvent(TransactionJoinEvent transactionJoinEvent) {
        incrementTransaction(TransactionState.JOIN);
        Event event = transactionJoinEvent.decodeEvent(Event.class);
        if (event.event() < 0) {
            throw new IllegalArgumentException();
        }
    }

    @TransactionCommitListener
    public Mono<Long> listenTransactionCommitEvent(TransactionCommitEvent transactionCommitEvent) {
        incrementTransaction(TransactionState.COMMIT);
        return Mono.just(1L);
    }

    @TransactionRollbackListener(noRetryFor = DecodeException.class)
    public String listenTransactionRollbackEvent(TransactionRollbackEvent transactionRollbackEvent) {
        incrementTransaction(TransactionState.ROLLBACK);
        transactionRollbackEvent.decodeUndo(Undo.class);
        return "listenTransactionRollbackEvent";
    }

    private void incrementTransaction(TransactionState transactionState) {
        receivedTransactions.put(transactionState,
            receivedTransactions.getOrDefault(transactionState, 0) + 1);
    }
}
