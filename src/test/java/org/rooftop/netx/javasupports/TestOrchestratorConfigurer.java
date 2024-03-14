package org.rooftop.netx.javasupports;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.rooftop.netx.api.Orchestrator;
import org.rooftop.netx.meta.OrchestratorConfigurer;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Mono;

public class TestOrchestratorConfigurer extends OrchestratorConfigurer {

    @Bean
    public Orchestrator<Integer> javaSupportsOrchestrator() {
        var atomicI = new AtomicInteger(1);
        return newOrchestrator()
            .startSync(List.of(IllegalArgumentException.class), orchestrateRequest -> {
                if (atomicI.getAndIncrement() <= 1) {
                    throw new IllegalArgumentException("startSync atomicI <= 1");
                }
                return "Complete startSync";
            })
            .joinSync(orchestrateRequest -> "Complete joinSync")
            .join(orchestrateRequest -> Mono.just("Complete join"))
            .commitSync(orchestrateRequest -> "Complete")
            .build();
    }
}
