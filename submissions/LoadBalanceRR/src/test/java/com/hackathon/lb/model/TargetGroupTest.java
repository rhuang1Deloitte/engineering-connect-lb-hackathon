package com.hackathon.lb.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Confirms TargetGroup aggregates targets and health checks as expected.
 */
class TargetGroupTest {
    
    /**
     * Ensures path, algorithm, and target list are preserved on creation.
     */
    @Test
    void testBasicConstruction() {
        List<Target> targets = Arrays.asList(
            new Target("http://localhost:8080", 1),
            new Target("http://localhost:8081", 1)
        );
        
        TargetGroup group = new TargetGroup("/api", "ROUND_ROBIN", targets, null, null);
        
        assertEquals("/api", group.getPath());
        assertEquals("ROUND_ROBIN", group.getAlgorithm());
        assertEquals(2, group.getTargets().size());
        assertTrue(group.hasHealthyTargets());
        assertFalse(group.getHealthCheck().isPresent());
    }
    
    /**
     * Verifies healthy target filtering reacts to health state changes.
     */
    @Test
    void testHealthyTargetsFiltering() {
        Target target1 = new Target("http://localhost:8080", 1);
        Target target2 = new Target("http://localhost:8081", 1);
        Target target3 = new Target("http://localhost:8082", 1);
        
        List<Target> targets = Arrays.asList(target1, target2, target3);
        TargetGroup group = new TargetGroup("/api", "ROUND_ROBIN", targets, null, null);
        
        // All healthy initially
        assertEquals(3, group.getHealthyTargets().size());
        assertTrue(group.hasHealthyTargets());
        
        // Mark one as unhealthy
        target2.markUnhealthy();
        assertEquals(2, group.getHealthyTargets().size());
        assertTrue(group.hasHealthyTargets());
        
        // Mark all as unhealthy
        target1.markUnhealthy();
        target3.markUnhealthy();
        assertEquals(0, group.getHealthyTargets().size());
        assertFalse(group.hasHealthyTargets());
    }
    
    /**
     * Ensures provided health check data is exposed through the getter.
     */
    @Test
    void testTargetGroupWithHealthCheck() {
        List<Target> targets = Arrays.asList(
            new Target("http://localhost:8080", 1)
        );
        
        TargetGroup.HealthCheck healthCheck = new TargetGroup.HealthCheck(
            true, "/health", 5000, 2, 3
        );
        
        TargetGroup group = new TargetGroup("/api", "ROUND_ROBIN", targets, healthCheck, null);
        
        assertTrue(group.getHealthCheck().isPresent());
        assertTrue(group.getHealthCheck().get().isEnabled());
        assertEquals("/health", group.getHealthCheck().get().getPath());
        assertEquals(5000, group.getHealthCheck().get().getInterval());
        assertEquals(2, group.getHealthCheck().get().getSuccessThreshold());
        assertEquals(3, group.getHealthCheck().get().getFailureThreshold());
    }
    
    /**
     * Validates the HealthCheck helper records its configuration.
     */
    @Test
    void testHealthCheckCreation() {
        TargetGroup.HealthCheck healthCheck = new TargetGroup.HealthCheck(
            true, "/healthz", 10000, 1, 5
        );
        
        assertTrue(healthCheck.isEnabled());
        assertEquals("/healthz", healthCheck.getPath());
        assertEquals(10000, healthCheck.getInterval());
        assertEquals(1, healthCheck.getSuccessThreshold());
        assertEquals(5, healthCheck.getFailureThreshold());
    }
    
    /**
     * Asserts toString surfaces the configured health check fields.
     */
    @Test
    void testHealthCheckToString() {
        TargetGroup.HealthCheck healthCheck = new TargetGroup.HealthCheck(
            true, "/health", 5000, 2, 3
        );
        
        String str = healthCheck.toString();
        assertTrue(str.contains("enabled=true"));
        assertTrue(str.contains("path='/health'"));
        assertTrue(str.contains("interval=5000"));
    }
    
    /**
     * Asserts toString surfaces high-level target group metrics.
     */
    @Test
    void testTargetGroupToString() {
        List<Target> targets = Arrays.asList(
            new Target("http://localhost:8080", 1),
            new Target("http://localhost:8081", 1)
        );
        
        TargetGroup group = new TargetGroup("/api", "ROUND_ROBIN", targets, null, null);
        String result = group.toString();
        
        assertTrue(result.contains("path='/api'"));
        assertTrue(result.contains("algorithm='ROUND_ROBIN'"));
        assertTrue(result.contains("targets=2"));
        assertTrue(result.contains("healthyTargets=2"));
    }
}
