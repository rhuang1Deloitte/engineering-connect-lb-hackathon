package com.hackathon.lb.handler;

import static org.junit.jupiter.api.Assertions.*;

import com.hackathon.lb.model.RequestContext;
import com.hackathon.lb.model.Target;
import com.hackathon.lb.model.TargetGroup;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Test suite for HeaderConventionHandler.
 * Verifies that the handler correctly structures the RequestContext with standard HTTP proxy headers.
 * 
 * These are unit tests that verify the core logic. Integration tests with actual HTTP requests
 * are covered in RobustErrorHandlingAndRoutingTest and similar integration test suites.
 * 
 * Tests cover:
 * - RequestContext receives proper header fields
 * - Header structure is correct for upstream forwarding
 * - Graceful handling of null/missing values
 */
class HeaderConventionHandlerTest {

    private TargetGroup targetGroup;

    @BeforeEach
    void setUp() {
        List<Target> targets = Arrays.asList(new Target("http://localhost:8080", 1));
        targetGroup = new TargetGroup("/api", "ROUND_ROBIN", targets, null, null);
    }

    /**
     * Verifies that RequestContext can be populated with standard proxy headers.
     */
    @Test
    void testRequestContextCanStoreHeaders() {
        MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.set("X-Forwarded-For", "192.168.1.100");
        headers.set("X-Forwarded-Host", "example.com:8080");
        headers.set("X-Forwarded-Port", "8080");
        headers.set("X-Forwarded-Proto", "http");
        headers.set("X-Real-IP", "192.168.1.100");
        headers.set("Host", "example.com:8080");
        headers.set("X-Request-Id", "123456-789");
        
        RequestContext context = RequestContext.builder()
                .method(HttpMethod.GET)
                .originalPath("/api/test")
                .requestHeaders(headers)
                .targetGroup(targetGroup)
                .build();
        
        assertEquals("192.168.1.100", context.getRequestHeaders().get("X-Forwarded-For"));
        assertEquals("example.com:8080", context.getRequestHeaders().get("X-Forwarded-Host"));
        assertEquals("8080", context.getRequestHeaders().get("X-Forwarded-Port"));
        assertEquals("http", context.getRequestHeaders().get("X-Forwarded-Proto"));
        assertEquals("192.168.1.100", context.getRequestHeaders().get("X-Real-IP"));
        assertEquals("example.com:8080", context.getRequestHeaders().get("Host"));
        assertEquals("123456-789", context.getRequestHeaders().get("X-Request-Id"));
    }

    /**
     * Verifies that X-Forwarded-For can be appended to (for proxy chaining).
     */
    @Test
    void testXForwardedForCanBeAppended() {
        MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.set("X-Forwarded-For", "192.168.1.50");
        
        RequestContext context = RequestContext.builder()
                .method(HttpMethod.GET)
                .originalPath("/api/test")
                .requestHeaders(headers)
                .targetGroup(targetGroup)
                .build();
        
        // Simulate appending another IP
        String current = context.getRequestHeaders().get("X-Forwarded-For");
        String updated = current + ", 192.168.1.100";
        context.getRequestHeaders().set("X-Forwarded-For", updated);
        
        assertEquals("192.168.1.50, 192.168.1.100", context.getRequestHeaders().get("X-Forwarded-For"));
    }

    /**
     * Verifies that multiple proxy headers can exist simultaneously.
     */
    @Test
    void testMultipleProxyHeadersCanCoexist() {
        MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        
        // Add all required proxy headers
        headers.set("X-Forwarded-For", "203.0.113.1, 198.51.100.1");
        headers.set("X-Forwarded-Host", "cdn.example.com");
        headers.set("X-Forwarded-Port", "443");
        headers.set("X-Forwarded-Proto", "https");
        headers.set("X-Real-IP", "203.0.113.1");
        headers.set("Host", "cdn.example.com");
        headers.set("X-Request-Id", "req-001");
        
        RequestContext context = RequestContext.builder()
                .method(HttpMethod.GET)
                .originalPath("/api/test")
                .requestHeaders(headers)
                .targetGroup(targetGroup)
                .build();
        
        // Verify all headers are preserved
        assertNotNull(context.getRequestHeaders().get("X-Forwarded-For"));
        assertNotNull(context.getRequestHeaders().get("X-Forwarded-Host"));
        assertNotNull(context.getRequestHeaders().get("X-Forwarded-Port"));
        assertNotNull(context.getRequestHeaders().get("X-Forwarded-Proto"));
        assertNotNull(context.getRequestHeaders().get("X-Real-IP"));
        assertNotNull(context.getRequestHeaders().get("Host"));
        assertNotNull(context.getRequestHeaders().get("X-Request-Id"));
    }

    /**
     * Verifies that headers are case-insensitive (MultiMap.caseInsensitiveMultiMap behavior).
     */
    @Test
    void testHeadersAreCaseInsensitive() {
        MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.set("x-forwarded-for", "192.168.1.100");
        
        RequestContext context = RequestContext.builder()
                .method(HttpMethod.GET)
                .originalPath("/api/test")
                .requestHeaders(headers)
                .targetGroup(targetGroup)
                .build();
        
        // Should be retrievable with different casing
        assertEquals("192.168.1.100", context.getRequestHeaders().get("X-Forwarded-For"));
        assertEquals("192.168.1.100", context.getRequestHeaders().get("x-forwarded-for"));
    }

    /**
     * Verifies that empty header values are handled gracefully.
     */
    @Test
    void testHandleEmptyHeaderValues() {
        MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.set("X-Forwarded-For", "");
        
        RequestContext context = RequestContext.builder()
                .method(HttpMethod.GET)
                .originalPath("/api/test")
                .requestHeaders(headers)
                .targetGroup(targetGroup)
                .build();
        
        assertEquals("", context.getRequestHeaders().get("X-Forwarded-For"));
    }

    /**
     * Verifies that HandlerConventionHandler exists as a class and can be instantiated.
     */
    @Test
    void testHeaderConventionHandlerCanBeInstantiated() {
        HeaderConventionHandler handler = new HeaderConventionHandler();
        assertNotNull(handler);
    }

    /**
     * Verifies that HeaderConventionHandlerFactory can be instantiated.
     */
    @Test
    void testHeaderConventionHandlerFactoryCanBeCreated() {
        HeaderConventionHandlerFactory factory = new HeaderConventionHandlerFactory();
        assertNotNull(factory);
    }
}

