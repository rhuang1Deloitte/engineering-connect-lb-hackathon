package com.hackathon.lb.algorithm;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import com.hackathon.lb.model.RequestContext;
import com.hackathon.lb.model.Target;
import com.hackathon.lb.model.TargetGroup;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Round-robin load balancing algorithm.
 * Distributes requests evenly across all healthy targets in sequential order.
 */
@ApplicationScoped
public class RoundRobinAlgorithm implements LoadBalancingAlgorithm {
    
    private static final String NAME = "ROUND_ROBIN";
    private final AtomicInteger counter = new AtomicInteger(0);
    
    /**
     * Selects the next healthy target using a circular counter to ensure even distribution.
     */
    @Override
    public Optional<Target> selectTarget(TargetGroup targetGroup, RequestContext requestContext) {
        List<Target> healthyTargets = targetGroup.getHealthyTargets();
        
        if (healthyTargets.isEmpty()) {
            return Optional.empty();
        }
        
        // Get next index using round-robin
        int index = Math.abs(counter.getAndIncrement() % healthyTargets.size());
        return Optional.of(healthyTargets.get(index));
    }
    
    /**
     * Gets the constant name for the round-robin algorithm.
     */
    @Override
    public String getName() {
        return NAME;
    }
}
