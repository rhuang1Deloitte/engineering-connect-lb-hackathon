package com.hackathon.lb.config;

import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import jakarta.validation.constraints.Min;

/**
 * Health check configuration for a target group.
 * Defines how and when health checks are performed on target servers.
 */
public interface HealthCheckConfig {

    /**
     * Whether health checks are enabled for this target group.
     * Defaults to false if not specified.
     */
    @WithName("enabled")
    @WithDefault("false")
    boolean enabled();

    /**
     * URL path for the health check endpoint (e.g., /healthz).
     * Mandatory if health checks are enabled.
     */
    @WithName("path")
    @WithDefault("/")
    String path();

    /**
     * Interval in milliseconds between health check attempts.
     * Defaults to 5000 (5 seconds) if not specified.
     */
    @WithName("interval")
    @Min(1)
    @WithDefault("5000")
    int interval();

    /**
     * Number of successful health checks required to mark a target as healthy.
     * Defaults to 1 if not specified.
     */
    @WithName("successThreshold")
    @Min(1)
    @WithDefault("1")
    int successThreshold();

    /**
     * Number of failed health checks required to mark a target as unhealthy.
     * Defaults to 3 if not specified.
     */
    @WithName("failureThreshold")
    @Min(1)
    @WithDefault("3")
    int failureThreshold();
}
