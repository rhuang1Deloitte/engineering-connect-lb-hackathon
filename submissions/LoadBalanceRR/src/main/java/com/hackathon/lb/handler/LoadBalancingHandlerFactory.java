package com.hackathon.lb.handler;

import com.hackathon.lb.algorithm.LoadBalancingAlgorithm;
import com.hackathon.lb.algorithm.LoadBalancingAlgorithmRegistry;
import com.hackathon.lb.model.TargetGroup;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Factory for creating parameterized LoadBalancingHandler instances.
 * Each instance is bound to a specific LoadBalancingAlgorithm for a target group,
 * eliminating the need for runtime algorithm lookups in the request handling path.
 */
@ApplicationScoped
public class LoadBalancingHandlerFactory {
    
    @Inject
    LoadBalancingAlgorithmRegistry algorithmRegistry;
    
    /**
     * Creates a new LoadBalancingHandler bound to the algorithm specified by the target group.
     * 
     * @param targetGroup the target group whose algorithm will be used
     * @return a new handler instance pre-bound to the algorithm, or null if algorithm not found
     */
    public LoadBalancingHandler create(TargetGroup targetGroup) {
        String algorithmName = targetGroup.getAlgorithm();
        LoadBalancingAlgorithm algorithm = algorithmRegistry.getAlgorithm(algorithmName);
        
        if (algorithm == null) {
            Log.errorf("Algorithm '%s' not found in registry for target group '%s'", 
                algorithmName, targetGroup.getPath());
            return null;
        }
        
        return new LoadBalancingHandler(algorithm);
    }
}
