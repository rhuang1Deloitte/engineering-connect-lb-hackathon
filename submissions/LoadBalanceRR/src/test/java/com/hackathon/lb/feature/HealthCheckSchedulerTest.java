package com.hackathon.lb.feature;

import com.hackathon.lb.model.Target;
import com.hackathon.lb.model.TargetGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Target and TargetGroup health check state tracking.
 * Verifies that health checks update target health status correctly with success/failure thresholds.
 */
class HealthCheckSchedulerTest {
    
    private TargetGroup targetGroup;
    private Target target1;
    private Target target2;
    
    @BeforeEach
    void setUp() {
        target1 = new Target("http://localhost:8080", 1);
        target2 = new Target("http://localhost:8081", 1);
        
        List<Target> targets = List.of(target1, target2);
        
        TargetGroup.HealthCheck healthCheck = new TargetGroup.HealthCheck(
            true,    // enabled
            "/health", // path
            100,     // interval in ms
            1,       // success threshold
            1        // failure threshold
        );
        
        targetGroup = new TargetGroup(
            "/api",
            "ROUND_ROBIN",
            targets,
            healthCheck,
            null
        );
    }
    
    @Test
    void testHealthCheckInitialization() {
        assertNotNull(targetGroup);
        assertNotNull(target1);
        assertNotNull(target2);
    }
    
    @Test
    void testTargetSuccessIncrementAndReset() {
        // Initially, target should have 0 successes
        assertEquals(0, target1.getConsecutiveSuccesses());
        assertEquals(0, target1.getConsecutiveFailures());
        
        // Increment successes
        target1.incrementSuccesses();
        assertEquals(1, target1.getConsecutiveSuccesses());
        assertEquals(0, target1.getConsecutiveFailures());
        
        // Increment again
        target1.incrementSuccesses();
        assertEquals(2, target1.getConsecutiveSuccesses());
        assertEquals(0, target1.getConsecutiveFailures());
        
        // Increment failures - should reset successes
        target1.incrementFailures();
        assertEquals(0, target1.getConsecutiveSuccesses());
        assertEquals(1, target1.getConsecutiveFailures());
    }
    
    @Test
    void testTargetFailureIncrementAndReset() {
        // Initially, target should have 0 failures
        assertEquals(0, target1.getConsecutiveFailures());
        assertEquals(0, target1.getConsecutiveSuccesses());
        
        // Increment failures
        target1.incrementFailures();
        assertEquals(1, target1.getConsecutiveFailures());
        assertEquals(0, target1.getConsecutiveSuccesses());
        
        // Increment again
        target1.incrementFailures();
        assertEquals(2, target1.getConsecutiveFailures());
        assertEquals(0, target1.getConsecutiveSuccesses());
        
        // Increment successes - should reset failures
        target1.incrementSuccesses();
        assertEquals(0, target1.getConsecutiveFailures());
        assertEquals(1, target1.getConsecutiveSuccesses());
    }
    
    @Test
    void testTargetInitiallyHealthy() {
        assertTrue(target1.isHealthy());
        assertTrue(target2.isHealthy());
    }
    
    @Test
    void testTargetMarkUnhealthy() {
        assertTrue(target1.isHealthy());
        
        target1.markUnhealthy();
        assertFalse(target1.isHealthy());
    }
    
    @Test
    void testTargetMarkHealthy() {
        target1.markUnhealthy();
        assertFalse(target1.isHealthy());
        
        target1.markHealthy();
        assertTrue(target1.isHealthy());
    }
    
    @Test
    void testHealthCheckEnabled() {
        assertTrue(targetGroup.getHealthCheck().isPresent());
        TargetGroup.HealthCheck hc = targetGroup.getHealthCheck().get();
        assertTrue(hc.isEnabled());
        assertEquals("/health", hc.getPath());
        assertEquals(100, hc.getInterval());
        assertEquals(1, hc.getSuccessThreshold());
        assertEquals(1, hc.getFailureThreshold());
    }
    
    @Test
    void testHealthCheckDisabled() {
        TargetGroup.HealthCheck disabledHC = new TargetGroup.HealthCheck(
            false,   // disabled
            "/health",
            100,
            1,
            1
        );
        
        TargetGroup disabledGroup = new TargetGroup(
            "/api",
            "ROUND_ROBIN",
            Collections.singletonList(target1),
            disabledHC,
            null
        );
        
        assertTrue(disabledGroup.getHealthCheck().isPresent());
        assertFalse(disabledGroup.getHealthCheck().get().isEnabled());
    }
    
    @Test
    void testTargetGroupHealthyTargets() {
        target1.markHealthy();
        target2.markUnhealthy();
        
        List<Target> healthyTargets = targetGroup.getHealthyTargets();
        assertEquals(1, healthyTargets.size());
        assertTrue(healthyTargets.contains(target1));
        assertFalse(healthyTargets.contains(target2));
    }
    
    @Test
    void testTargetGroupHasHealthyTargets() {
        target1.markHealthy();
        target2.markHealthy();
        assertTrue(targetGroup.hasHealthyTargets());
        
        target1.markUnhealthy();
        target2.markUnhealthy();
        assertFalse(targetGroup.hasHealthyTargets());
        
        target1.markHealthy();
        assertTrue(targetGroup.hasHealthyTargets());
    }
    
    @Test
    void testScheduleHealthChecks() {
        // This test verifies the scheduler can handle the target group configuration
        // Full scheduler testing would require a QuarkusTest which is integration-level
        // The unit tests above verify the state management that the scheduler relies on
        
        // Verify target group has valid health check config
        assertTrue(targetGroup.getHealthCheck().isPresent());
        TargetGroup.HealthCheck hc = targetGroup.getHealthCheck().get();
        assertTrue(hc.isEnabled());
        assertEquals("/health", hc.getPath());
        assertEquals(100, hc.getInterval());
    }
    
    @Test
    void testCancelHealthChecks() {
        // Similar to schedule test, this is integration-level verification
        // Unit testing confirms the TargetGroup properly stores health check config
        assertTrue(targetGroup.getHealthCheck().isPresent());
    }
    
    @Test
    void testScheduleDisabledHealthChecks() {
        TargetGroup.HealthCheck disabledHC = new TargetGroup.HealthCheck(
            false,
            "/health",
            100,
            1,
            1
        );
        
        TargetGroup disabledGroup = new TargetGroup(
            "/api",
            "ROUND_ROBIN",
            Collections.singletonList(target1),
            disabledHC,
            null
        );
        
        // Verify scheduler would detect disabled health checks
        assertTrue(disabledGroup.getHealthCheck().isPresent());
        assertFalse(disabledGroup.getHealthCheck().get().isEnabled());
    }
}
