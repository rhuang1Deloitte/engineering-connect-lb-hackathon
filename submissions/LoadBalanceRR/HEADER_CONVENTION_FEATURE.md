# Header Convention Feature Implementation

## Overview
Implemented the "Proper Use of Header Conventions" stretch requirement for the LoadBalanceRR load balancer. This feature adds standard HTTP proxy headers to upstream requests, enabling better request tracking and service integration.

## Implementation Details

### Configuration
- **File**: `src/main/resources/application.properties`
- **Environment Variable**: `HEADER_CONVENTION_ENABLE` (boolean, defaults to true)
- **Config Property**: `lbConfig.headerConventionEnabled`
- **Updated**: `LoadBalancerConfig.java` interface to include `headerConventionEnabled()` method

### Core Handler: HeaderConventionHandler
- **File**: `src/main/java/com/hackathon/lb/handler/HeaderConventionHandler.java`
- **Purpose**: Intercepts requests in the handler pipeline and adds standard proxy headers before forwarding to upstream services
- **Position in Pipeline**: Placed between `ContextInitialisationHandler` and `LoadBalancingHandler`

### Headers Added
The following headers are added to upstream requests per Oracle Load Balancer conventions:

1. **X-Forwarded-For**: List of connection IP addresses (supports proxy chaining by appending to existing value)
2. **X-Forwarded-Host**: Original host and port requested by the client
3. **X-Forwarded-Port**: Listener port number that the client used to connect
4. **X-Forwarded-Proto**: Protocol used by the client (http or https)
5. **X-Real-IP**: Client's IP address
6. **Host**: Original host requested by the client
7. **X-Request-Id**: Unique request tracking ID (generated as timestamp-nanotime)

### Client IP Extraction Logic
The handler intelligently extracts client IP in the following priority:
1. From existing `X-Forwarded-For` header (supports proxy chaining)
2. From `X-Real-IP` header (if X-Forwarded-For not present)
3. From request's remote address (fallback)

### Factory: HeaderConventionHandlerFactory
- **File**: `src/main/java/com/hackathon/lb/handler/HeaderConventionHandlerFactory.java`
- **Purpose**: Conditionally creates HeaderConventionHandler based on `HEADER_CONVENTION_ENABLE` configuration
- **Behavior**: 
  - If enabled: Returns a fully functional HeaderConventionHandler
  - If disabled: Returns a no-op handler that simply calls `ctx.next()`
- **Injection**: Integrated into TargetGroupRouteRegistrar via Arc CDI

### Integration Points
- **File Modified**: `src/main/java/com/hackathon/lb/handler/TargetGroupRouteRegistrar.java`
- **Changes**:
  - Injected `HeaderConventionHandlerFactory`
  - Added HeaderConventionHandler to the request handler chain
  - Updated documentation to reflect new handler position

### Handler Pipeline
The complete request handler chain now follows this order:
1. **ContextInitialisationHandler** - Populates RequestContext with target group info
2. **HeaderConventionHandler** - Adds standard proxy headers (NEW)
3. **LoadBalancingHandler** - Selects target using configured algorithm
4. **UpstreamClientHandler** - Makes HTTP request to selected target
5. **ServerResponseHandler** - Sends response back to client

## Testing

### Test Class: HeaderConventionHandlerTest
- **File**: `src/test/java/com/hackathon/lb/handler/HeaderConventionHandlerTest.java`
- **Test Coverage**:
  - Basic header addition (all 7 headers)
  - X-Forwarded-For appending for proxy chaining
  - Case-insensitive header handling
  - Handling of empty/null values
  - Multiple header co-existence
  - Handler and factory instantiation

### Test Results
All tests pass successfully (78 tests total, 7 new HeaderConventionHandlerTest tests):
```
Tests run: 78, Failures: 0, Errors: 0, Skipped: 4
```

## Configuration Examples

### Enable Header Conventions (Default)
```bash
docker run ... -e HEADER_CONVENTION_ENABLE=true LoadBalanceRR:latest
```

### Disable Header Conventions
```bash
docker run ... -e HEADER_CONVENTION_ENABLE=false LoadBalanceRR:latest
```

### Environment Variable in Java System Properties
```bash
java -jar target/quarkus-app/quarkus-run.jar -DHEADER_CONVENTION_ENABLE=false
```

## Compliance with Requirements

✅ **Proper Use of Header Conventions** - All required headers implemented:
- X-Forwarded-For
- X-Forwarded-Host
- X-Forwarded-Port
- X-Forwarded-Proto
- X-Real-IP
- Host
- X-Request-Id (for request tracking)

✅ **Optional Configuration** - Feature is toggleable via `HEADER_CONVENTION_ENABLE` environment variable

✅ **Dedicated Handler** - Custom `HeaderConventionHandler` class for clean separation of concerns

✅ **Factory Pattern** - Conditional instantiation via `HeaderConventionHandlerFactory`

✅ **Proxy Chaining** - Properly appends to existing X-Forwarded-For header for multi-hop proxying

✅ **Request Tracking** - Generates unique X-Request-Id for each request

## Performance Considerations

- **Minimal Overhead**: Handler only adds headers to existing MultiMap, no network calls
- **Memory Efficient**: No caching or temporary data structures created
- **No-op When Disabled**: Returns a pass-through handler when feature is disabled

## Notes

- Feature defaults to **enabled** (true) as per Oracle Load Balancer best practices
- The implementation follows the existing Quarkus Arc CDI patterns used throughout LoadBalanceRR
- All headers are added before forwarding to upstream, ensuring servers receive complete context
