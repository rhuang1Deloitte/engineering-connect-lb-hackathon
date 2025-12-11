package com.hackathon.lb.model;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.hackathon.lb.config.LoadBalancerConfig;

import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Registry for managing TargetGroup instances initialized from configuration.
 * Provides lookup methods for handlers to retrieve target groups by name or path.
 */
@ApplicationScoped
public class TargetGroupRegistry {
    
    @Inject
    LoadBalancerConfig config;
    
    private final Map<String, TargetGroup> targetGroups = new ConcurrentHashMap<>();
    
    @PostConstruct
    void initialize() {
        Log.info("Initializing TargetGroups from configuration...");
        
        String defaultAlgorithm = config.algorithm();
        
        config.targetGroups().forEach((name, tgConfig) -> {
            TargetGroup group = TargetGroupFactory.fromConfig(tgConfig, defaultAlgorithm);
            targetGroups.put(name, group);
            Log.infof("Registered TargetGroup '%s': %s", name, group);
        });
        
        Log.infof("Initialized %d target groups", targetGroups.size());
    }
    
    /**
     * Gets a target group by its configured name.
     * 
     * @param name the target group name
     * @return Optional containing the target group if found
     */
    public Optional<TargetGroup> getTargetGroup(String name) {
        return Optional.ofNullable(targetGroups.get(name));
    }
    
    /**
     * Gets a target group that matches the given request path.
     * Uses longest prefix matching to find the most specific target group.
     * 
     * @param path the request path to match
     * @return Optional containing the matching target group if found
     */
    public Optional<TargetGroup> getTargetGroupByPath(String path) {
        return targetGroups.values().stream()
            .filter(tg -> path.startsWith(tg.getPath()))
            .max((tg1, tg2) -> Integer.compare(tg1.getPath().length(), tg2.getPath().length()));
    }
    
    /**
     * Gets all registered target groups.
     * 
     * @return collection of all target groups
     */
    public Collection<TargetGroup> getAllTargetGroups() {
        return targetGroups.values();
    }
    
    /**
     * Gets the number of registered target groups.
     * 
     * @return count of target groups
     */
    public int size() {
        return targetGroups.size();
    }
}
