# Load Balancing Algorithm Strategy Pattern

## Overview

The load balancing algorithm implementation follows the **Strategy Pattern**, allowing different algorithms to be plugged in dynamically. Each algorithm is a CDI-managed bean that can be injected and used by the load balancer.

## Architecture

### Core Interface
- **`LoadBalancingAlgorithm`**: Strategy interface defining `selectTarget()` method

### Implementations

1. **`RoundRobinAlgorithm`** (`ROUND_ROBIN`)
   - Distributes requests evenly in sequential order
   - Uses atomic counter for thread-safe operation
   - Best for: Servers with similar capabilities

2. **`WeightedAlgorithm`** (`WEIGHTED`)
   - Distributes based on target weights
   - Higher weight = more requests
   - Uses weighted random selection
   - Best for: Servers with different capacities

3. **`LeastConnectionsAlgorithm`** (`LRT` - Least Response Time)
   - Selects target with fewest active connections
   - Proxy for response time optimization
   - Best for: Long-running requests, variable load

4. **`StickySessionAlgorithm`** (`STICKY`)
   - Routes same client to same server
   - Session identification via:
     - `JSESSIONID` cookie
     - `LB_SESSION` cookie
     - `X-Forwarded-For` header
     - `X-Real-IP` header
   - Falls back to round-robin for new sessions
   - Handles unhealthy target failover
   - Best for: Stateful applications

### Registry
- **`LoadBalancingAlgorithmRegistry`**: CDI-managed registry
  - Auto-discovers all algorithm implementations
  - Provides lookup by name
  - Supports default fallback

## Usage

### 1. Inject the Registry
```java
@Inject
LoadBalancingAlgorithmRegistry registry;
```

### 2. Get Algorithm by Name
```java
LoadBalancingAlgorithm algorithm = registry.getAlgorithm("ROUND_ROBIN");
```

### 3. Select Target
```java
Optional<Target> target = algorithm.selectTarget(targetGroup, requestContext);
```

### 4. Handle Connection Lifecycle
```java
if (target.isPresent()) {
    Target selectedTarget = target.get();
    selectedTarget.incrementConnections();
    
    try {
        // Handle request
    } finally {
        selectedTarget.decrementConnections();
    }
}
```

## Integration with TargetGroup

Each `TargetGroup` has an associated algorithm name. The typical flow:

```java
// Get algorithm for the target group
String algorithmName = targetGroup.getAlgorithm();
LoadBalancingAlgorithm algorithm = registry.getAlgorithm(algorithmName);

// Select target for request
Optional<Target> target = algorithm.selectTarget(targetGroup, requestContext);
```

## Thread Safety

All algorithms are thread-safe:
- **RoundRobinAlgorithm**: Uses `AtomicInteger` for counter
- **WeightedAlgorithm**: Uses `ThreadLocalRandom` (no shared state)
- **LeastConnectionsAlgorithm**: Reads atomic connection counts
- **StickySessionAlgorithm**: Uses `ConcurrentHashMap` for session cache

## CDI Scope

All algorithms are `@ApplicationScoped`, meaning:
- Single instance per application
- Shared across all requests
- Managed by Quarkus Arc CDI container

## Testing

Comprehensive test coverage (58 tests total):
- Algorithm behavior tests (31 tests)
- Model tests (17 tests)
- Registry integration tests (10 tests)

All tests pass with 100% success rate.

## Configuration

Algorithms are configured per target group in `application.properties`:

```yaml
lb:
  target-groups:
    - path: /api
      algorithm: ROUND_ROBIN
      targets:
        - url: http://localhost:8080
        - url: http://localhost:8081
```

If no algorithm is specified, the default from `LoadBalancerConfig` is used.
