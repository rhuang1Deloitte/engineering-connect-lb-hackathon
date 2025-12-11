# Copilot Instructions: LoadBalanceRR

## Project Overview
**LoadBalanceRR** is a high-performance load balancer built with **Quarkus 3.30.2** and **Java 21**. 

## Requirements

Include REQUIREMENTS.md

## Architecture

### Package Structure
```
com.hackathon.lb
├── algorithm/      # Load balancing algorithms (RR, least-conn, etc.)
├── config/         # Quarkus configuration and bootstrapping
├── feature/        # Optional feature toggles/extensions
├── handler/        # Custom request/response handling
```

The architecture suggests a **pluggable algorithm pattern** where load balancing strategies are independently implemented and injectable via Arc CDI.

### Build & Development
- **Build Tool**: Maven with Maven Wrapper (`./mvnw`)
- **Java Version**: 21 (enforced in `pom.xml` via `maven.compiler.release`)
- **Quarkus**: Native image support enabled via `-Dnative` profile
- **Compilation**: Parameters enabled (`maven.compiler.parameters=true`) - required for reflection/annotation processing

**Key Build Profiles**:
- **JVM (default)**: `./mvnw package` → `target/quarkus-app/quarkus-run.jar`
- **Uber JAR**: `./mvnw package -Dquarkus.package.jar.type=uber-jar`
- **Native**: `./mvnw package -Dnative` (requires GraalVM) or with container build: `-Dquarkus.native.container-build=true`

## Critical Workflows

### Development
```bash
./mvnw quarkus:dev          # Live reload, Dev UI at http://localhost:8080/q/dev
```
Quarkus' Dev UI is critical for observing hot-reload behavior and debugging Arc CDI injection issues.

### Building for Production
```bash
./mvnw clean package                                    # JVM JAR
./mvnw clean package -Dquarkus.package.jar.type=uber-jar  # Single JAR
./mvnw clean package -Dnative -Dquarkus.native.container-build=true  # Native (recommended for performance)
```

### Docker Deployment
Four pre-configured Dockerfiles available in `src/main/docker/`:
- **Dockerfile.jvm**: Multi-layer JVM build (optimized for Docker layer caching)
- **Dockerfile.legacy-jar**: Legacy JAR format (uses `run-java.sh` script)
- **Dockerfile.native**: Full UBI9 native image (~400MB)
- **Dockerfile.native-micro**: Quarkus micro image native build (~100MB) - **preferred**

All expect native build output to be pre-built before `docker build` execution.

### Testing
```bash
./mvnw test              # Unit tests (surefire)
./mvnw verify            # Integration tests (failsafe) - requires `skipITs=false`
```
**Test Config Note**: Surefire/Failsafe configured with `--add-opens java.base/java.lang=ALL-UNNAMED` for reflection; native profile disables `skipITs`.

## Project Conventions & Patterns

### New Features

Use Test-Driven Development (TDD):

1. Write Quarkus tests in `src/test/java/` for new features.
2. Implement feature in `src/main/java/`.
3. Run `./mvnw test` and `./mvnw verify` to ensure correctness.

### Quarkus-Specific
1. **Arc CDI over Spring**: No Spring dependency. Use `@ApplicationScoped`, `@Singleton`, `@Dependent` from `jakarta.inject`.
2. **Configuration**: Load from `application.properties` in `src/main/resources/` (currently empty - define environment-specific configs here).
3. **Build-Time Optimization**: GraalVM reflection config auto-generated via `quarkus-maven-plugin` with `native-image-agent` goal.

### Request Handling
- **Stub Server**: `stub/reflect.py` is a Python test utility (echoes HTTP requests) - use for integration testing load balancer redirect/proxy behavior.

### Algorithm Integration
The `algorithm/` package contains interfaces like `LoadBalancingStrategy` with implementations:
- Round-Robin (RR)
- Least connections
- Weighted distribution
- Others

**Pattern**: Inject strategy via `@Inject` in handlers; make strategies `@ApplicationScoped` for shared state.

### Code Quality Expectations
- **Reflection-Heavy Code**: Compiler parameters enabled (`-parameters`) to preserve method/constructor parameter names for reflection APIs.
- **Native Image Compatibility**: Avoid reflection on unknown classes; use `@RegisterForReflection` if needed.
- **Logging**: Uses JBoss LogManager (configured in test surefire/failsafe) - use `java.util.logging` API.

## Key Dependencies
- **io.quarkus:quarkus-arc**: CDI DI container (only runtime dependency)
- **io.quarkus:quarkus-junit5**: Test framework
- **No Web/HTTP Framework**: Custom implementation required

## Integration Points
1. **Request Handler Design**: Implement custom HTTP server (likely using Netty or direct socket handling).
2. **Backend Service Discovery**: Load balancer must forward requests to upstream services - define configuration for backend pool management.
3. **Health Checks**: Consider Quarkus health extension (`quarkus-smallrye-health`) if not already in custom handler.

## Deployment Notes
- **Quarkus HTTP Port**: Defaults to 8080. Override with `-Dquarkus.http.host=0.0.0.0 -Dquarkus.http.port=8080` (pre-configured in Dockerfiles).
- **Native Image Startup**: <100ms vs ~500ms JVM mode - critical for auto-scaling environments.
- **Memory**: Native images use ~40MB RSS vs 200MB+ JVM - measure actual metrics post-build.

## Debugging Tips
- **Dev Mode Issues**: Check Dev UI logs and use `QUARKUS_LOG_LEVEL=DEBUG` environment variable.
- **Native Image Build Failures**: Common with reflection - check `target/native-image-build-output.log` and add `-H:+PrintClassInitialization` for initialization order issues.
- **Arc Injection Failures**: Verify `@Inject` targets are `@ApplicationScoped` or have matching scope; use `quarkus:dev` with Dev UI to inspect registered beans.
