package org.rooftop.netx.javasupports;

import org.rooftop.netx.api.TransactionManager;
import org.springframework.boot.test.context.TestComponent;

@TestComponent
public class TransactionClient {

    private final TransactionManager transactionManager;

    public TransactionClient(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    public String startTransaction(Undo undo, Event event) {
        return transactionManager.syncStart(undo, event);
    }

    public void joinTransaction(String transactionId, Undo undo, Event event) {
        transactionManager.syncJoin(transactionId, undo, event);
    }

    public void commitTransaction(String transactionId) {
        transactionManager.syncCommit(transactionId);
    }

    public void rollbackTransaction(String transactionId, String cause) {
        transactionManager.syncRollback(transactionId, cause);
    }
}
