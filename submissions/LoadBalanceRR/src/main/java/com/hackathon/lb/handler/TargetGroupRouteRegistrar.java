package com.hackathon.lb.handler;

import com.hackathon.lb.model.TargetGroup;
import com.hackathon.lb.model.TargetGroupRegistry;

import io.quarkus.logging.Log;
import io.vertx.ext.web.Router;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * Registers routes for all target groups from the TargetGroupRegistry.
 * Uses ContextInitialisationHandlerFactory to create parameterized handlers
 * for each target group, eliminating runtime lookups.
 */
@ApplicationScoped
public class TargetGroupRouteRegistrar {
    
    @Inject
    TargetGroupRegistry registry;
    
    @Inject
    ContextInitialisationHandlerFactory contextInitialisationHandlerFactory;
    
    @Inject
    LoadBalancingHandlerFactory loadBalancingHandlerFactory;
    
    @Inject
    HeaderConventionHandlerFactory headerConventionHandlerFactory;
    
    @Inject
    UpstreamClientHandler upstreamClientHandler;
    
    @Inject
    ServerResponseHandler serverResponseHandler;
    
    /**
     * Observes Router initialization and registers routes for all target groups.
     * This happens after TargetGroupRegistry @PostConstruct initialization.
     */
    void initRoutes(@Observes Router router) {
        Log.info("Installing proxy routes from TargetGroupRegistry...");
        
        registry.getAllTargetGroups().forEach(targetGroup -> {
            registerTargetGroupRoutes(router, targetGroup);
        });
        
        Log.infof("Successfully registered routes for %d target groups", registry.size());

        // If no listener rule matches, return 404 with an *empty* payload.
        router.route().last().handler(ctx -> {
            if (!ctx.response().ended()) {
                ctx.response().setStatusCode(404).end();
            }
        });
    }
    
    /**
     * Registers routes for a single target group with the configured handler chain:
     * 1. Context Initialization - populates RequestContext with target group info
     * 2. Header Convention Handler - adds standard proxy headers (X-Forwarded-*, X-Real-IP, Host)
     * 3. Load Balancing Handler - selects target using the configured algorithm (pre-bound per target group)
     * 4. Upstream Client Handler - makes HTTP request to selected target and updates context
     * 5. Server Response Handler - sends the response back to the client
     */
    private void registerTargetGroupRoutes(Router router, TargetGroup targetGroup) {
        String path = targetGroup.getPath();
        
        Log.infof("Registering routes for path prefix '%s' (algorithm: %s, targets: %d)", 
            path, targetGroup.getAlgorithm(), targetGroup.getTargets().size());
        
        // Create a load balancing handler pre-bound to this target group's algorithm
        LoadBalancingHandler loadBalancingHandler = loadBalancingHandlerFactory.create(targetGroup);
        
        if (loadBalancingHandler == null) {
            Log.errorf("Failed to create load balancing handler for target group '%s' - skipping route registration", path);
            return;
        }
        
        router.route(path + "*")
            .handler(contextInitialisationHandlerFactory.create(targetGroup))
            .handler(headerConventionHandlerFactory.create())
            .handler(loadBalancingHandler)
            .handler(upstreamClientHandler)
            .handler(serverResponseHandler);
    }
}
