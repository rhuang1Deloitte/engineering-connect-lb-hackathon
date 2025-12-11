# Instance Health Checks Implementation Summary

## Overview
Implemented a comprehensive Instance Health Checks feature for the LoadBalanceRR load balancer. This allows each target group to periodically health check its backend targets and automatically mark them as healthy or unhealthy based on configurable success/failure thresholds.

## Architecture

### Key Components

#### 1. **HealthCheckScheduler** (`src/main/java/com/hackathon/lb/feature/HealthCheckScheduler.java`)
- **Purpose**: Manages all periodic health check tasks using Vertx's timer API
- **Scope**: ApplicationScoped CDI bean for application-wide health check management
- **Key Responsibilities**:
  - Schedule periodic health checks for enabled target groups
  - Execute HTTP GET requests to health check endpoints
  - Track consecutive successes and failures per target
  - Update target health status based on thresholds
  - Provide logging for health status changes

#### 2. **Target Model Updates** (`src/main/java/com/hackathon/lb/model/Target.java`)
- Added `incrementSuccesses()` method to track consecutive successful health checks
- Added `incrementFailures()` method to track consecutive failed health checks
- Both methods reset the opposing counter (successes reset on failure and vice versa)
- Existing `markHealthy()` and `markUnhealthy()` methods already present for state transitions

#### 3. **TargetGroupRegistry Integration** (`src/main/java/com/hackathon/lb/model/TargetGroupRegistry.java`)
- Injects `HealthCheckScheduler` bean
- Calls `scheduleHealthChecks()` during `@PostConstruct` initialization for each target group
- Passes the target group name for logging purposes

#### 4. **Existing Configuration Support**
- Leverages existing `HealthCheckConfig` interface with proper defaults
- Existing `TargetGroup.HealthCheck` model already structured correctly
- Configuration is read from YAML via Quarkus configuration system

## How It Works

### Health Check Execution Flow
1. **Initialization** (at application startup):
   - `TargetGroupRegistry` loads all target groups from configuration
   - For each target group with health checks enabled, `HealthCheckScheduler.scheduleHealthChecks()` is called
   - A Vertx periodic timer is created with the configured interval

2. **Periodic Execution** (every `interval` milliseconds):
   - Timer callback invokes `performHealthChecks()` for all targets in the group
   - Each target's health check endpoint is called with HTTP GET request
   - Response status is checked: 200 = success, any other = failure

3. **State Management**:
   - **On Success**: 
     - Increment consecutive successes counter
     - When successes reach `successThreshold`, mark target as healthy
   - **On Failure**:
     - Increment consecutive failures counter  
     - When failures reach `failureThreshold`, mark target as unhealthy

### Logging

The implementation provides three levels of logging:

1. **INFO Level** (always logged):
   - Health check scheduling starts: `"Scheduling health checks for target group '%s' with interval %d ms"`
   - Single health check failure: `"Target %s in group '%s' failed health check (failures: %d/%d)"`
   - Target becomes healthy: `"Target %s in group '%s' is now HEALTHY (success threshold %d reached)"`

2. **WARN Level** (state change alerts):
   - Target becomes unhealthy: `"Target %s in group '%s' is now UNHEALTHY (failure threshold %d reached)"`

3. **DEBUG Level** (operational):
   - Health checks not enabled: logged at DEBUG
   - Timer cancellation: logged at DEBUG

## Configuration

### YAML Example
```yaml
lbConfig:
  targetGroups:
    trio:
      path: /trio/
      targets:
        - url: http://localhost:8090
        - url: http://localhost:8091
        - url: http://localhost:8092
      healthCheck:
        enabled: true              # Enable health checks for this group
        path: /healthz            # Health check endpoint path
        interval: 5000            # Check interval in milliseconds
        successThreshold: 1       # Success checks before marking healthy
        failureThreshold: 3       # Failure checks before marking unhealthy
```

### Configuration Parameters
- `enabled` (boolean): Activate health checks for this target group
- `path` (string): Relative path for health check endpoint (e.g., `/health`, `/healthz`)
- `interval` (integer): Milliseconds between health checks (minimum 1)
- `successThreshold` (integer): Consecutive successful responses to mark healthy (minimum 1)
- `failureThreshold` (integer): Consecutive failures to mark unhealthy (minimum 1)

## Implementation Details

### Health Check Request Format
- **Method**: HTTP GET
- **Target URL**: Combines target base URL with health check path
  - Target: `http://host:port/base/path`
  - Health endpoint: `/healthz`
  - Full request: `http://host:port/base/path/healthz`
- **Timeout**: 5 seconds (connect and idle)
- **Success Criteria**: HTTP 200 response code only

### State Tracking
Each `Target` maintains:
- `healthy` (AtomicBoolean): Current health status
- `consecutiveSuccesses` (AtomicInteger): Count of consecutive successful checks
- `consecutiveFailures` (AtomicInteger): Count of consecutive failed checks
- These are thread-safe for concurrent health check operations

### Timer Management
- Timers are stored in a `ConcurrentMap<String, List<Long>>` indexed by target group name
- Timers can be cancelled via `cancelHealthChecks(groupName)`
- All timers are cleaned up in `@PreDestroy` during application shutdown

## Testing

### Unit Tests (`src/test/java/com/hackathon/lb/feature/HealthCheckSchedulerTest.java`)
13 comprehensive unit tests covering:
- Target success/failure increment logic
- Counter reset behavior
- Health status transitions
- Target group healthy target filtering
- Health check configuration validation
- Disabled health check handling

### Test Results
- All 91 tests pass (including 13 new health check tests)
- No test failures or errors
- 4 tests skipped (integration-level tests not enabled in unit test phase)

### Configuration Used in Tests
Test configuration in `src/test/resources/config.yml` includes:
- `echo` group: No health checks
- `trio` group: Health checks enabled with 5-second intervals
- `rewrite` group: No health checks

## Health Check Flow Diagram

```
Application Startup
    ↓
TargetGroupRegistry.initialize()
    ↓
For each TargetGroup:
    ├─ If healthCheck.enabled == true
    │   └─ HealthCheckScheduler.scheduleHealthChecks()
    │       └─ vertx.setPeriodic(interval, callback)
    │
Every {interval} milliseconds:
    └─ performHealthChecks()
        └─ For each Target:
            ├─ GET {target.url}{healthCheck.path}
            │
            ├─ If response == 200:
            │   ├─ incrementSuccesses()
            │   └─ if successes >= threshold
            │       └─ markHealthy() + log INFO
            │
            └─ If response != 200 or error:
                ├─ incrementFailures()
                ├─ log INFO (single failure)
                └─ if failures >= threshold
                    └─ markUnhealthy() + log WARN
```

## Integration Points

### With Request Routing
- Request handlers call `targetGroup.getHealthyTargets()` to get only healthy targets
- Load balancing algorithms only distribute requests to healthy targets
- This automatically implements request exclusion for unhealthy targets

### With TargetGroupRegistry
- Registry initializes health check scheduler on startup
- Health checks run independently but target health state is used by request handlers

## Cleanup

Application shutdown (`@PreDestroy`):
- All active Vertx timers are cancelled
- Resources are released
- Graceful shutdown ensures no pending health checks remain

## Error Handling

The implementation gracefully handles:
- **Network errors**: Treated as failed health checks, logged at INFO level
- **Malformed URLs**: Logged at WARN level with exception details
- **Missing health endpoints**: Results in repeated failures until threshold met
- **Connection timeouts**: Treated as failed health checks

## Future Enhancements

Possible future improvements:
- Jitter in health check intervals to prevent thundering herd
- Custom status code handling (e.g., 503 = unhealthy)
- Health check request headers customization
- Metrics collection on health check results
- Circuit breaker pattern integration
