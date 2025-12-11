# loadbalancerr

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Features

LoadBalanceRR implements the following load balancing features:

- **Round Robin Algorithm** – Evenly distributes requests across targets
- **Weighted Algorithm** – Routes requests based on target weights
- **Sticky Sessions** – Routes client requests to the same target during a session
- **LRT Algorithm** – Routes based on connection count and response time
- **Path-Based Routing** – Routes requests to different target groups based on URI prefix
- **Request Retries** – Automatically retries requests on 5xx errors and connection failures with exponential backoff
- **Header Conventions** – Adds proxy headers (X-Forwarded-*, X-Real-IP) for upstream services
- **Health Checks** – Monitors target health and removes unhealthy targets temporarily
- **Robust Error Handling** – Returns appropriate HTTP status codes for various failure scenarios

## Configuration

Detailed configuration of the load balancer is done through a YAML config file. A detailed example is provided under `src/test/resources/config.yml`

```yaml
lbConfig:
  # Default balancing algorithm: ROUND_ROBIN, WEIGHTED, STICKY, LRT
  algorithm: ROUND_ROBIN
  targetGroups:
    # each target group represents a backend service
    # multiple target groups can be defined for different services
    # targetGroups are keyed by a unique name
    # A targetGroup has:
    # - path (mandatory): URL path prefix for this target group
    # - algorithm (optional): load balancing algorithm for this target group
    # - targets (mandatory): list of target servers for this target group. Each target has:
    #   - url (mandatory): target server URL
    #   - weight (optional): weight for WEIGHTED algorithm
    # - healthCheck (optional): configuration for health checking the targets
    # - pathRewrite (optional): path prefix to strip from URI before forwarding to targets
    
    # 'echo' is an example of a minimal target group using default algorithm without health checks
    echo:
      path: /echo/
      targets:
        - url: http://localhost:8090
    
    # 'trio' is an example of a fully-specified target group using WEIGHTED algorithm with health checks
    trio:
      path: /trio/
      algorithm: WEIGHTED
      targets:
        - url: http://localhost:8090
          weight: 1
        - url: http://localhost:8091
          weight: 2
        - url: http://localhost:8092
          weight: 4
      healthCheck:
        enabled: true
        path: /healthz
        interval: 5000
        successThreshold: 1
        failureThreshold: 3
    
    # Example showing pathRewrite feature
    # When a request comes to /api/v1/users, it will be forwarded to targets as /users
    # (the /api/v1 prefix is stripped)
    rewrite:
      path: /api/rewrite/
      pathRewrite: /api/
      targets:
        - url: http://localhost:8090
```

### Request Retries

Retry behavior is configured via environment variables:

```bash
export RETRY_ENABLE=true              # Enable request retries (default: true)
export RETRY_BACKOFF=100              # Initial backoff in milliseconds (default: 100)
export RETRY_COUNT=3                  # Maximum retry attempts (default: 3)
```

The retry handler implements exponential backoff with jitter: `initial_backoff * (2^attempt) + jitter`

- **Retries on**: 5xx errors and connection failures
- **Does NOT retry**: 4xx errors (client responsibility) or successful responses

For detailed architecture information, see [RETRY_HANDLER_ARCHITECTURE.md](RETRY_HANDLER_ARCHITECTURE.md).

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

### Building a Docker Container

For convenience, use the provided `build-container.sh` script to package the application and build the JVM Docker container:

```shell script
./build-container.sh
```

This script will:
1. Clean and package the application with Maven
2. Build the Docker image tagged as `quarkus/loadbalancerr-jvm`

Alternatively, to build the container manually:

```shell script
./mvnw package
docker build -f src/main/docker/Dockerfile.jvm -t quarkus/loadbalancerr-jvm .
```

### Running the containerized load balancer

To run the containerised load balancer (assuming Docker is your container runtime):

```shell script
docker run -i --rm -p 8080:8080 --volume "<path/to/config.yml>:/opt/config/config.yml" quarkus/loadbalancerr-jvm
```

To run with debug port enabled:

```shell script
docker run -i --rm -p 8080:8080 -p 5005:5005 -e JAVA_DEBUG=true -e JAVA_DEBUG_PORT=*:5005 quarkus/loadbalancerr-jvm
```

