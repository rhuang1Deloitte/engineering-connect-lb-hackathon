package com.hackathon.lb;

import io.quarkus.logging.Log;
import io.quarkus.vertx.VertxOptionsCustomizer;
import io.vertx.core.VertxOptions;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class VertxConfigDebug implements VertxOptionsCustomizer {

    @Override
    public void accept(VertxOptions options) {
        Log.info("Vert.x Event Loop Pool Size: " + options.getEventLoopPoolSize());
        Log.info("Vert.x Worker Pool Size: " + options.getWorkerPoolSize());
        Log.info("Vert.x Internal Blocking Pool Size: " + options.getInternalBlockingPoolSize());
    }
    
}
