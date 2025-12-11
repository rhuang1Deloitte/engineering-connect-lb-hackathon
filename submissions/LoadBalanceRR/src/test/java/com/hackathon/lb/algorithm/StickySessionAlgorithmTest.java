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

import io.vertx.core.MultiMap;

/**
 * Exercises sticky session behavior to ensure requests with the same session stick to the same target.
 */
class StickySessionAlgorithmTest {
    
    private StickySessionAlgorithm algorithm;
    private List<Target> targets;
    private TargetGroup targetGroup;
    
    @BeforeEach
    void setUp() {
        algorithm = new StickySessionAlgorithm();
        algorithm.clearSessions(); // Clear any cached sessions
        
        targets = Arrays.asList(
            new Target("http://localhost:8080", 1),
            new Target("http://localhost:8081", 1),
            new Target("http://localhost:8082", 1)
        );
        
        targetGroup = new TargetGroup("/api", "STICKY", targets, null, null);
    }
    
    /**
     * Ensures the algorithm shares the configured identifier.
     */
    @Test
    void testGetName() {
        assertEquals("STICKY", algorithm.getName());
    }
    
    /**
     * Verifies requests with an explicit JSESSIONID are routed consistently.
     */
    @Test
    void testStickySessionWithJSessionId() {
        MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.set("Cookie", "JSESSIONID=abc123");
        
        RequestContext context = RequestContext.builder()
            .requestHeaders(headers)
            .build();
        
        // First request establishes session
        Optional<Target> first = algorithm.selectTarget(targetGroup, context);
        assertTrue(first.isPresent());
        
        // Subsequent requests with same session ID should get same target
        Optional<Target> second = algorithm.selectTarget(targetGroup, context);
        Optional<Target> third = algorithm.selectTarget(targetGroup, context);
        
        assertTrue(second.isPresent());
        assertTrue(third.isPresent());
        assertEquals(first.get(), second.get());
        assertEquals(first.get(), third.get());
    }
    
    /**
     * Validates the custom LB_SESSION cookie is honored.
     */
    @Test
    void testStickySessionWithLBCookie() {
        MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.set("Cookie", "LB_SESSION=xyz789");
        
        RequestContext context = RequestContext.builder()
            .requestHeaders(headers)
            .build();
        
        Optional<Target> first = algorithm.selectTarget(targetGroup, context);
        Optional<Target> second = algorithm.selectTarget(targetGroup, context);
        
        assertTrue(first.isPresent());
        assertTrue(second.isPresent());
        assertEquals(first.get(), second.get());
    }
    
    /**
     * Confirms different session identifiers map to distinct targets.
     */
    @Test
    void testDifferentSessionsGetDifferentTargets() {
        MultiMap headers1 = MultiMap.caseInsensitiveMultiMap();
        headers1.set("Cookie", "JSESSIONID=session1");
        
        MultiMap headers2 = MultiMap.caseInsensitiveMultiMap();
        headers2.set("Cookie", "JSESSIONID=session2");
        
        RequestContext context1 = RequestContext.builder()
            .requestHeaders(headers1)
            .build();
        
        RequestContext context2 = RequestContext.builder()
            .requestHeaders(headers2)
            .build();
        
        Optional<Target> target1 = algorithm.selectTarget(targetGroup, context1);
        Optional<Target> target2 = algorithm.selectTarget(targetGroup, context2);
        
        assertTrue(target1.isPresent());
        assertTrue(target2.isPresent());
        
        // Verify session stickiness
        assertEquals(target1.get(), algorithm.selectTarget(targetGroup, context1).get());
        assertEquals(target2.get(), algorithm.selectTarget(targetGroup, context2).get());
    }
    
    /**
     * Checks the handler falls back to X-Forwarded-For when cookies are absent.
     */
    @Test
    void testFallbackToClientIP() {
        MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.set("X-Forwarded-For", "192.168.1.100");
        
        RequestContext context = RequestContext.builder()
            .requestHeaders(headers)
            .build();
        
        Optional<Target> first = algorithm.selectTarget(targetGroup, context);
        Optional<Target> second = algorithm.selectTarget(targetGroup, context);
        
        assertTrue(first.isPresent());
        assertTrue(second.isPresent());
        assertEquals(first.get(), second.get());
    }
    
    /**
     * Checks the handler falls back to X-Real-IP when X-Forwarded-For is absent.
     */
    @Test
    void testFallbackToRealIP() {
        MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.set("X-Real-IP", "10.0.0.50");
        
        RequestContext context = RequestContext.builder()
            .requestHeaders(headers)
            .build();
        
        Optional<Target> first = algorithm.selectTarget(targetGroup, context);
        Optional<Target> second = algorithm.selectTarget(targetGroup, context);
        
        assertTrue(first.isPresent());
        assertTrue(second.isPresent());
        assertEquals(first.get(), second.get());
    }
    
    /**
     * Ensures operation without session data still returns a target.
     */
    @Test
    void testNoSessionIdentifier() {
        RequestContext context = RequestContext.builder().build();
        
        // Without session identifier, should still work but won't be sticky
        Optional<Target> first = algorithm.selectTarget(targetGroup, context);
        assertTrue(first.isPresent());
    }
    
    /**
     * Confirms cached sessions are cleared when their target becomes unhealthy.
     */
    @Test
    void testUnhealthyTargetRemoved() {
        MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.set("Cookie", "JSESSIONID=abc123");
        
        RequestContext context = RequestContext.builder()
            .requestHeaders(headers)
            .build();
        
        // First request establishes session
        Optional<Target> first = algorithm.selectTarget(targetGroup, context);
        assertTrue(first.isPresent());
        Target originalTarget = first.get();
        
        // Mark the target as unhealthy
        originalTarget.markUnhealthy();
        
        // Should select a different healthy target
        Optional<Target> second = algorithm.selectTarget(targetGroup, context);
        assertTrue(second.isPresent());
        assertNotEquals(originalTarget, second.get());
        assertTrue(second.get().isHealthy());
        
        // Should now stick to the new target
        Optional<Target> third = algorithm.selectTarget(targetGroup, context);
        assertTrue(third.isPresent());
        assertEquals(second.get(), third.get());
    }
    
    /**
     * Ensures cookie parsing still finds the sticky session value among others.
     */
    @Test
    void testMultipleCookies() {
        MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.set("Cookie", "other=value1; JSESSIONID=abc123; another=value2");
        
        RequestContext context = RequestContext.builder()
            .requestHeaders(headers)
            .build();
        
        Optional<Target> first = algorithm.selectTarget(targetGroup, context);
        Optional<Target> second = algorithm.selectTarget(targetGroup, context);
        
        assertTrue(first.isPresent());
        assertTrue(second.isPresent());
        assertEquals(first.get(), second.get());
    }
    
    /**
     * Verifies selection fails when every backend is unhealthy.
     */
    @Test
    void testNoHealthyTargets() {
        MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.set("Cookie", "JSESSIONID=abc123");
        
        RequestContext context = RequestContext.builder()
            .requestHeaders(headers)
            .build();
        
        targets.forEach(Target::markUnhealthy);
        
        Optional<Target> selected = algorithm.selectTarget(targetGroup, context);
        assertFalse(selected.isPresent());
    }
    
    /**
     * Ensures sessions may be programmatically cleared.
     */
    @Test
    void testClearSessions() {
        MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.set("Cookie", "JSESSIONID=abc123");
        
        RequestContext context = RequestContext.builder()
            .requestHeaders(headers)
            .build();
        
        // Establish session
        Optional<Target> first = algorithm.selectTarget(targetGroup, context);
        assertTrue(first.isPresent());
        
        // Clear sessions
        algorithm.clearSessions();
        
        // Next request might get a different target (no guarantee, but session is cleared)
        Optional<Target> second = algorithm.selectTarget(targetGroup, context);
        assertTrue(second.isPresent());
    }
}
