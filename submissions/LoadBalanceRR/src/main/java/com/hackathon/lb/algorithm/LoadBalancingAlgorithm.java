package com.hackathon.lb.algorithm;

import java.util.Optional;

import com.hackathon.lb.model.RequestContext;
import com.hackathon.lb.model.Target;
import com.hackathon.lb.model.TargetGroup;

/**
 * Strategy interface for load balancing algorithms.
 * Implementations determine how to select a target from a group of healthy backend servers.
 */
public interface LoadBalancingAlgorithm {
    
    /**
     * Selects a target from the target group for the given request.
     * 
     * @param targetGroup the target group containing available targets
     * @param requestContext the request context containing request details
     * @return an Optional containing the selected target, or empty if no healthy target is available
     */
    Optional<Target> selectTarget(TargetGroup targetGroup, RequestContext requestContext);
    
    /**
     * Returns the name of this algorithm (e.g., "ROUND_ROBIN", "WEIGHTED").
     * 
     * @return the algorithm name
     */
    String getName();
}
