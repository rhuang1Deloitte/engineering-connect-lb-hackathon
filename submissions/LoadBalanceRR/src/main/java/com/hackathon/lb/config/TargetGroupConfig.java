package com.hackathon.lb.config;

import java.util.List;
import java.util.Optional;

import io.smallrye.config.WithName;
import jakarta.validation.constraints.Pattern;

/**
 * Configuration for a target group (backend service).
 * Defines how requests are routed to a set of target servers.
 */
public interface TargetGroupConfig {

    /**
     * URL path prefix that triggers routing to this target group.
     * Mandatory field.
     */
    @WithName("path")
    String path();

    /**
     * Load balancing algorithm specific to this target group.
     * If not specified, uses the default algorithm from LoadBalancerConfig.
     */
    
    @WithName("algorithm")
    Optional<@Pattern(regexp = "ROUND_ROBIN|WEIGHTED|STICKY|LRT") String> algorithm();

    /**
     * List of target servers in this target group.
     * Mandatory field - must have at least one target.
     */
    @WithName("targets")
    List<TargetConfig> targets();

    /**
     * Health check configuration for targets in this group.
     * Optional - if not specified, health checks are disabled.
     */
    @WithName("healthCheck")
    Optional<HealthCheckConfig> healthCheck();

    /**
     * Path prefix to strip from the URI before forwarding to target.
     * Optional - if not specified, no path rewriting is performed.
     * Example: if pathRewrite="/api" and request is "/api/users", 
     * the forwarded request will be "/users".
     */
    @WithName("pathRewrite")
    Optional<String> pathRewrite();
}
