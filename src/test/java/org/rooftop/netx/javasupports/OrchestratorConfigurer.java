package org.rooftop.netx.javasupports;

import org.rooftop.netx.api.Orchestrator;
import org.rooftop.netx.api.OrchestratorFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrchestratorConfigurer {

    @Bean
    public Orchestrator<Integer, Integer> intOrchestrator() {
        return OrchestratorFactory.Instance.instance().<Integer>create("intOrchestrator")
            .start(
                request -> request + 1,
                request -> request - 1
            )
            .join(
                request -> request + 1,
                request -> request - 1
            )
            .commit(
                request -> request + 1,
                request -> request - 1
            );
    }
}
