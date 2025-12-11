package com.hackathon.lb.handler;

import com.hackathon.lb.model.RequestContext;

import io.quarkus.logging.Log;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Handler that sends the HTTP response back to the client based on upstream response details.
 * This handler:
 * 1. Retrieves response details from the RequestContext (populated by UpstreamClientHandler)
 * 2. Sets the response status code and message
 * 3. Copies response headers from upstream
 * 4. Sends the response body back to the client
 */
@ApplicationScoped
public class ServerResponseHandler implements Handler<RoutingContext> {
    
    @Override
    public void handle(RoutingContext ctx) {
        RequestContext requestContext = ContextInitialisationHandler.getContext(ctx);
        
        if (requestContext == null) {
            Log.error("RequestContext not found in routing context");
            ctx.response().setStatusCode(500).end("Internal server error: missing request context");
            return;
        }
        
        // Check if we have response details from upstream
        if (requestContext.getResponseStatus() == null) {
            Log.error("No response status found in RequestContext");
            ctx.response().setStatusCode(502).end("Bad Gateway: no upstream response");
            return;
        }
        
        var serverResponse = ctx.response();
        
        // Set status code and message
        serverResponse.setStatusCode(requestContext.getResponseStatus());
        if (requestContext.getResponseStatusMessage() != null) {
            serverResponse.setStatusMessage(requestContext.getResponseStatusMessage());
        }
        
        // Copy response headers from upstream
        if (requestContext.getResponseHeaders() != null) {
            serverResponse.headers().addAll(requestContext.getResponseHeaders());
        }
        
        Log.debugf("Sending response to client: status=%d, path=%s", 
                  requestContext.getResponseStatus(), requestContext.getOriginalPath());
        
        // Send response body if present
        if (requestContext.getResponseBody() != null) {
            serverResponse.end(requestContext.getResponseBody());
        } else {
            serverResponse.end();
        }
    }
}
