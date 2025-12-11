package com.hackathon.lb.model;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Represents a group of backend targets with associated routing and health check configuration.
 * Manages multiple Target instances that serve requests for a specific path prefix.
 */
public class TargetGroup {
    
    private final String path;
    private final String algorithm;
    private final List<Target> targets;
    private final HealthCheck healthCheck;
    private final String pathRewrite;
    
    /**
     * Constructs a target group with its routing logic and health checks.
     */
    public TargetGroup(String path, String algorithm, List<Target> targets, HealthCheck healthCheck, String pathRewrite) {
        this.path = path;
        this.algorithm = algorithm;
        this.targets = targets;
        this.healthCheck = healthCheck;
        this.pathRewrite = pathRewrite;
    }
    
    /**
     * Returns the request path prefix associated with this target group.
     */
    public String getPath() {
        return path;
    }
    
    /**
     * Returns the algorithm override defined for this group or the default algorithm name.
     */
    public String getAlgorithm() {
        return algorithm;
    }
    
    /**
     * Returns all configured targets for this group.
     */
    public List<Target> getTargets() {
        return targets;
    }
    
    /**
     * Returns only the targets that are currently marked healthy.
     */
    public List<Target> getHealthyTargets() {
        return targets.stream()
            .filter(Target::isHealthy)
            .collect(Collectors.toList());
    }
    
    /**
     * Returns the optional health check configuration for this group.
     */
    public Optional<HealthCheck> getHealthCheck() {
        return Optional.ofNullable(healthCheck);
    }

    /**
     * Returns the path prefix to strip from URIs before forwarding to targets.
     * If null, no path rewriting is performed.
     */
    public String getPathRewrite() {
        return pathRewrite;
    }
    
    /**
     * Indicates whether at least one target in the group is healthy.
     */
    public boolean hasHealthyTargets() {
        return targets.stream().anyMatch(Target::isHealthy);
    }
    
    /**
     * Returns a compact description used for logging target group instrumentation.
     */
    @Override
    public String toString() {
        return String.format("TargetGroup{path='%s', algorithm='%s', targets=%d, healthyTargets=%d}", 
            path, algorithm, targets.size(), getHealthyTargets().size());
    }
    
    /**
     * Health check configuration for this target group.
     */
    public static class HealthCheck {
        private final boolean enabled;
        private final String path;
        private final int interval;
        private final int successThreshold;
        private final int failureThreshold;
        
        /**
         * Constructs the runtime health check representation for a target group.
         */
        public HealthCheck(boolean enabled, String path, int interval, int successThreshold, int failureThreshold) {
            this.enabled = enabled;
            this.path = path;
            this.interval = interval;
            this.successThreshold = successThreshold;
            this.failureThreshold = failureThreshold;
        }
        
        /**
         * Indicates whether health checks are active for this target group.
         */
        public boolean isEnabled() {
            return enabled;
        }
        
        /**
         * Returns the health check path relative to the target host.
         */
        public String getPath() {
            return path;
        }
        
        /**
         * Returns how often (in ms) health checks are performed.
         */
        public int getInterval() {
            return interval;
        }
        
        /**
         * Returns how many consecutive successes are required to mark a target healthy.
         */
        public int getSuccessThreshold() {
            return successThreshold;
        }
        
        /**
         * Returns how many consecutive failures are required to mark a target unhealthy.
         */
        public int getFailureThreshold() {
            return failureThreshold;
        }
        
        /**
         * Returns a human readable representation of the health check configuration.
         */
        @Override
        public String toString() {
            return String.format("HealthCheck{enabled=%s, path='%s', interval=%d, successThreshold=%d, failureThreshold=%d}", 
                enabled, path, interval, successThreshold, failureThreshold);
        }
    }
}
