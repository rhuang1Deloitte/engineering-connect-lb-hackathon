package com.hackathon.lb.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

/**
 * Verifies that LoadBalancerConfig is correctly mapped from configuration
 * using SmallRyeConfig directly, without starting Quarkus.
 */
class LoadBalancerConfigEnvMappingTest {

    /**
     * Defaults should match the @WithDefault annotations on LoadBalancerConfig.
     */
    @Test
    void defaultValuesMatchAnnotations() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(LoadBalancerConfig.class)
                // no explicit values, so defaults on the interface apply
                .build();

        LoadBalancerConfig lbConfig = config.getConfigMapping(LoadBalancerConfig.class);

        assertEquals("ROUND_ROBIN", lbConfig.algorithm(),
                "Default algorithm should be ROUND_ROBIN");
        assertEquals(2000L, lbConfig.connectionTimeoutMillis(),
                "Default timeout should be 2000 ms");
    }

    /**
     * Explicit config values should override the defaults. This simulates what
     * would happen if environment variables (e.g. LOAD_BALANCING_ALGORITHM,
     * CONNECTION_TIMEOUT) are mapped to lbConfig.* properties.
     */
    @Test
    void explicitConfigOverridesDefaults() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(LoadBalancerConfig.class)
                .withDefaultValue("lbConfig.algorithm", "WEIGHTED")
                .withDefaultValue("lbConfig.connectionTimeoutMillis", "5000")
                .build();

        LoadBalancerConfig lbConfig = config.getConfigMapping(LoadBalancerConfig.class);

        assertEquals("WEIGHTED", lbConfig.algorithm(),
                "Algorithm should be overridden to WEIGHTED");
        assertEquals(5000L, lbConfig.connectionTimeoutMillis(),
                "Timeout should be overridden to 5000 ms");
    }
}
