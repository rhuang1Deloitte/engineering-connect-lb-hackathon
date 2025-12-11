package com.hackathon.lb.handler;

import com.hackathon.lb.model.TargetGroup;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Factory for creating parameterized ContextInitialisationHandler instances.
 * Each instance is bound to a specific TargetGroup, eliminating the need
 * for runtime lookups in the request handling path.
 */
@ApplicationScoped
public class ContextInitialisationHandlerFactory {
    
    /**
     * Creates a new ContextInitialisationHandler bound to the specified target group.
     * 
     * @param targetGroup the target group this handler will be bound to
     * @return a new handler instance for the target group
     */
    public ContextInitialisationHandler create(TargetGroup targetGroup) {
        return new ContextInitialisationHandler(targetGroup);
    }
}
