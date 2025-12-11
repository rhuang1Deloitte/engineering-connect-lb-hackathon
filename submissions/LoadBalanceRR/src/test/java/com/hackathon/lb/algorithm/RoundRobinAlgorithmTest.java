package com.hackathon.lb.algorithm;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.hackathon.lb.model.RequestContext;
import com.hackathon.lb.model.Target;
import com.hackathon.lb.model.TargetGroup;

/**
 * Ensures the round robin strategy visits healthy targets in a predictable cycle.
 */
class RoundRobinAlgorithmTest {
    
    private RoundRobinAlgorithm algorithm;
    private TargetGroup targetGroup;
    private List<Target> targets;
    
    @BeforeEach
    void setUp() {
        algorithm = new RoundRobinAlgorithm();
        
        targets = Arrays.asList(
            new Target("http://localhost:8080", 1),
            new Target("http://localhost:8081", 1),
            new Target("http://localhost:8082", 1)
        );
        
        targetGroup = new TargetGroup("/api", "ROUND_ROBIN", targets, null, null);
    }
    
    /**
     * Verifies the algorithm exposes the expected constant name.
     */
    @Test
    void testGetName() {
        assertEquals("ROUND_ROBIN", algorithm.getName());
    }
    
    /**
     * Confirms the counter cycles through each target before repeating.
     */
    @Test
    void testRoundRobinDistribution() {
        RequestContext context = RequestContext.builder().build();
        
        // Should cycle through targets in order
        Optional<Target> target1 = algorithm.selectTarget(targetGroup, context);
        Optional<Target> target2 = algorithm.selectTarget(targetGroup, context);
        Optional<Target> target3 = algorithm.selectTarget(targetGroup, context);
        Optional<Target> target4 = algorithm.selectTarget(targetGroup, context);
        
        assertTrue(target1.isPresent());
        assertTrue(target2.isPresent());
        assertTrue(target3.isPresent());
        assertTrue(target4.isPresent());
        
        // Should have visited all targets
        Set<Target> visited = new HashSet<>(Arrays.asList(
            target1.get(), target2.get(), target3.get()
        ));
        assertEquals(3, visited.size());
        
        // Fourth should be the same as first (cycling)
        assertEquals(target1.get(), target4.get());
    }
    
    /**
     * Checks the selections are evenly distributed when executed many times.
     */
    @Test
    void testEvenDistribution() {
        RequestContext context = RequestContext.builder().build();
        
        int[] counts = new int[3];
        int iterations = 300;
        
        for (int i = 0; i < iterations; i++) {
            Optional<Target> selected = algorithm.selectTarget(targetGroup, context);
            assertTrue(selected.isPresent());
            
            int index = targets.indexOf(selected.get());
            counts[index]++;
        }
        
        // Each target should get exactly 100 requests (300/3)
        assertEquals(100, counts[0]);
        assertEquals(100, counts[1]);
        assertEquals(100, counts[2]);
    }
    
    /**
     * Asserts that unhealthy targets are skipped during selection.
     */
    @Test
    void testOnlyHealthyTargets() {
        RequestContext context = RequestContext.builder().build();
        
        // Mark one target as unhealthy
        targets.get(1).markUnhealthy();
        
        Set<Target> selected = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            Optional<Target> target = algorithm.selectTarget(targetGroup, context);
            assertTrue(target.isPresent());
            selected.add(target.get());
        }
        
        // Should only select from healthy targets
        assertEquals(2, selected.size());
        assertFalse(selected.contains(targets.get(1)));
    }
    
    /**
     * Verifies no target is returned when all targets are unhealthy.
     */
    @Test
    void testNoHealthyTargets() {
        RequestContext context = RequestContext.builder().build();
        
        // Mark all targets as unhealthy
        targets.forEach(Target::markUnhealthy);
        
        Optional<Target> selected = algorithm.selectTarget(targetGroup, context);
        assertFalse(selected.isPresent());
    }
    
    /**
     * Ensures a single target group always returns the same backend.
     */
    @Test
    void testSingleTarget() {
        List<Target> singleTarget = Arrays.asList(new Target("http://localhost:8080", 1));
        TargetGroup singleGroup = new TargetGroup("/api", "ROUND_ROBIN", singleTarget, null, null);
        
        RequestContext context = RequestContext.builder().build();
        
        // Should always return the same target
        for (int i = 0; i < 5; i++) {
            Optional<Target> selected = algorithm.selectTarget(singleGroup, context);
            assertTrue(selected.isPresent());
            assertEquals(singleTarget.get(0), selected.get());
        }
    }
}
