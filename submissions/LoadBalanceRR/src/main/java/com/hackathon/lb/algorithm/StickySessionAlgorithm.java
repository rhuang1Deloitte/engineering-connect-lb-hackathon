package com.hackathon.lb.algorithm;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.hackathon.lb.model.RequestContext;
import com.hackathon.lb.model.Target;
import com.hackathon.lb.model.TargetGroup;

import io.vertx.core.MultiMap;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Sticky session load balancing algorithm.
 * Routes requests from the same client to the same target server based on session cookies or client IP.
 * Falls back to round-robin for new sessions.
 */
@ApplicationScoped
public class StickySessionAlgorithm implements LoadBalancingAlgorithm {
    
    private static final String NAME = "STICKY";
    private static final String SESSION_COOKIE = "JSESSIONID";
    private static final String LB_COOKIE = "LB_SESSION";
    
    private final Map<String, Target> sessionMap = new ConcurrentHashMap<>();
    private final RoundRobinAlgorithm fallbackAlgorithm = new RoundRobinAlgorithm();
    
    /**
     * Attempts to reuse a previously selected target for the session or delegates to round-robin.
     */
    @Override
    public Optional<Target> selectTarget(TargetGroup targetGroup, RequestContext requestContext) {
        List<Target> healthyTargets = targetGroup.getHealthyTargets();
        
        if (healthyTargets.isEmpty()) {
            return Optional.empty();
        }
        
        // Extract session identifier
        String sessionId = extractSessionId(requestContext);
        
        if (sessionId != null) {
            // Check if we have a cached target for this session
            Target cachedTarget = sessionMap.get(sessionId);
            
            // Verify the cached target is still healthy
            if (cachedTarget != null && cachedTarget.isHealthy() && 
                healthyTargets.contains(cachedTarget)) {
                return Optional.of(cachedTarget);
            }
            
            // Clean up stale mapping if target is no longer healthy
            if (cachedTarget != null) {
                sessionMap.remove(sessionId);
            }
        }
        
        // No cached target or new session - use fallback algorithm
        Optional<Target> selected = fallbackAlgorithm.selectTarget(targetGroup, requestContext);
        
        // Cache the selection if we have a session ID
        if (sessionId != null && selected.isPresent()) {
            sessionMap.put(sessionId, selected.get());
        }
        
        return selected;
    }
    
    /**
     * Extracts session identifier from request headers.
     * Looks for common session cookies or falls back to client IP.
     */
    private String extractSessionId(RequestContext requestContext) {
        MultiMap headers = requestContext.getRequestHeaders();
        
        if (headers == null) {
            return null;
        }
        
        // Try to extract from Cookie header
        String cookieHeader = headers.get("Cookie");
        if (cookieHeader != null) {
            // Look for JSESSIONID
            String sessionId = extractCookieValue(cookieHeader, SESSION_COOKIE);
            if (sessionId != null) {
                return sessionId;
            }
            
            // Look for custom LB session cookie
            sessionId = extractCookieValue(cookieHeader, LB_COOKIE);
            if (sessionId != null) {
                return sessionId;
            }
        }
        
        // Fallback to client IP if available
        String clientIp = headers.get("X-Forwarded-For");
        if (clientIp == null) {
            clientIp = headers.get("X-Real-IP");
        }
        
        return clientIp;
    }
    
    /**
     * Extracts a specific cookie value from a Cookie header string.
     */
    private String extractCookieValue(String cookieHeader, String cookieName) {
        String[] cookies = cookieHeader.split(";");
        for (String cookie : cookies) {
            String trimmed = cookie.trim();
            if (trimmed.startsWith(cookieName + "=")) {
                return trimmed.substring(cookieName.length() + 1);
            }
        }
        return null;
    }
    
    /**
     * Clears all cached session mappings.
     * Useful for testing or manual session reset.
     */
    public void clearSessions() {
        sessionMap.clear();
    }
    
    /**
     * Provides the name used when registering the sticky session algorithm.
     */
    @Override
    public String getName() {
        return NAME;
    }
}
