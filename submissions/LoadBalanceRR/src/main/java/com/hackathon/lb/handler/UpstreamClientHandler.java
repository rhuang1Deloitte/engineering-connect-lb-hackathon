package com.hackathon.lb.handler;

import com.hackathon.lb.config.LoadBalancerConfig;
import com.hackathon.lb.model.RequestContext;
import com.hackathon.lb.model.Target;

import io.quarkus.logging.Log;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Handler that makes HTTP requests to upstream target servers using WebClient.
 * This handler:
 * 1. Retrieves the selected Target from the RequestContext
 * 2. Makes an HTTP request to the target using the shared WebClient
 * 3. Updates the RequestContext with response details (status, headers, body)
 */
@ApplicationScoped
public class UpstreamClientHandler implements Handler<RoutingContext> {
    
    @Inject
    Vertx vertx;
    
    @Inject
    LoadBalancerConfig config;
    
    @ConfigProperty(name = "CONNECTION_TIMEOUT", defaultValue = "5000")
    long connectionTimeoutMs;
    
    private WebClient client;
    
    @PostConstruct
    void init() {
        WebClientOptions options = new WebClientOptions()
                .setTcpFastOpen(true)
                .setTcpNoDelay(true)
                .setKeepAlive(true)
                .setMaxPoolSize(100)
                .setMaxWaitQueueSize(1000)
                .setIdleTimeout(15)
                .setIdleTimeoutUnit(TimeUnit.SECONDS)
                // only connect timeout here; read timeout will be per request
                .setConnectTimeout((int) connectionTimeoutMs);
        
        client = WebClient.create(vertx, options);
        
        Log.infof("UpstreamClientHandler initialized with WebClient (retry=%s, maxRetries=%d, backoff=%dms)", 
                  config.retryEnabled(), config.retryCount(), config.retryBackoffMillis());
    }
    
    @Override
    public void handle(RoutingContext ctx) {
        RequestContext requestContext = ContextInitialisationHandler.getContext(ctx);
        
        if (requestContext == null) {
            Log.error("RequestContext not found in routing context");
            ctx.response().setStatusCode(500).end("Internal server error: missing request context");
            return;
        }
        
        Target target = requestContext.getTarget();
        if (target == null) {
            Log.error("No target selected in RequestContext");
            // Matched rule but no target -> 503 with empty body (spec)
            ctx.response().setStatusCode(503).end();
            return;
        }
        
        // Parse target URL to extract host, port, and scheme
        URI targetUri;
        try {
            targetUri = URI.create(target.getUrl());
        } catch (IllegalArgumentException e) {
            Log.errorf("Invalid target URL: %s", target.getUrl());
            ctx.response().setStatusCode(500).end("Internal server error: invalid target URL");
            return;
        }
        
        String host = targetUri.getHost();
        int port = targetUri.getPort() != -1 ? targetUri.getPort() : 
                   ("https".equals(targetUri.getScheme()) ? 443 : 80);
        boolean ssl = "https".equals(targetUri.getScheme());
        
        // Use rewrite path if available, otherwise use original path
        String requestPath = requestContext.getRewritePath() != null ? 
                            requestContext.getRewritePath() : 
                            requestContext.getOriginalPath();
        
        // Preserve query string when forwarding
        String uri = requestPath;
        String query = ctx.request().query();
        if (query != null && !query.isEmpty()) {
            uri = requestPath + "?" + query;
        }
        
        Log.debugf("Forwarding request to target: %s (host=%s, port=%d, ssl=%s, uri=%s)", 
                   target.getUrl(), host, port, ssl, uri);
        
        // Increment active connections counter
        target.incrementConnections();
        
        // Wrap the upstream request in circuit breaker with retry logic
        if (config.retryEnabled()) {
            executeWithRetry(ctx, requestContext, target, host, port, ssl, uri, 0);
        } else {
            executeRequest(ctx, requestContext, target, host, port, ssl, uri);
        }
    }
    
    /**
     * Execute a request with exponential backoff retry logic.
     * Retries on 5xx errors and connection failures, but NOT on 4xx errors.
     */
    private void executeWithRetry(RoutingContext ctx, RequestContext requestContext, 
                                   Target target, String host, int port, boolean ssl, 
                                   String uri, int attemptNumber) {
        
        executeRequest(ctx, requestContext, target, host, port, ssl, uri, (succeeded, response, cause) -> {
            if (succeeded) {
                // Success - pass through
                ctx.next();
            } else {
                // Check if we should retry
                int statusCode = requestContext.getResponseStatus();
                boolean is4xx = statusCode >= 400 && statusCode < 500;
                boolean is5xx = statusCode >= 500 && statusCode < 600;
                boolean isConnectionError = statusCode == 0;
                
                // Retry on 5xx or connection errors, but NOT on 4xx
                boolean shouldRetry = (is5xx || isConnectionError) && attemptNumber < config.retryCount();
                
                if (shouldRetry) {
                    // Calculate exponential backoff: initialBackoff * (2^attemptNumber)
                    long backoffMs = config.retryBackoffMillis() * (long) Math.pow(2, attemptNumber);
                    
                    Log.warnf("Request to %s failed (status=%d, attempt=%d/%d), retrying in %dms", 
                             target.getUrl(), statusCode, attemptNumber + 1, config.retryCount(), backoffMs);
                    
                    // Schedule retry with exponential backoff
                    vertx.setTimer(backoffMs, timerId -> {
                        executeWithRetry(ctx, requestContext, target, host, port, ssl, uri, attemptNumber + 1);
                    });
                } else {
                    // No more retries or 4xx error - return the error response
                    if (is4xx) {
                        Log.debugf("Request to %s returned 4xx status=%d, not retrying", 
                                  target.getUrl(), statusCode);
                    } else {
                        Log.warnf("Request to %s failed after %d retries (status=%d)", 
                                 target.getUrl(), attemptNumber, statusCode);
                    }
                    
                    // Return appropriate error status
                    if (statusCode > 0) {
                        // We got a response from upstream, pass it through
                        ctx.next();
                    } else {
                        // Connection error - return 502 or 504
                        String message = cause != null && cause.getMessage() != null
                                ? cause.getMessage().toLowerCase()
                                : "";
                        
                        if (message.contains("timeout")) {
                            ctx.response().setStatusCode(504).end();
                        } else {
                            ctx.response().setStatusCode(502).end();
                        }
                    }
                }
            }
        });
    }
    
    /**
     * Execute a single request without retry logic.
     */
    private void executeRequest(RoutingContext ctx, RequestContext requestContext, 
                               Target target, String host, int port, boolean ssl, String uri) {
        executeRequest(ctx, requestContext, target, host, port, ssl, uri, (succeeded, response, cause) -> {
            if (succeeded) {
                ctx.next();
            } else {
                // Return error response
                String message = cause != null && cause.getMessage() != null
                        ? cause.getMessage().toLowerCase()
                        : "";
                
                if (message.contains("timeout")) {
                    ctx.response().setStatusCode(504).end();
                } else {
                    ctx.response().setStatusCode(502).end();
                }
            }
        });
    }
    
    /**
     * Core method to execute an upstream HTTP request.
     * Calls the provided callback with the result.
     */
    private void executeRequest(RoutingContext ctx, RequestContext requestContext, 
                               Target target, String host, int port, boolean ssl, String uri,
                               RequestCallback callback) {
        
        // Build the request using the (possibly rewritten) path + query
        var request = client
                .request(requestContext.getMethod(), port, host, uri)
                .ssl(ssl)
                .putHeaders(requestContext.getRequestHeaders())
                .timeout(connectionTimeoutMs);

        request.sendStream(requestContext.getRequestBody(), ar -> {
            // Decrement active connections counter
            target.decrementConnections();
            
            if (ar.succeeded()) {
                HttpResponse<Buffer> response = ar.result();
                
                // Update RequestContext with response details
                requestContext.setResponseStatus(response.statusCode());
                requestContext.setResponseStatusMessage(response.statusMessage());
                requestContext.setResponseHeaders(response.headers());
                requestContext.setResponseBody(response.body());
                
                Log.debugf("Received response from target %s: status=%d", 
                          target.getUrl(), response.statusCode());
                
                // Invoke callback with success
                callback.handle(true, response, null);
            } else {
                Throwable cause = ar.cause();
                Log.warnf(cause, "Failed to connect or get response from target %s", target.getUrl());
                
                // Record error info in the context
                requestContext.setResponseStatus(0);
                requestContext.setResponseStatusMessage(
                    cause != null ? cause.getMessage() : "Upstream failure"
                );
                
                // Invoke callback with failure
                callback.handle(false, null, cause);
            }
        });
    }
    
    /**
     * Callback interface for request completion.
     */
    @FunctionalInterface
    private interface RequestCallback {
        void handle(boolean succeeded, HttpResponse<Buffer> response, Throwable cause);
    }
}
