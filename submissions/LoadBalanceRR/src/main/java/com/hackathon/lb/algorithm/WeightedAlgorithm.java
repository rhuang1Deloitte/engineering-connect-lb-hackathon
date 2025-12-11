package com.hackathon.lb.algorithm;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import com.hackathon.lb.model.RequestContext;
import com.hackathon.lb.model.Target;
import com.hackathon.lb.model.TargetGroup;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Weighted load balancing algorithm.
 * Distributes requests based on target weights - higher weight targets receive more requests.
 * Uses weighted random selection for distribution.
 */
@ApplicationScoped
public class WeightedAlgorithm implements LoadBalancingAlgorithm {
    
    private static final String NAME = "WEIGHTED";
    
    /**
     * Chooses a target by accounting for each target's configured weight.
     */
    @Override
    public Optional<Target> selectTarget(TargetGroup targetGroup, RequestContext requestContext) {
        List<Target> healthyTargets = targetGroup.getHealthyTargets();
        
        if (healthyTargets.isEmpty()) {
            return Optional.empty();
        }
        
        // Calculate total weight
        int totalWeight = healthyTargets.stream()
            .mapToInt(Target::getWeight)
            .sum();
        
        if (totalWeight == 0) {
            // Fallback to simple random selection if all weights are 0
            int index = ThreadLocalRandom.current().nextInt(healthyTargets.size());
            return Optional.of(healthyTargets.get(index));
        }
        
        // Weighted random selection
        int random = ThreadLocalRandom.current().nextInt(totalWeight);
        int cumulativeWeight = 0;
        
        for (Target target : healthyTargets) {
            cumulativeWeight += target.getWeight();
            if (random < cumulativeWeight) {
                return Optional.of(target);
            }
        }
        
        // Fallback (shouldn't reach here, but return last target if we do)
        return Optional.of(healthyTargets.get(healthyTargets.size() - 1));
    }
    
    /**
     * Returns the stable name for the weighted algorithm.
     */
    @Override
    public String getName() {
        return NAME;
    }
}
