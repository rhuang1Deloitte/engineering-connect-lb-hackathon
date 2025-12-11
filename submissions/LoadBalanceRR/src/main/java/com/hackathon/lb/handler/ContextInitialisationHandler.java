package com.hackathon.lb.handler;

import com.hackathon.lb.model.RequestContext;
import com.hackathon.lb.model.TargetGroup;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

/**
 * Parameterized handler that initializes request context for a specific target group.
 * Instances are created per-target-group by ContextInitialisationHandlerFactory,
 * eliminating the need for runtime lookups.
 */
public class ContextInitialisationHandler implements Handler<RoutingContext> {
    public static final String REQUEST_CONTEXT = "requestContext";
    
    private final TargetGroup targetGroup;

    /**
     * Package-private constructor - use ContextInitialisationHandlerFactory to create instances.
     */
    ContextInitialisationHandler(TargetGroup targetGroup) {
        this.targetGroup = targetGroup;
    }

    /**
     * Extracts request metadata and saves a RequestContext for downstream handlers.
     */
    @Override
    public void handle(RoutingContext event) {
        HttpServerRequest request = event.request();
        String requestPath = request.path();

        // Apply path rewriting if configured
        String rewritePath = calculateRewritePath(requestPath, targetGroup.getPathRewrite());

        RequestContext requestContext = RequestContext.builder()
                .method(request.method())
                .originalPath(requestPath)
                .requestHeaders(request.headers())
                .requestBody(request)
                .targetGroup(targetGroup)
                .rewritePath(rewritePath)
                .build();

        event.put(REQUEST_CONTEXT, requestContext);

        event.next();
    }

    /**
     * Calculates the rewritten path by stripping the configured prefix.
     * If pathRewrite is null or empty, returns the original path.
     * If the request path doesn't start with the prefix, returns the original path.
     * 
     * @param requestPath the original request path
     * @param pathRewrite the prefix to strip
     * @return the rewritten path
     */
    private String calculateRewritePath(String requestPath, String pathRewrite) {
        if (pathRewrite == null || pathRewrite.isEmpty()) {
            return requestPath;
        }
        
        if (requestPath.startsWith(pathRewrite)) {
            String rewritten = requestPath.substring(pathRewrite.length());
            // Ensure the rewritten path starts with / if not empty
            if (!rewritten.isEmpty() && !rewritten.startsWith("/")) {
                rewritten = "/" + rewritten;
            }
            // If empty, return /
            if (rewritten.isEmpty()) {
                rewritten = "/";
            }
            return rewritten;
        }
        
        return requestPath;
    }

    /**
     * Stores the constructed RequestContext inside the routing context for downstream handlers.
     */
    public static void saveContext(RoutingContext routingContext, RequestContext requestContext) {
        routingContext.put(REQUEST_CONTEXT, requestContext);
    }

    /**
     * Retrieves the RequestContext associated with the current routing context.
     */
    public static RequestContext getContext(RoutingContext routingContext) {
        return routingContext.get(REQUEST_CONTEXT);
    }
}
