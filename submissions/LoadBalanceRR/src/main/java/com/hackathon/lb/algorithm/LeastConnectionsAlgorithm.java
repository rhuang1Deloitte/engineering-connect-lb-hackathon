package com.hackathon.lb.algorithm;

import java.util.List;
import java.util.Optional;

import com.hackathon.lb.model.RequestContext;
import com.hackathon.lb.model.Target;
import com.hackathon.lb.model.TargetGroup;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Least Response Time (LRT) load balancing algorithm.
 * Selects the target with the fewest active connections.
 * This is a proxy for response time - fewer connections typically means faster responses.
 */
@ApplicationScoped
public class LeastConnectionsAlgorithm implements LoadBalancingAlgorithm {
    
    private static final String NAME = "LRT";
    
    /**
     * Picks the healthy target that currently has the fewest active connections.
     */
    @Override
    public Optional<Target> selectTarget(TargetGroup targetGroup, RequestContext requestContext) {
        List<Target> healthyTargets = targetGroup.getHealthyTargets();
        
        if (healthyTargets.isEmpty()) {
            return Optional.empty();
        }
        
        // Find target with minimum active connections
        Target selected = healthyTargets.get(0);
        int minConnections = selected.getActiveConnections();
        
        for (int i = 1; i < healthyTargets.size(); i++) {
            Target target = healthyTargets.get(i);
            int connections = target.getActiveConnections();
            
            if (connections < minConnections) {
                minConnections = connections;
                selected = target;
            }
        }
        
        return Optional.of(selected);
    }
    
    /**
     * Returns the fixed identifier for the least-connections algorithm.
     */
    @Override
    public String getName() {
        return NAME;
    }
}
