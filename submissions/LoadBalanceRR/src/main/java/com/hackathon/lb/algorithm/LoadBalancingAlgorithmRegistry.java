package com.hackathon.lb.algorithm;

import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

/**
 * Registry for load balancing algorithms.
 * Provides access to algorithm implementations by name.
 */
@ApplicationScoped
public class LoadBalancingAlgorithmRegistry {
    
    private final Map<String, LoadBalancingAlgorithm> algorithms = new HashMap<>();
    
    /**
     * Injects all discovered algorithms so they can be looked up by name.
     */
    @Inject
    public LoadBalancingAlgorithmRegistry(Instance<LoadBalancingAlgorithm> algorithmInstances) {
        // Register all available algorithm implementations
        for (LoadBalancingAlgorithm algorithm : algorithmInstances) {
            algorithms.put(algorithm.getName(), algorithm);
        }
    }
    
    /**
     * Gets an algorithm by name.
     * 
     * @param name the algorithm name (e.g., "ROUND_ROBIN", "WEIGHTED", "STICKY", "LRT")
     * @return the algorithm implementation, or null if not found
     */
    public LoadBalancingAlgorithm getAlgorithm(String name) {
        return algorithms.get(name);
    }
    
    /**
     * Gets an algorithm by name, with a fallback to a default algorithm.
     * 
     * @param name the algorithm name
     * @param defaultAlgorithm the default algorithm to use if the named one isn't found
     * @return the algorithm implementation, never null
     */
    public LoadBalancingAlgorithm getAlgorithmOrDefault(String name, String defaultAlgorithm) {
        LoadBalancingAlgorithm algorithm = algorithms.get(name);
        if (algorithm == null) {
            algorithm = algorithms.get(defaultAlgorithm);
        }
        return algorithm;
    }
    
    /**
     * Checks if an algorithm is registered.
     * 
     * @param name the algorithm name
     * @return true if the algorithm exists
     */
    public boolean hasAlgorithm(String name) {
        return algorithms.containsKey(name);
    }
}
