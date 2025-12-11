package com.hackathon.lb.handler;

import com.hackathon.lb.config.LoadBalancerConfig;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Factory for creating HeaderConventionHandler instances conditionally.
 * Only creates handlers if the header convention feature is enabled via configuration.
 */
@ApplicationScoped
public class HeaderConventionHandlerFactory {
    
    @Inject
    LoadBalancerConfig config;
    
    /**
     * Creates a new HeaderConventionHandler if header conventions are enabled.
     * Returns a no-op handler if disabled.
     * 
     * @return a HeaderConventionHandler if enabled, or a no-op handler if disabled
     */
    public io.vertx.core.Handler<io.vertx.ext.web.RoutingContext> create() {
        if (config.headerConventionEnabled()) {
            Log.info("Header convention handler enabled");
            return new HeaderConventionHandler();
        } else {
            Log.info("Header convention handler disabled");
            // Return a no-op handler that just passes through
            return ctx -> ctx.next();
        }
    }
}
