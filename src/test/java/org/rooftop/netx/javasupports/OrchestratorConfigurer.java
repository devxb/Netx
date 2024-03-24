package org.rooftop.netx.javasupports;

import org.rooftop.netx.api.Orchestrator;
import org.rooftop.netx.engine.OrchestratorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrchestratorConfigurer {

    @Autowired
    private OrchestratorFactory orchestratorFactory;

    @Bean
    public Orchestrator<Integer, Integer> intOrchestrator() {
        return orchestratorFactory.<Integer>create("intOrchestrator")
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
