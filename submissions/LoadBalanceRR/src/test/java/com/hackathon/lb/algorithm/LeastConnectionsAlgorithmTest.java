package com.hackathon.lb.algorithm;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.hackathon.lb.model.RequestContext;
import com.hackathon.lb.model.Target;
import com.hackathon.lb.model.TargetGroup;

/**
 * Covers scenarios that should favor targets with the fewest active connections.
 */
class LeastConnectionsAlgorithmTest {
    
    private LeastConnectionsAlgorithm algorithm;
    private List<Target> targets;
    private TargetGroup targetGroup;
    
    @BeforeEach
    void setUp() {
        algorithm = new LeastConnectionsAlgorithm();
        
        targets = Arrays.asList(
            new Target("http://localhost:8080", 1),
            new Target("http://localhost:8081", 1),
            new Target("http://localhost:8082", 1)
        );
        
        targetGroup = new TargetGroup("/api", "LRT", targets, null, null);
    }
    
    /**
     * Ensures the algorithm identifies itself as LRT.
     */
    @Test
    void testGetName() {
        assertEquals("LRT", algorithm.getName());
    }
    
    /**
     * Picks the target that currently tracks the smallest connection count.
     */
    @Test
    void testSelectsLeastConnections() {
        RequestContext context = RequestContext.builder().build();
        
        // Set different connection counts
        targets.get(0).incrementConnections();
        targets.get(0).incrementConnections();
        targets.get(1).incrementConnections();
        // targets.get(2) has 0 connections
        
        Optional<Target> selected = algorithm.selectTarget(targetGroup, context);
        
        assertTrue(selected.isPresent());
        assertEquals(targets.get(2), selected.get());
    }
    
    /**
     * Defaults to the first target when multiple targets share the same count.
     */
    @Test
    void testSelectsFirstWhenEqual() {
        RequestContext context = RequestContext.builder().build();
        
        // All have equal connections (0)
        Optional<Target> selected = algorithm.selectTarget(targetGroup, context);
        
        assertTrue(selected.isPresent());
        assertEquals(targets.get(0), selected.get());
    }
    
    /**
     * Observes that connections update dynamically during selection.
     */
    @Test
    void testDynamicSelection() {
        RequestContext context = RequestContext.builder().build();
        
        // First selection - all equal, should pick first
        Optional<Target> first = algorithm.selectTarget(targetGroup, context);
        assertTrue(first.isPresent());
        first.get().incrementConnections();
        
        // Second selection - should pick one with 0 connections
        Optional<Target> second = algorithm.selectTarget(targetGroup, context);
        assertTrue(second.isPresent());
        assertNotEquals(first.get(), second.get());
        assertEquals(0, second.get().getActiveConnections());
        
        second.get().incrementConnections();
        
        // Third selection - should pick the remaining one with 0
        Optional<Target> third = algorithm.selectTarget(targetGroup, context);
        assertTrue(third.isPresent());
        assertNotEquals(first.get(), third.get());
        assertNotEquals(second.get(), third.get());
        assertEquals(0, third.get().getActiveConnections());
    }
    
    /**
     * Avoids selecting unhealthy targets even if they have fewer connections.
     */
    @Test
    void testOnlyHealthyTargets() {
        RequestContext context = RequestContext.builder().build();
        
        // Target with least connections is unhealthy
        targets.get(0).incrementConnections();
        targets.get(0).incrementConnections();
        targets.get(1).incrementConnections();
        targets.get(2).markUnhealthy(); // Has 0 connections but unhealthy
        
        Optional<Target> selected = algorithm.selectTarget(targetGroup, context);
        
        assertTrue(selected.isPresent());
        assertEquals(targets.get(1), selected.get()); // Second target has 1 connection
    }
    
    /**
     * Returns empty when there are no healthy servers.
     */
    @Test
    void testNoHealthyTargets() {
        RequestContext context = RequestContext.builder().build();
        
        targets.forEach(Target::markUnhealthy);
        
        Optional<Target> selected = algorithm.selectTarget(targetGroup, context);
        assertFalse(selected.isPresent());
    }
    
    /**
     * Leaves requests pinned to the single configured target.
     */
    @Test
    void testSingleTarget() {
        List<Target> singleTarget = Arrays.asList(new Target("http://localhost:8080", 1));
        TargetGroup singleGroup = new TargetGroup("/api", "LRT", singleTarget, null, null);
        
        RequestContext context = RequestContext.builder().build();
        
        singleTarget.get(0).incrementConnections();
        singleTarget.get(0).incrementConnections();
        
        Optional<Target> selected = algorithm.selectTarget(singleGroup, context);
        assertTrue(selected.isPresent());
        assertEquals(singleTarget.get(0), selected.get());
    }
    
    /**
     * Picks the target with the lowest relative load when counts are skewed.
     */
    @Test
    void testHighConnectionCount() {
        RequestContext context = RequestContext.builder().build();
        
        // Set very high connection counts
        for (int i = 0; i < 100; i++) {
            targets.get(0).incrementConnections();
        }
        for (int i = 0; i < 50; i++) {
            targets.get(1).incrementConnections();
        }
        for (int i = 0; i < 75; i++) {
            targets.get(2).incrementConnections();
        }
        
        Optional<Target> selected = algorithm.selectTarget(targetGroup, context);
        
        assertTrue(selected.isPresent());
        assertEquals(targets.get(1), selected.get());
        assertEquals(50, selected.get().getActiveConnections());
    }
}
