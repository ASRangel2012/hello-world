# hello-world-service

Production-grade "hello world" microservice built with **Spring Boot 4.0.5** on **Java 25 (LTS)**, using **Gradle** (Kotlin DSL). Small in scope, but structured the way a real service should be: layered architecture, versioned REST API, externalized configuration, observability, resilience, security, and a full path to Kubernetes.

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
- **Observability** — Actuator health/liveness/readiness probes (readiness includes the DB check), Prometheus metrics at `/actuator/prometheus`, `@Timed` business metrics (`greeting.resolve`, `greeting.list`), ECS-format JSON structured logs in `prod`, and a `CorrelationIdFilter` that propagates `X-Correlation-Id` through MDC into every log line. In `prod` all actuator endpoints move to a dedicated management port (8081) that the ingress never routes.
- **Security** — stateless HTTP Basic via Spring Security; public: hello endpoint, health, info, Prometheus scrape, API docs (dev only). Everything else authenticated. Hardened response headers: HSTS (with `forward-headers-strategy` behind the TLS-terminating ingress), CSP, Referrer-Policy, Permissions-Policy on top of the framework defaults (nosniff, frame options, cache control). Inbound `X-Correlation-Id` values are allow-list validated to prevent log injection. Swap `httpBasic` for `oauth2ResourceServer` when an IdP is available.
- **Schema management** — Flyway owns the schema (`db/migration`); Hibernate only validates.
- **Caching** — greeting templates are static reference data; `findByLocale` is cached with Caffeine (bounded, 10 min TTL) via `@Cacheable`.

## Prerequisites

- JDK 25 (Temurin recommended)
- Gradle 9.5+ (or generate the wrapper once with `gradle wrapper` and commit it)
- Docker (for docker-compose and Testcontainers-based integration tests)
- kubectl + a cluster (deployment only)

## Local development

```bash
# Run with the dev profile (default): in-memory H2, credentials dev/dev
gradle bootRun
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

### Full local stack (PostgreSQL)

```bash
docker compose up --build
# app on :8080 (prod profile), actuator on :8081, basic auth admin/admin-local-only
```

## Build & test

```bash
gradle build            # unit tests + Testcontainers ITs (integrationTest) + JaCoCo + boot JAR
gradle test             # unit tests only (no Docker needed)
gradle integrationTest  # *IT tests only (requires Docker)
gradle bootJar          # executable layered JAR in build/libs/
docker build -t hello-world-service:local .
```

Unit tests run without Docker; the `*IT` integration tests need a Docker daemon (spins up `postgres:17-alpine`).

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

1. **build-test** — Gradle wrapper validation, then `gradle build` on Temurin 25 / Gradle 9.5.1 (unit + integration tests, JaCoCo), then SonarQube analysis (`SONAR_TOKEN`/`SONAR_HOST_URL`).
2. **docker** — multi-stage layered-JAR image built locally, gated by a Trivy scan (fails on CRITICAL/HIGH with fixes available), then pushed to GHCR (`:sha` + `:latest`) and signed with cosign (keyless, GitHub OIDC), main branch only.
3. **deploy** — pins the immutable image SHA into the manifest, then applies `k8s/` manifests (single rollout); gated by the `production` environment.

Required secrets: `SONAR_TOKEN` (optional), `KUBE_CONFIG` (base64 kubeconfig). `GITHUB_TOKEN` covers GHCR.

Dependency updates are automated via Dependabot (`.github/dependabot.yml`): weekly PRs for Gradle dependencies (Spring artifacts grouped), GitHub Actions and Docker base images.

## Deployment (Kubernetes)

```bash
# One-time: namespace + config + real secret (see k8s/secret.template.yaml)
kubectl apply -f k8s/configmap.yaml
kubectl create secret generic hello-world-service-secrets -n hello-world \
  --from-literal=DB_USERNAME=... --from-literal=DB_PASSWORD=... \
  --from-literal=API_USERNAME=... --from-literal=API_PASSWORD=...

kubectl apply -f k8s/deployment.yaml -f k8s/service.yaml -f k8s/ingress.yaml \
  -f k8s/pdb.yaml -f k8s/hpa.yaml -f k8s/networkpolicy.yaml
kubectl rollout status deployment/hello-world-service -n hello-world
```

The container runs as non-root with a read-only root filesystem; probes hit `/actuator/health/liveness` and `/actuator/health/readiness` on the management port (8081); rolling updates keep `maxUnavailable: 0` and a PodDisruptionBudget keeps at least one replica during voluntary disruptions. An HPA scales 2–6 replicas on CPU (70 %), and a NetworkPolicy restricts traffic to the ingress controller (8080), monitoring (8081), DNS and PostgreSQL.

## Project layout

```
src/main/java/com/example/helloworld/
├── HelloWorldApplication.java      # @SpringBootApplication + @EnableResilientMethods
├── api/v1/GreetingController.java  # versioned REST API
├── api/dto/                        # wire DTOs (entities never leave the service layer)
├── api/error/GlobalExceptionHandler.java
├── client/                         # QuoteClient port + simulated/http adapters (@Retryable)
├── config/                         # Security, OpenAPI, properties, Clock, Cache, Metrics
├── domain/Greeting.java            # JPA entity
├── exception/
├── repository/GreetingRepository.java
└── web/CorrelationIdFilter.java    # MDC request tracing
```
