# Path Rewrite Feature

## Overview

The Path Rewrite feature allows target groups to strip a specified prefix from incoming request URIs before forwarding them to upstream targets. This is useful for API versioning, service proxying, and clean URL management.

## Implementation

### Configuration

Path rewriting is configured per target group using the optional `pathRewrite` parameter:

```yaml
lbConfig:
  targetGroups:
    api:
      path: /api/v1/
      pathRewrite: /api/v1
      targets:
        - url: http://localhost:9000
```

### Behavior

When a request is received:
1. The load balancer matches the request path against the target group's `path` prefix
2. If `pathRewrite` is configured, that prefix is stripped from the URI
3. The modified URI is forwarded to the selected upstream target

### Examples

#### Example 1: API Versioning
**Configuration:**
```yaml
api:
  path: /api/v1/
  pathRewrite: /api/v1
  targets:
    - url: http://backend:8080
```

**Request:** `GET /api/v1/users/123`  
**Forwarded as:** `GET /users/123` to `http://backend:8080`

#### Example 2: Service Prefix Stripping
**Configuration:**
```yaml
services:
  path: /services/users/
  pathRewrite: /services/users
  targets:
    - url: http://user-service:3000
```

**Request:** `GET /services/users/profile`  
**Forwarded as:** `GET /profile` to `http://user-service:3000`

#### Example 3: No Path Rewrite
**Configuration:**
```yaml
echo:
  path: /echo/
  targets:
    - url: http://localhost:8090
```

**Request:** `GET /echo/test`  
**Forwarded as:** `GET /echo/test` to `http://localhost:8090`

## Technical Details

### Implementation Files

1. **TargetGroupConfig.java** - Added `Optional<String> pathRewrite()` configuration option
2. **TargetGroup.java** - Added `pathRewrite` field and getter
3. **TargetGroupFactory.java** - Extracts `pathRewrite` from config and passes to TargetGroup
4. **ContextInitialisationHandler.java** - Calculates and sets `rewritePath` in RequestContext
5. **UpstreamClientHandler.java** - Uses `rewritePath` when forwarding requests to targets

### Algorithm

The path rewrite algorithm in `ContextInitialisationHandler.calculateRewritePath()`:

1. If `pathRewrite` is null or empty, return the original path
2. If the request path starts with `pathRewrite`, strip that prefix
3. Ensure the result starts with `/` (or is exactly `/` if empty after stripping)
4. Otherwise, return the original path unchanged

### Edge Cases Handled

- **Exact prefix match:** `/api` → `/`
- **Trailing slashes:** `/api/` → `/`
- **No match:** Path doesn't start with prefix → original path preserved
- **Empty/null pathRewrite:** No rewriting occurs
- **Nested paths:** `/services/api/v1/users` with rewrite `/services/api` → `/v1/users`

## Testing

Comprehensive test suite in `PathRewriteTest.java` covers:
- Path rewrite configuration storage
- Null and empty pathRewrite handling
- Nested path rewriting
- Integration with TargetGroup model

All existing tests pass without modification (71 tests total, 4 skipped).

## Configuration Example

See `src/test/resources/config.yml` for a complete configuration example demonstrating path rewrite functionality.
