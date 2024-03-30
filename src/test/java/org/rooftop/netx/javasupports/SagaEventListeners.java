package org.rooftop.netx.javasupports;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.assertj.core.api.Assertions;
import org.rooftop.netx.api.SuccessWith;
import org.rooftop.netx.api.SagaCommitEvent;
import org.rooftop.netx.api.SagaCommitListener;
import org.rooftop.netx.api.SagaJoinEvent;
import org.rooftop.netx.api.SagaJoinListener;
import org.rooftop.netx.api.SagaRollbackEvent;
import org.rooftop.netx.api.SagaRollbackListener;
import org.rooftop.netx.api.SagaStartEvent;
import org.rooftop.netx.api.SagaStartListener;
import org.rooftop.netx.meta.SagaHandler;
import reactor.core.publisher.Mono;

@SagaHandler
public class SagaEventListeners {

    private final Map<String, Integer> receivedSagas = new ConcurrentHashMap<>();

    public void clear() {
        receivedSagas.clear();
    }

    public void assertSagaCount(String sagaState, int count) {
        Assertions.assertThat(receivedSagas.getOrDefault(sagaState, 0))
            .isEqualTo(count);
    }

    @SagaStartListener(
        event = Event.class,
        noRollbackFor = IllegalArgumentException.class,
        successWith = SuccessWith.PUBLISH_JOIN
    )
    public void listenSagaStartEvent(SagaStartEvent sagaStartEvent) {
        incrementSaga("START");
        Event event = sagaStartEvent.decodeEvent(Event.class);
        sagaStartEvent.setNextEvent(event);
    }

    @SagaJoinListener(
        event = Event.class,
        successWith = SuccessWith.PUBLISH_COMMIT
    )
    public void listenSagaJoinEvent(SagaJoinEvent sagaJoinEvent) {
        incrementSaga("JOIN");
        Event event = sagaJoinEvent.decodeEvent(Event.class);
        sagaJoinEvent.setNextEvent(event);
        if (event.event() < 0) {
            throw new IllegalArgumentException();
        }
    }

    @SagaCommitListener
    public Mono<Long> listenSagaCommitEvent(SagaCommitEvent sagaCommitEvent) {
        incrementSaga("COMMIT");
        return Mono.just(1L);
    }

    @SagaRollbackListener(event = Event.class)
    public String listenSagaRollbackEvent(SagaRollbackEvent sagaRollbackEvent) {
        incrementSaga("ROLLBACK");
        sagaRollbackEvent.decodeEvent(Event.class);
        return "listenSagaRollbackEvent";
    }

    private void incrementSaga(String sagaState) {
        receivedSagas.put(sagaState,
            receivedSagas.getOrDefault(sagaState, 0) + 1);
    }
}
