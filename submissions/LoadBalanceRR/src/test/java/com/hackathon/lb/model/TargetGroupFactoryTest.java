package com.hackathon.lb.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.hackathon.lb.config.HealthCheckConfig;
import com.hackathon.lb.config.TargetConfig;
import com.hackathon.lb.config.TargetGroupConfig;

/**
 * Tests creation of TargetGroup objects from configuration mappings.
 */
class TargetGroupFactoryTest {
    
    /**
     * Verifies basic config with defaults maps to a target group.
     */
    @Test
    void testFromConfigBasic() {
        TargetGroupConfig config = new MockTargetGroupConfig(
            "/api",
            Optional.empty(),
            List.of(
                new MockTargetConfig("http://localhost:8080", Optional.empty()),
                new MockTargetConfig("http://localhost:8081", Optional.of(2))
            ),
            Optional.empty()
        );
        
        TargetGroup group = TargetGroupFactory.fromConfig(config, "ROUND_ROBIN");
        
        assertEquals("/api", group.getPath());
        assertEquals("ROUND_ROBIN", group.getAlgorithm());
        assertEquals(2, group.getTargets().size());
        assertEquals(1, group.getTargets().get(0).getWeight());
        assertEquals(2, group.getTargets().get(1).getWeight());
        assertFalse(group.getHealthCheck().isPresent());
    }
    
    /**
     * Ensures overriding the algorithm in config takes precedence.
     */
    @Test
    void testFromConfigWithAlgorithmOverride() {
        TargetGroupConfig config = new MockTargetGroupConfig(
            "/api",
            Optional.of("WEIGHTED"),
            List.of(new MockTargetConfig("http://localhost:8080", Optional.empty())),
            Optional.empty()
        );
        
        TargetGroup group = TargetGroupFactory.fromConfig(config, "ROUND_ROBIN");
        
        assertEquals("WEIGHTED", group.getAlgorithm());
    }
    
    /**
     * Ensures health check configuration is translated into the model.
     */
    @Test
    void testFromConfigWithHealthCheck() {
        MockHealthCheckConfig healthCheckConfig = new MockHealthCheckConfig(
            true, "/health", 5000, 2, 3
        );
        
        TargetGroupConfig config = new MockTargetGroupConfig(
            "/api",
            Optional.empty(),
            List.of(new MockTargetConfig("http://localhost:8080", Optional.empty())),
            Optional.of(healthCheckConfig)
        );
        
        TargetGroup group = TargetGroupFactory.fromConfig(config, "ROUND_ROBIN");
        
        assertTrue(group.getHealthCheck().isPresent());
        TargetGroup.HealthCheck healthCheck = group.getHealthCheck().get();
        assertTrue(healthCheck.isEnabled());
        assertEquals("/health", healthCheck.getPath());
        assertEquals(5000, healthCheck.getInterval());
        assertEquals(2, healthCheck.getSuccessThreshold());
        assertEquals(3, healthCheck.getFailureThreshold());
    }
    
    /**
     * Throws when the config object is null.
     */
    @Test
    void testFromConfigNullConfigThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            TargetGroupFactory.fromConfig(null, "ROUND_ROBIN");
        });
    }
    
    /**
     * Throws when no targets are present in the config.
     */
    @Test
    void testFromConfigEmptyTargetsThrows() {
        TargetGroupConfig config = new MockTargetGroupConfig(
            "/api",
            Optional.empty(),
            List.of(),
            Optional.empty()
        );
        
        assertThrows(IllegalArgumentException.class, () -> {
            TargetGroupFactory.fromConfig(config, "ROUND_ROBIN");
        });
    }
    
    // Mock implementations for testing
    
    private static class MockTargetGroupConfig implements TargetGroupConfig {
        private final String path;
        private final Optional<String> algorithm;
        private final List<TargetConfig> targets;
        private final Optional<HealthCheckConfig> healthCheck;
        private final Optional<String> pathRewrite;
        
        public MockTargetGroupConfig(String path, Optional<String> algorithm, 
                                    List<TargetConfig> targets, Optional<HealthCheckConfig> healthCheck) {
            this.path = path;
            this.algorithm = algorithm;
            this.targets = targets;
            this.healthCheck = healthCheck;
            this.pathRewrite = Optional.empty();
        }
        
        @Override
        public String path() {
            return path;
        }
        
        @Override
        public Optional<String> algorithm() {
            return algorithm;
        }
        
        @Override
        public List<TargetConfig> targets() {
            return targets;
        }
        
        @Override
        public Optional<HealthCheckConfig> healthCheck() {
            return healthCheck;
        }
        
        @Override
        public Optional<String> pathRewrite() {
            return pathRewrite;
        }
    }
    
    private static class MockTargetConfig implements TargetConfig {
        private final String url;
        private final Optional<Integer> weight;
        
        public MockTargetConfig(String url, Optional<Integer> weight) {
            this.url = url;
            this.weight = weight;
        }
        
        @Override
        public String url() {
            return url;
        }
        
        @Override
        public Optional<Integer> weight() {
            return weight;
        }
    }
    
    private static class MockHealthCheckConfig implements HealthCheckConfig {
        private final boolean enabled;
        private final String path;
        private final int interval;
        private final int successThreshold;
        private final int failureThreshold;
        
        public MockHealthCheckConfig(boolean enabled, String path, int interval, 
                                    int successThreshold, int failureThreshold) {
            this.enabled = enabled;
            this.path = path;
            this.interval = interval;
            this.successThreshold = successThreshold;
            this.failureThreshold = failureThreshold;
        }
        
        @Override
        public boolean enabled() {
            return enabled;
        }
        
        @Override
        public String path() {
            return path;
        }
        
        @Override
        public int interval() {
            return interval;
        }
        
        @Override
        public int successThreshold() {
            return successThreshold;
        }
        
        @Override
        public int failureThreshold() {
            return failureThreshold;
        }
    }
}
