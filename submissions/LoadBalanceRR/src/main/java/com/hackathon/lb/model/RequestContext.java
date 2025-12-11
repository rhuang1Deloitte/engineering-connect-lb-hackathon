package com.hackathon.lb.model;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.streams.ReadStream;
import lombok.Builder;
import lombok.Data;

/**
 * Captures data about the incoming request, upstream target selection, and response.
 */
@Data
@Builder
public class RequestContext {
    private HttpMethod method;
    private String originalPath;
    private MultiMap requestHeaders;
    private ReadStream<Buffer> requestBody;

    private TargetGroup targetGroup;
    private Target target;
    private String rewritePath;

    private Integer responseStatus;
    private String responseStatusMessage;
    private MultiMap responseHeaders;
    private Buffer responseBody;
    
    // Retry tracking
    private int retryAttempt;
} 