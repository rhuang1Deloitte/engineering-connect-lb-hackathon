package com.hackathon.lb.config;

import java.util.Map;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import jakarta.validation.constraints.Pattern;

/**
 * Root configuration mapping for the load balancer.
 * Maps to the 'lbConfig' section in application.yml/config.yml
 */
@ConfigMapping(prefix = "lbConfig")
public interface LoadBalancerConfig {

    /**
     * Default load balancing algorithm for all target groups.
     * Defaults to ROUND_ROBIN if not specified.
     */
    @WithName("algorithm")
    @Pattern(regexp = "ROUND_ROBIN|WEIGHTED|STICKY|LRT")
    @WithDefault("ROUND_ROBIN")
    String algorithm();

    /**
     * Map of target groups, keyed by target group name.
     * Each target group contains configuration for a backend service.
     */
    @WithName("targetGroups")
    Map<String, TargetGroupConfig> targetGroups();

    /**
     * Global upstream connection timeout in milliseconds.
     */
    @WithName("connectionTimeoutMillis")
    @WithDefault("2000")
    long connectionTimeoutMillis();

    /**
     * Enable header conventions (X-Forwarded-*, X-Real-IP, Host) on upstream requests.
     * Defaults to true if not specified.
     */
    @WithName("headerConventionEnabled")
    @WithDefault("true")
    boolean headerConventionEnabled();

    /**
     * Enable request retries on 5xx errors and connection failures.
     * Defaults to false if not specified.
     */
    @WithName("retryEnabled")
    @WithDefault("false")
    boolean retryEnabled();

    /**
     * Initial backoff duration in milliseconds before retrying a request.
     * Each retry increases this duration exponentially.
     * Defaults to 100ms if not specified.
     */
    @WithName("retryBackoffMillis")
    @WithDefault("100")
    long retryBackoffMillis();

    /**
     * Maximum number of retry attempts before returning an error.
     * Defaults to 3 if not specified.
     */
    @WithName("retryCount")
    @WithDefault("3")
    int retryCount();
}
