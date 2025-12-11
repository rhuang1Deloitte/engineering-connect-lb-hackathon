package com.hackathon.lb.feature;

import com.hackathon.lb.model.Target;
import com.hackathon.lb.model.TargetGroup;
import com.hackathon.lb.model.TargetGroupRegistry;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Manages periodic health checks for all targets in target groups.
 * Uses Quarkus @Scheduled annotation for reliable, configurable task scheduling.
 */
@ApplicationScoped
public class HealthCheckScheduler {
    
    @Inject
    Vertx vertx;
    
    @Inject
    TargetGroupRegistry targetGroupRegistry;
    
    /**
     * Performs health checks for all target groups.
     * Executed periodically according to the health check interval of each group.
     * This method runs as a background task and gracefully handles errors.
     */
    @Scheduled(every = "1s", delayed = "5s")
    void checkAllTargetGroupsHealth() {
        long currentTime = System.currentTimeMillis();
        
        for (TargetGroup targetGroup : targetGroupRegistry.getAllTargetGroups()) {
            if (targetGroup.getHealthCheck().isEmpty() || !targetGroup.getHealthCheck().get().isEnabled()) {
                continue;
            }
            
            TargetGroup.HealthCheck healthCheckConfig = targetGroup.getHealthCheck().get();
            int interval = healthCheckConfig.getInterval();
            
            // Only perform health checks for targets that haven't been checked recently
            for (Target target : targetGroup.getTargets()) {
                long lastCheck = target.getLastHealthCheckTime();
                if (currentTime - lastCheck >= interval) {
                    performHealthCheck(target, healthCheckConfig, targetGroup.getPath());
                }
            }
        }
    }
    
    /**
     * Performs a single health check for a target.
     * Updates the target's consecutive success/failure counters and health status.
     * 
     * @param target the target to check
     * @param healthCheckConfig the health check configuration
     * @param targetGroupPath the path of the target group (for logging)
     */
    private void performHealthCheck(Target target, TargetGroup.HealthCheck healthCheckConfig, String targetGroupPath) {
        String healthCheckPath = healthCheckConfig.getPath();
        int successThreshold = healthCheckConfig.getSuccessThreshold();
        int failureThreshold = healthCheckConfig.getFailureThreshold();
        
        // Update the last health check time immediately
        target.setLastHealthCheckTime(System.currentTimeMillis());
        
        try {
            URI targetUri = new URI(target.getUrl());
            String host = targetUri.getHost();
            int port = targetUri.getPort();
            if (port == -1) {
                port = 80; // Default HTTP port
            }
            
            // Build the health check request URI
            String healthCheckUri = buildHealthCheckPath(targetUri, healthCheckPath);
            
            HttpClient client = vertx.createHttpClient();
            
            client.request(HttpMethod.GET, port, host, healthCheckUri)
                .flatMap(request -> request.send())
                .subscribe().with(
                    response -> {
                        // A successful health check is a 200 response
                        if (response.statusCode() == 200) {
                            handleHealthCheckSuccess(target, successThreshold, targetGroupPath);
                        } else {
                            handleHealthCheckFailure(target, failureThreshold, targetGroupPath);
                        }
                    },
                    failure -> {
                        Log.debugf(failure, "Health check failed for target: %s", target.getUrl());
                        handleHealthCheckFailure(target, failureThreshold, targetGroupPath);
                    }
                );
        } catch (URISyntaxException e) {
            Log.warnf(e, "Failed to parse target URL for health check: %s", target.getUrl());
        }
    }
    
    /**
     * Handles a successful health check.
     * Increments consecutive successes and marks target healthy if threshold is reached.
     * 
     * @param target the target that passed health check
     * @param successThreshold the number of consecutive successes needed
     * @param targetGroupPath the path of the target group (for logging)
     */
    private void handleHealthCheckSuccess(Target target, int successThreshold, String targetGroupPath) {
        target.incrementSuccesses();
        int currentSuccesses = target.getConsecutiveSuccesses();
        
        if (currentSuccesses >= successThreshold && !target.isHealthy()) {
            // Target is becoming healthy
            target.markHealthy();
            Log.infof("Target %s in group '%s' is now HEALTHY (success threshold %d reached)", 
                target.getUrl(), targetGroupPath, successThreshold);
        }
    }
    
    /**
     * Handles a failed health check.
     * Increments consecutive failures and marks target unhealthy if threshold is reached.
     * Logs a single failure or warns when target becomes unhealthy.
     * 
     * @param target the target that failed health check
     * @param failureThreshold the number of consecutive failures needed
     * @param targetGroupPath the path of the target group (for logging)
     */
    private void handleHealthCheckFailure(Target target, int failureThreshold, String targetGroupPath) {
        target.incrementFailures();
        int currentFailures = target.getConsecutiveFailures();
        
        // Always log a single failure at info level
        Log.infof("Target %s in group '%s' failed health check (failures: %d/%d)", 
            target.getUrl(), targetGroupPath, currentFailures, failureThreshold);
        
        if (currentFailures >= failureThreshold && target.isHealthy()) {
            // Target is becoming unhealthy
            target.markUnhealthy();
            Log.warnf("Target %s in group '%s' is now UNHEALTHY (failure threshold %d reached)", 
                target.getUrl(), targetGroupPath, failureThreshold);
        }
    }
    
    /**
     * Builds the full health check path by combining the target's base URI with the health check path.
     * 
     * @param targetUri the URI of the target
     * @param healthCheckPath the health check path from configuration
     * @return the full health check URI path
     */
    private String buildHealthCheckPath(URI targetUri, String healthCheckPath) {
        String basePath = targetUri.getPath();
        if (basePath == null || basePath.isEmpty()) {
            basePath = "/";
        }
        
        // Normalize paths
        if (!basePath.endsWith("/") && !healthCheckPath.startsWith("/")) {
            return basePath + "/" + healthCheckPath;
        } else if (basePath.endsWith("/") && healthCheckPath.startsWith("/")) {
            return basePath + healthCheckPath.substring(1);
        } else {
            return basePath + healthCheckPath;
        }
    }
}
