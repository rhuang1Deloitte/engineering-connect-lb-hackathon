package com.hackathon.lb.algorithm;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
/**
 * Checks the registry returns the expected implementations for each algorithm type.
 */
class LoadBalancingAlgorithmRegistryTest {
    
    @Inject
    LoadBalancingAlgorithmRegistry registry;
    
    /**
     * Verifies the registry is injected into the Quarkus test context.
     */
    @Test
    void testRegistryInjected() {
        assertNotNull(registry);
    }
    
    /**
     * Ensures the round robin implementation is resolvable by name.
     */
    @Test
    void testGetRoundRobinAlgorithm() {
        LoadBalancingAlgorithm algorithm = registry.getAlgorithm("ROUND_ROBIN");
        assertNotNull(algorithm);
        assertEquals("ROUND_ROBIN", algorithm.getName());
        assertInstanceOf(RoundRobinAlgorithm.class, algorithm);
    }
    
    /**
     * Ensures the weighted implementation is resolvable by name.
     */
    @Test
    void testGetWeightedAlgorithm() {
        LoadBalancingAlgorithm algorithm = registry.getAlgorithm("WEIGHTED");
        assertNotNull(algorithm);
        assertEquals("WEIGHTED", algorithm.getName());
        assertInstanceOf(WeightedAlgorithm.class, algorithm);
    }
    
    /**
     * Ensures the least-connections implementation is resolvable by name.
     */
    @Test
    void testGetLeastConnectionsAlgorithm() {
        LoadBalancingAlgorithm algorithm = registry.getAlgorithm("LRT");
        assertNotNull(algorithm);
        assertEquals("LRT", algorithm.getName());
        assertInstanceOf(LeastConnectionsAlgorithm.class, algorithm);
    }
    
    /**
     * Ensures the sticky session implementation is resolvable by name.
     */
    @Test
    void testGetStickySessionAlgorithm() {
        LoadBalancingAlgorithm algorithm = registry.getAlgorithm("STICKY");
        assertNotNull(algorithm);
        assertEquals("STICKY", algorithm.getName());
        assertInstanceOf(StickySessionAlgorithm.class, algorithm);
    }
    
    /**
     * Verifies unknown names return null when no default is provided.
     */
    @Test
    void testGetNonExistentAlgorithm() {
        LoadBalancingAlgorithm algorithm = registry.getAlgorithm("NON_EXISTENT");
        assertNull(algorithm);
    }
    
    /**
     * Ensures a fallback algorithm is returned when the requested one is missing.
     */
    @Test
    void testGetAlgorithmOrDefault() {
        LoadBalancingAlgorithm algorithm = registry.getAlgorithmOrDefault("NON_EXISTENT", "ROUND_ROBIN");
        assertNotNull(algorithm);
        assertEquals("ROUND_ROBIN", algorithm.getName());
    }
    
    /**
     * Ensures the requested algorithm is returned when it exists.
     */
    @Test
    void testGetAlgorithmOrDefaultWithValidName() {
        LoadBalancingAlgorithm algorithm = registry.getAlgorithmOrDefault("WEIGHTED", "ROUND_ROBIN");
        assertNotNull(algorithm);
        assertEquals("WEIGHTED", algorithm.getName());
    }
    
    /**
     * Confirms the registry reports which algorithms are registered.
     */
    @Test
    void testHasAlgorithm() {
        assertTrue(registry.hasAlgorithm("ROUND_ROBIN"));
        assertTrue(registry.hasAlgorithm("WEIGHTED"));
        assertTrue(registry.hasAlgorithm("LRT"));
        assertTrue(registry.hasAlgorithm("STICKY"));
        assertFalse(registry.hasAlgorithm("NON_EXISTENT"));
    }
    
    /**
     * Ensures Arc scopes algorithms as singletons so repeated lookups match.
     */
    @Test
    void testAllAlgorithmsAreSingleton() {
        // Getting the same algorithm twice should return the same instance (Arc @ApplicationScoped)
        LoadBalancingAlgorithm first = registry.getAlgorithm("ROUND_ROBIN");
        LoadBalancingAlgorithm second = registry.getAlgorithm("ROUND_ROBIN");
        assertSame(first, second);
    }
}
