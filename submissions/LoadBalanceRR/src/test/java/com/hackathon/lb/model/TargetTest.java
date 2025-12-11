package com.hackathon.lb.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Verifies core Target state transitions and tracking helpers.
 */
class TargetTest {
    
    /**
     * Confirms default health and connection state at creation.
     */
    @Test
    void testTargetCreation() {
        Target target = new Target("http://localhost:8080", 1);
        
        assertEquals("http://localhost:8080", target.getUrl());
        assertEquals(1, target.getWeight());
        assertTrue(target.isHealthy(), "Target should be healthy initially");
        assertEquals(0, target.getActiveConnections());
    }
    
    /**
     * Exercises healthy/unhealthy transitions and counters.
     */
    @Test
    void testHealthStatus() {
        Target target = new Target("http://localhost:8080", 1);
        
        // Initially healthy
        assertTrue(target.isHealthy());
        assertEquals(0, target.getConsecutiveSuccesses());
        assertEquals(0, target.getConsecutiveFailures());
        
        // Mark unhealthy
        target.markUnhealthy();
        assertFalse(target.isHealthy());
        assertEquals(0, target.getConsecutiveSuccesses());
        assertEquals(1, target.getConsecutiveFailures());
        
        // Mark unhealthy again
        target.markUnhealthy();
        assertEquals(2, target.getConsecutiveFailures());
        
        // Mark healthy
        target.markHealthy();
        assertTrue(target.isHealthy());
        assertEquals(1, target.getConsecutiveSuccesses());
        assertEquals(0, target.getConsecutiveFailures());
    }
    
    /**
     * Ensures connection counters mutate as expected.
     */
    @Test
    void testActiveConnections() {
        Target target = new Target("http://localhost:8080", 1);
        
        assertEquals(0, target.getActiveConnections());
        
        target.incrementConnections();
        assertEquals(1, target.getActiveConnections());
        
        target.incrementConnections();
        assertEquals(2, target.getActiveConnections());
        
        target.decrementConnections();
        assertEquals(1, target.getActiveConnections());
    }
    
    /**
     * Verifies the health check timestamp accessor and mutator.
     */
    @Test
    void testHealthCheckTimestamp() {
        Target target = new Target("http://localhost:8080", 1);
        
        assertEquals(0, target.getLastHealthCheckTime());
        
        long timestamp = System.currentTimeMillis();
        target.setLastHealthCheckTime(timestamp);
        assertEquals(timestamp, target.getLastHealthCheckTime());
    }
    
    /**
     * Ensures the configured weight is preserved.
     */
    @Test
    void testWeightedTarget() {
        Target target = new Target("http://localhost:8080", 5);
        assertEquals(5, target.getWeight());
    }
    
    /**
     * Asserts toString includes key target metadata for logging.
     */
    @Test
    void testToString() {
        Target target = new Target("http://localhost:8080", 2);
        String str = target.toString();
        
        assertTrue(str.contains("http://localhost:8080"));
        assertTrue(str.contains("weight=2"));
        assertTrue(str.contains("healthy=true"));
        assertTrue(str.contains("activeConnections=0"));
    }
}
