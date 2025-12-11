package com.hackathon.lb.handler;

import com.hackathon.lb.model.RequestContext;

import io.quarkus.logging.Log;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler that adds standard HTTP header conventions to upstream requests.
 * 
 * This handler implements the following headers as specified by Oracle Load Balancer documentation:
 * - X-Forwarded-For: List of connection IP addresses (client IP)
 * - X-Forwarded-Host: Original host and port requested by the client
 * - X-Forwarded-Port: Listener port number that the client used
 * - X-Forwarded-Proto: Protocol used by the client (HTTP or HTTPS)
 * - X-Real-IP: Client's IP address
 * - Host: Original host and port requested by the client
 * 
 * This handler should be placed in the request chain BEFORE the UpstreamClientHandler
 * to ensure headers are added to the request before it's forwarded upstream.
 */
public class HeaderConventionHandler implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext ctx) {
        HttpServerRequest request = ctx.request();
        RequestContext requestContext = ContextInitialisationHandler.getContext(ctx);
        
        if (requestContext == null) {
            Log.debug("RequestContext not found in routing context");
            ctx.next();
            return;
        }
        
        // Extract client information from the incoming request
        String clientIP = extractClientIP(request);
        String originalHost = request.getHeader("Host");
        String protocol = request.isSSL() ? "https" : "http";
        int port = request.localAddress().port();
        
        Log.debugf("Adding header conventions: clientIP=%s, host=%s, proto=%s, port=%d",
                   clientIP, originalHost, protocol, port);
        
        // Add X-Forwarded-For header (client IP, or append if already exists)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            xForwardedFor = xForwardedFor + ", " + clientIP;
        } else {
            xForwardedFor = clientIP;
        }
        requestContext.getRequestHeaders().set("X-Forwarded-For", xForwardedFor);
        
        // Add X-Forwarded-Host header (original host requested by client)
        if (originalHost != null && !originalHost.isEmpty()) {
            requestContext.getRequestHeaders().set("X-Forwarded-Host", originalHost);
        }
        
        // Add X-Forwarded-Port header (port the client connected to)
        requestContext.getRequestHeaders().set("X-Forwarded-Port", String.valueOf(port));
        
        // Add X-Forwarded-Proto header (protocol the client used)
        requestContext.getRequestHeaders().set("X-Forwarded-Proto", protocol);
        
        // Add X-Real-IP header (client's IP address)
        requestContext.getRequestHeaders().set("X-Real-IP", clientIP);
        
        // Add Host header (original host requested by client)
        if (originalHost != null && !originalHost.isEmpty()) {
            requestContext.getRequestHeaders().set("Host", originalHost);
        }
        
        // Generate and add X-Request-Id header for request tracking
        String requestId = generateRequestId();
        requestContext.getRequestHeaders().set("X-Request-Id", requestId);
        
        Log.debugf("Headers added successfully with request ID: %s", requestId);
        
        ctx.next();
    }
    
    /**
     * Extracts the client IP address from the request.
     * Considers X-Forwarded-For and X-Real-IP headers if present,
     * falling back to the remote address from the request.
     * 
     * @param request the HTTP server request
     * @return the client's IP address
     */
    private String extractClientIP(HttpServerRequest request) {
        // Check for X-Forwarded-For header first (in case request was proxied before)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the last one (the most recent)
            String[] ips = xForwardedFor.split(",");
            return ips[ips.length - 1].trim();
        }
        
        // Check for X-Real-IP header
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        // Fall back to remote address
        String remoteAddress = request.remoteAddress().host();
        return remoteAddress != null ? remoteAddress : "unknown";
    }
    
    /**
     * Generates a unique request ID for tracking purposes.
     * Uses a combination of timestamp and random values to ensure uniqueness.
     * 
     * @return a unique request ID
     */
    private String generateRequestId() {
        return System.currentTimeMillis() + "-" + System.nanoTime();
    }
}
