package com.hackathon.lb.config;

import java.util.Optional;

import org.hibernate.validator.constraints.URL;

import io.smallrye.config.WithName;
import jakarta.validation.constraints.Min;

/**
 * Configuration for a single target server within a target group.
 * Represents a backend service that receives load-balanced requests.
 */
public interface TargetConfig {

    /**
     * URL of the target server (e.g., http://localhost:8090).
     * Mandatory field.
     */
    @WithName("url")
    @URL(protocol = "http")
    String url();

    /**
     * Weight of this target for WEIGHTED load balancing algorithm.
     * If not specified, defaults to 1.
     * Ignored for other algorithms like ROUND_ROBIN.
     */
    @WithName("weight")
    Optional<@Min(1) Integer> weight();
}
