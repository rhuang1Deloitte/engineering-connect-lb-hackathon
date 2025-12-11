package com.hackathon.lb.handler;

import com.hackathon.lb.algorithm.LoadBalancingAlgorithm;
import com.hackathon.lb.model.RequestContext;
import com.hackathon.lb.model.Target;
import com.hackathon.lb.model.TargetGroup;

import io.quarkus.logging.Log;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * Parameterized handler that selects a target from the target group using a pre-bound load balancing algorithm.
 * Instances are created per-target-group by LoadBalancingHandlerFactory,
 * eliminating the need for runtime algorithm lookups.
 * 
 * This handler:
 * 1. Retrieves the TargetGroup from the RequestContext
 * 2. Invokes the pre-bound algorithm to select a target
 * 3. Updates the RequestContext with the selected target
 */
public class LoadBalancingHandler implements Handler<RoutingContext> {
    
    private final LoadBalancingAlgorithm algorithm;
    
    /**
     * Package-private constructor - use LoadBalancingHandlerFactory to create instances.
     */
    LoadBalancingHandler(LoadBalancingAlgorithm algorithm) {
        this.algorithm = algorithm;
    }
    
    @Override
    public void handle(RoutingContext ctx) {
        RequestContext requestContext = ContextInitialisationHandler.getContext(ctx);
        
        if (requestContext == null) {
            Log.error("RequestContext not found in routing context");
            ctx.response().setStatusCode(500).end("Internal server error: missing request context");
            return;
        }
        
        TargetGroup targetGroup = requestContext.getTargetGroup();
        if (targetGroup == null) {
            Log.error("TargetGroup not found in RequestContext");
            ctx.response().setStatusCode(500).end("Internal server error: missing target group");
            return;
        }
        
        // Select a target using the pre-bound algorithm
        var targetOptional = algorithm.selectTarget(targetGroup, requestContext);
        
        if (targetOptional.isEmpty()) {
            Log.warnf("No healthy target available for target group '%s'", targetGroup.getPath());
            // 503 with empty payload
            ctx.response().setStatusCode(503).end();
            return;
        }
        
        Target selectedTarget = targetOptional.get();
        
        // Update the request context with the selected target
        requestContext.setTarget(selectedTarget);
        
        Log.debugf("Selected target %s for path %s using algorithm %s", 
            selectedTarget.getUrl(), targetGroup.getPath(), algorithm.getName());
        
        // Proceed to the next handler
        ctx.next();
    }
}
