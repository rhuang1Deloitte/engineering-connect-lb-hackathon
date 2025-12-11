package com.hackathon.lb.algorithm;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.hackathon.lb.model.RequestContext;
import com.hackathon.lb.model.Target;
import com.hackathon.lb.model.TargetGroup;

/**
 * Validates that the weighted strategy favors higher weight targets while respecting health state.
 */
class WeightedAlgorithmTest {
    
    private WeightedAlgorithm algorithm;
    
    @BeforeEach
    void setUp() {
        algorithm = new WeightedAlgorithm();
    }
    
    /**
     * Ensures the algorithm reports its canonical name.
     */
    @Test
    void testGetName() {
        assertEquals("WEIGHTED", algorithm.getName());
    }
    
    /**
     * Confirms distribution roughly matches configured weights over many iterations.
     */
    @Test
    void testWeightedDistribution() {
        // Create targets with weights 1, 2, 3 (total 6)
        List<Target> targets = Arrays.asList(
            new Target("http://localhost:8080", 1),
            new Target("http://localhost:8081", 2),
            new Target("http://localhost:8082", 3)
        );
        
        TargetGroup targetGroup = new TargetGroup("/api", "WEIGHTED", targets, null, null);
        RequestContext context = RequestContext.builder().build();
        
        Map<Target, Integer> counts = new HashMap<>();
        int iterations = 6000;
        
        for (int i = 0; i < iterations; i++) {
            Optional<Target> selected = algorithm.selectTarget(targetGroup, context);
            assertTrue(selected.isPresent());
            counts.merge(selected.get(), 1, Integer::sum);
        }
        
        // Check distribution roughly matches weights (1:2:3)
        int count1 = counts.getOrDefault(targets.get(0), 0);
        int count2 = counts.getOrDefault(targets.get(1), 0);
        int count3 = counts.getOrDefault(targets.get(2), 0);
        
        // Allow 5% tolerance for randomness
        assertTrue(Math.abs(count1 - 1000) < 100, "Expected ~1000, got " + count1);
        assertTrue(Math.abs(count2 - 2000) < 150, "Expected ~2000, got " + count2);
        assertTrue(Math.abs(count3 - 3000) < 200, "Expected ~3000, got " + count3);
    }
    
    /**
     * Ensures equal weights produce nearly uniform traffic.
     */
    @Test
    void testEqualWeights() {
        List<Target> targets = Arrays.asList(
            new Target("http://localhost:8080", 1),
            new Target("http://localhost:8081", 1),
            new Target("http://localhost:8082", 1)
        );
        
        TargetGroup targetGroup = new TargetGroup("/api", "WEIGHTED", targets, null, null);
        RequestContext context = RequestContext.builder().build();
        
        Map<Target, Integer> counts = new HashMap<>();
        int iterations = 3000;
        
        for (int i = 0; i < iterations; i++) {
            Optional<Target> selected = algorithm.selectTarget(targetGroup, context);
            assertTrue(selected.isPresent());
            counts.merge(selected.get(), 1, Integer::sum);
        }
        
        // Should distribute roughly evenly (33% each)
        counts.values().forEach(count -> {
            assertTrue(Math.abs(count - 1000) < 100, "Expected ~1000, got " + count);
        });
    }
    
    /**
     * Verifies unhealthy targets are excluded even if they have higher weights.
     */
    @Test
    void testOnlyHealthyTargets() {
        List<Target> targets = Arrays.asList(
            new Target("http://localhost:8080", 1),
            new Target("http://localhost:8081", 2),
            new Target("http://localhost:8082", 3)
        );
        
        TargetGroup targetGroup = new TargetGroup("/api", "WEIGHTED", targets, null, null);
        RequestContext context = RequestContext.builder().build();
        
        // Mark highest weight target as unhealthy
        targets.get(2).markUnhealthy();
        
        Map<Target, Integer> counts = new HashMap<>();
        for (int i = 0; i < 300; i++) {
            Optional<Target> selected = algorithm.selectTarget(targetGroup, context);
            assertTrue(selected.isPresent());
            counts.merge(selected.get(), 1, Integer::sum);
        }
        
        // Should not select unhealthy target
        assertEquals(0, counts.getOrDefault(targets.get(2), 0));
        assertTrue(counts.containsKey(targets.get(0)));
        assertTrue(counts.containsKey(targets.get(1)));
    }
    
    /**
     * Verifies behavior when no healthy backends remain.
     */
    @Test
    void testNoHealthyTargets() {
        List<Target> targets = Arrays.asList(
            new Target("http://localhost:8080", 1),
            new Target("http://localhost:8081", 2)
        );
        
        TargetGroup targetGroup = new TargetGroup("/api", "WEIGHTED", targets, null, null);
        RequestContext context = RequestContext.builder().build();
        
        // Mark all as unhealthy
        targets.forEach(Target::markUnhealthy);
        
        Optional<Target> selected = algorithm.selectTarget(targetGroup, context);
        assertFalse(selected.isPresent());
    }
    
    /**
     * Ensures a single target always receives the request chain.
     */
    @Test
    void testSingleTarget() {
        List<Target> targets = Arrays.asList(
            new Target("http://localhost:8080", 5)
        );
        
        TargetGroup targetGroup = new TargetGroup("/api", "WEIGHTED", targets, null, null);
        RequestContext context = RequestContext.builder().build();
        
        // Should always return the single target
        for (int i = 0; i < 10; i++) {
            Optional<Target> selected = algorithm.selectTarget(targetGroup, context);
            assertTrue(selected.isPresent());
            assertEquals(targets.get(0), selected.get());
        }
    }
}
