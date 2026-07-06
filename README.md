# hello-world-service

Production-grade "hello world" microservice built with **Spring Boot 4.0.5** on **Java 25 (LTS)**. Small in scope, but structured the way a real service should be: layered architecture, versioned REST API, externalized configuration, observability, resilience, security, and a full path to Kubernetes.

## Architecture

```
api/v1  →  service  →  repository  →  PostgreSQL / H2
   │           │
   │           └── client (QuoteClient: simulated or HTTP, with retry + timeout)
   └── api/error (RFC 9457 Problem Details via @RestControllerAdvice)
```

Key decisions:

- **Layered architecture** — controller (`api/v1`), service, repository, domain are strictly separated; DTOs (`api/dto`) keep entities off the wire for the main endpoint.
- **API versioning** — URI-based (`/api/v1/...`).
- **Consistent errors** — every error is an RFC 9457 `application/problem+json` payload with a `correlationId`.
- **Resilience** — Spring Framework 7 core resilience: `@Retryable` with exponential back-off + jitter on the external quote call; connect/read timeouts via `spring.http.client.*`. The quote is decorative, so the service degrades gracefully to a fallback instead of failing the request.
- **Observability** — Actuator health/liveness/readiness probes, Prometheus metrics at `/actuator/prometheus`, ECS-format JSON structured logs in `prod`, and a `CorrelationIdFilter` that propagates `X-Correlation-Id` through MDC into every log line.
- **Security** — stateless HTTP Basic via Spring Security; public: hello endpoint, health, info, API docs (dev only). Everything else authenticated. Swap `httpBasic` for `oauth2ResourceServer` when an IdP is available.
- **Schema management** — Flyway owns the schema (`db/migration`); Hibernate only validates.

## Prerequisites

- JDK 25 (Temurin recommended)
- Maven 3.9+ (or use `mvnw` if you add the wrapper: `mvn wrapper:wrapper`)
- Docker (for docker-compose and Testcontainers-based integration tests)
- kubectl + a cluster (deployment only)

## Local development

```bash
# Run with the dev profile (default): in-memory H2, credentials dev/dev
mvn spring-boot:run
```

Useful URLs (dev):

| URL | What |
|---|---|
| http://localhost:8080/api/v1/greetings/hello?name=Ada&locale=es | Public hello endpoint |
| http://localhost:8080/api/v1/greetings | Templates list (basic auth `dev`/`dev`) |
| http://localhost:8080/swagger-ui.html | OpenAPI UI |
| http://localhost:8080/actuator/health | Health |
| http://localhost:8080/actuator/prometheus | Metrics |

Example response:

```json
{
  "message": "¡Hola, Ada!",
  "quoteOfTheDay": "Hope is not a strategy.",
  "timestamp": "2026-07-06T12:00:00.000Z",
  "version": "1.0.0"
}
```

### Full local stack (PostgreSQL + Redis)

```bash
docker compose up --build
# app on :8080 (prod profile), basic auth admin/admin-local-only
```

## Build & test

```bash
mvn verify          # unit tests (Surefire) + Testcontainers ITs (Failsafe) + JaCoCo report
mvn package         # executable layered JAR in target/
docker build -t hello-world-service:local .
```

Unit tests run without Docker; the `*IT` integration test needs a Docker daemon (spins up `postgres:17-alpine`).

## API

Versioned under `/api/v1`. Full OpenAPI 3 spec at `/v3/api-docs`, UI at `/swagger-ui.html` (disabled in `prod`).

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/greetings/hello?name=&locale=` | none | Localized greeting + quote + timestamp + version |
| GET | `/api/v1/greetings` | basic | All greeting templates |

Errors follow RFC 9457, e.g. `GET /api/v1/greetings/hello?locale=xx`:

```json
{
  "type": "https://api.example.com/problems/not-found",
  "title": "Resource not found",
  "status": 404,
  "detail": "No greeting configured for locale 'xx'",
  "timestamp": "2026-07-06T12:00:00Z",
  "correlationId": "…"
}
```

## Configuration profiles

| Profile | Database | Logging | Docs | Credentials |
|---|---|---|---|---|
| `dev` (default) | H2 in-memory | plain text, DEBUG app logs | enabled | `dev`/`dev` |
| `test` | Testcontainers PostgreSQL | plain text | enabled | `test`/`test` |
| `prod` | PostgreSQL via `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` | ECS JSON | disabled | `API_USERNAME`/`API_PASSWORD` |

## CI/CD

`.github/workflows/ci.yml`:

1. **build-test** — `mvn verify` on Temurin 25 (unit + integration tests, JaCoCo), then SonarQube analysis (`SONAR_TOKEN`/`SONAR_HOST_URL`).
2. **docker** — multi-stage layered-JAR image built and pushed to GHCR (`:sha` + `:latest`), main branch only.
3. **deploy** — applies `k8s/` manifests and pins the deployment to the new image SHA; gated by the `production` environment.

Required secrets: `SONAR_TOKEN` (optional), `KUBE_CONFIG` (base64 kubeconfig). `GITHUB_TOKEN` covers GHCR.

## Deployment (Kubernetes)

```bash
# One-time: namespace + config + real secret (see k8s/secret.template.yaml)
kubectl apply -f k8s/configmap.yaml
kubectl create secret generic hello-world-service-secrets -n hello-world \
  --from-literal=DB_USERNAME=... --from-literal=DB_PASSWORD=... \
  --from-literal=API_USERNAME=... --from-literal=API_PASSWORD=...

kubectl apply -f k8s/deployment.yaml -f k8s/service.yaml -f k8s/ingress.yaml
kubectl rollout status deployment/hello-world-service -n hello-world
```

The container runs as non-root with a read-only root filesystem; probes hit `/actuator/health/liveness` and `/actuator/health/readiness`; rolling updates keep `maxUnavailable: 0`.

## Project layout

```
src/main/java/com/example/helloworld/
├── HelloWorldApplication.java      # @SpringBootApplication + @EnableResilientMethods
├── api/v1/GreetingController.java  # versioned REST API
├── api/dto/GreetingResponse.java
├── api/error/GlobalExceptionHandler.java
├── client/                         # QuoteClient port + simulated/http adapters (@Retryable)
├── config/                         # Security, OpenAPI, properties, Clock
├── domain/Greeting.java            # JPA entity
├── exception/
├── repository/GreetingRepository.java
└── web/CorrelationIdFilter.java    # MDC request tracing
```
