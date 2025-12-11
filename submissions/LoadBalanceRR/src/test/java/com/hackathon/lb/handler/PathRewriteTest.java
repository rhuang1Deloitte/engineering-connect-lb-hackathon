package com.hackathon.lb.handler;

import com.hackathon.lb.model.Target;
import com.hackathon.lb.model.TargetGroup;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for path rewriting functionality in TargetGroup.
 * Verifies that the pathRewrite configuration is properly stored and retrieved.
 */
class PathRewriteTest {

    /**
     * Verifies that pathRewrite is properly stored in TargetGroup when configured.
     */
    @Test
    void testPathRewriteConfiguration() {
        List<Target> targets = Arrays.asList(new Target("http://localhost:8080", 1));
        TargetGroup targetGroup = new TargetGroup("/api", "ROUND_ROBIN", targets, null, "/api");
        
        assertEquals("/api", targetGroup.getPathRewrite());
    }

    /**
     * Verifies that pathRewrite can be null.
     */
    @Test
    void testPathRewriteNull() {
        List<Target> targets = Arrays.asList(new Target("http://localhost:8080", 1));
        TargetGroup targetGroup = new TargetGroup("/api", "ROUND_ROBIN", targets, null, null);
        
        assertNull(targetGroup.getPathRewrite());
    }

    /**
     * Verifies that pathRewrite can be an empty string.
     */
    @Test
    void testPathRewriteEmpty() {
        List<Target> targets = Arrays.asList(new Target("http://localhost:8080", 1));
        TargetGroup targetGroup = new TargetGroup("/api", "ROUND_ROBIN", targets, null, "");
        
        assertEquals("", targetGroup.getPathRewrite());
    }

    /**
     * Verifies that pathRewrite can be a complex nested path.
     */
    @Test
    void testPathRewriteNestedPath() {
        List<Target> targets = Arrays.asList(new Target("http://localhost:8080", 1));
        TargetGroup targetGroup = new TargetGroup("/services/api/v1", "ROUND_ROBIN", targets, null, "/services/api");
        
        assertEquals("/services/api", targetGroup.getPathRewrite());
    }

    /**
     * Verifies that TargetGroup maintains all properties including pathRewrite.
     */
    @Test
    void testTargetGroupWithAllProperties() {
        List<Target> targets = Arrays.asList(
            new Target("http://localhost:8080", 1),
            new Target("http://localhost:8081", 2)
        );
        
        TargetGroup.HealthCheck healthCheck = new TargetGroup.HealthCheck(
            true, "/health", 5000, 2, 3
        );
        
        TargetGroup targetGroup = new TargetGroup(
            "/api/v1", 
            "WEIGHTED", 
            targets, 
            healthCheck, 
            "/api"
        );
        
        assertEquals("/api/v1", targetGroup.getPath());
        assertEquals("WEIGHTED", targetGroup.getAlgorithm());
        assertEquals(2, targetGroup.getTargets().size());
        assertTrue(targetGroup.getHealthCheck().isPresent());
        assertEquals("/api", targetGroup.getPathRewrite());
    }
}
