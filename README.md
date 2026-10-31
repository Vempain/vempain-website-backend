# Vempain Website backend (Spring Boot)

Current public website backend on **Spring Boot 4.1.1** / **Java 25**. The
former PHP backend was discarded and is not a supported runtime.

The Gradle wrapper is included, so no system Gradle installation is required. A Java 25 JDK is
required for local builds and runtime. Docker builds use the same Java version.

The module is a drop-in replacement: it listens on the same port (`8000`), serves the same paths (`/api`, `/file`,
`/health`), reads the same environment variables and talks to the same PostgreSQL schema (`vempain_site`). The schema is
owned by the Vempain admin backend and is never modified from here (`spring.jpa.hibernate.ddl-auto: none`).

## Content safety

Page content is never evaluated as executable code. Page bodies are served from the publisher-provided
`web_site_page.cache` column when it is populated, and from `web_site_page.body` verbatim otherwise.
Everything else - embed parsing, the dynamic
`word_cloud` and `today_random` payload injection, ACL filtering, caching semantics - is reproduced.

## Dependencies

Kept deliberately small; there is no third-party JWT, mapping or utility library:

|              Dependency               |                                 Why                                 |
|---------------------------------------|---------------------------------------------------------------------|
| `spring-boot-starter-web`             | REST controllers, JSON, embedded Tomcat                             |
| `spring-boot-starter-data-jpa`        | entity mapping and `JdbcTemplate` for the native PostgreSQL queries |
| `postgresql`                          | JDBC driver                                                         |
| `spring-security-crypto`              | bcrypt verification of the existing password hashes                 |
| `springdoc-openapi-starter-webmvc-ui` | OpenAPI document and Swagger UI                                     |
| `spring-boot-starter-actuator`        | health probe on the management port and the Micrometer meters       |
| `micrometer-registry-otlp`            | OTLP/HTTP push of the meters to the monitoring host                 |
| `spring-boot-starter-test`            | tests only                                                          |
| `testcontainers-junit-jupiter`        | JUnit 5 integration-test support                                    |
| `testcontainers-postgresql`           | PostgreSQL container support for integration tests                  |

JWT signing and verification (HS256) is implemented directly on the JDK crypto API in
`auth/JwtService`.

> Spring Boot 4 ships **Jackson 3**: databind classes live in `tools.jackson.databind.*`
> while the annotations remain in `com.fasterxml.jackson.annotation.*`.

## Configuration

Every setting is bound to the environment variables used by the current Vempain deployment.

|          Environment variable           |                      Property                       |               Default               |
|-----------------------------------------|-----------------------------------------------------|-------------------------------------|
| `ENV_VEMPAIN_SITE_DB_HOST`              | `spring.datasource.url`                             | `127.0.0.1`                         |
| `ENV_VEMPAIN_SITE_DB_PORT`              | `spring.datasource.url`                             | `5434`                              |
| `ENV_VEMPAIN_SITE_DB_NAME`              | `spring.datasource.url`                             | `vempain_site_db`                   |
| `ENV_VEMPAIN_SITE_DB_SCHEMA`            | `spring.datasource.url`, `hibernate.default_schema` | `vempain_site`                      |
| `ENV_VEMPAIN_SITE_DB_USER`              | `spring.datasource.username`                        | `vempain_site`                      |
| `ENV_VEMPAIN_SITE_DB_PASSWORD`          | `spring.datasource.password`                        | -                                   |
| `JWT_SECRET`                            | `vempain.site.jwt-secret`                           | required (no fallback)              |
| `JWT_TTL_SECONDS`                       | `vempain.site.jwt-ttl-seconds`                      | `1200`                              |
| `COOKIE_SECURE`                         | `vempain.site.jwt-cookie-secure`                    | `true` (local profile: `false`)     |
| `VEMPAIN_WEBSITE_WEB_ROOT`              | `vempain.site.files-root`                           | `/files`                            |
| `ENV_VEMPAIN_CORS_ALLOW_ORIGINS`        | `vempain.site.cors-allowed-origins`                 | no cross-origin requests            |
| `VEMPAIN_METRICS_ENABLED`               | `vempain.metrics.enabled`                           | `false`                             |
| `VEMPAIN_METRICS_URL`                   | `vempain.metrics.url`                               | - (OTLP/HTTP metrics endpoint)      |
| `VEMPAIN_METRICS_USERNAME`              | `vempain.metrics.username`                          | - (basic auth of the endpoint)      |
| `VEMPAIN_METRICS_PASSWORD`              | `vempain.metrics.password`                          | -                                   |
| `VEMPAIN_METRICS_CERTIFICATE`           | `vempain.metrics.certificate`                       | - (PEM to trust, self-signed)       |
| `VEMPAIN_METRICS_STEP`                  | `vempain.metrics.step`                              | `1m`                                |
| `VEMPAIN_METRICS_ROLE`                  | `management.metrics.tags.role`                      | `api`                               |
| `VEMPAIN_TRACING_ENABLED`               | `vempain.tracing.enabled`                           | `false`                             |
| `VEMPAIN_TRACING_URL`                   | `vempain.tracing.url`                               | - (OTLP/HTTP traces endpoint)       |
| `VEMPAIN_TRACING_SAMPLING`              | `vempain.tracing.sampling`                          | `1.0` (0..1 of traces kept)         |
| `VEMPAIN_SITE_PROXY_MODE`               | `vempain.site.proxy.mode`                           | `none` (`cloudflare`, `forwarded`)  |
| `VEMPAIN_SITE_PROXY_TRUSTED_HEADER`     | `vempain.site.proxy.trusted-header`                 | - (the mode's default header)       |
| `VEMPAIN_SITE_GEOIP_DATABASE`           | `vempain.site.geoip.database`                       | - (GeoLite2-City `.mmdb`)           |
| `VEMPAIN_SITE_ACCESS_LOG_ENABLED`       | `vempain.site.access-log.enabled`                   | `true`                              |
| `VEMPAIN_SITE_FORWARD_HEADERS_STRATEGY` | `server.forward-headers-strategy`                   | `none` (`framework` behind a proxy) |

## Metrics

The backend pushes its metrics over OTLP/HTTP to the Prometheus receiver of the monitoring host (`vempain-cluster/monitoring`); nothing
is scraped and the management port (`8081`) serves only the health probe. Set `VEMPAIN_METRICS_ENABLED=true`, the endpoint URL
(`https://grafana.intra.poltsi.fi/prometheus/api/v1/otlp/v1/metrics`), the basic auth credentials and, because the endpoint uses a
self-signed certificate, the path of the PEM certificate to trust (mounted read-only into the container). Every meter carries the
`application`, `role` and `instance` tags.

Besides the Spring Boot meters (JVM, HTTP server requests, Hikari pool `site`, Tomcat, Logback, repository invocations) the site records:

- `vempain.site.resource.requests{result=served|not_found|denied}` and `vempain.site.resource.bytes` for `/file/...`
- `vempain.site.page.views{path}` for page reads (`unknown` for a missing page, so the tag set stays bounded)
- `vempain.site.logins{result=success|invalid_credentials|bad_request}`, `vempain.site.tokens.active`, `vempain.site.tokens.revoked`
- `vempain.site.search.duration` and `vempain.site.search.results` for subject searches
- `vempain.site.embed.requests{type,result}` and `vempain.site.opengraph.requests{format,result}`
- `vempain.site.last_publish.age.seconds`, the age of the newest published page (NaN before the first publish)

## Tracing

With `VEMPAIN_TRACING_ENABLED=true` the backend takes part in distributed tracing (Spring Boot `spring-boot-starter-opentelemetry`,
Micrometer Tracing with the OpenTelemetry bridge). An incoming W3C `traceparent` header, sent by the website frontend's API client or
injected by Traefik, is continued: the request runs in that trace, every log line of the request carries the trace and span ids (Spring Boot's console pattern
becomes `... --- [vempain-website-backend] [thread] [vempain-website-backend,<traceId>,<spanId>] logger : message`),
the access log line carries them as `trace_id` and `span_id`, and the spans are pushed over OTLP/HTTP to the Tempo receiver of the
monitoring host (`VEMPAIN_TRACING_URL`, for example `https://grafana.intra.poltsi.fi/tempo/v1/traces`) with the same basic auth
credentials and certificate as the metrics (`VEMPAIN_METRICS_USERNAME`, `VEMPAIN_METRICS_PASSWORD`, `VEMPAIN_METRICS_CERTIFICATE`).
`VEMPAIN_TRACING_SAMPLING` is the fraction of new traces that are kept (a trace started by the caller keeps the caller's decision).
CORS allows the `traceparent` and `tracestate` request headers and exposes `X-Request-Id`. Tracing off (the default) leaves the log
pattern and the access line as before, with null trace fields.

## Traffic: access log and client resolution

Every request (except the `/health` probe) ends with one JSON line on stdout through the logger `vempain.site.access`, and every response
carries an `X-Request-Id` header with the same id as the line. The host's log collector (`vempain-cluster/website`, Grafana Alloy) ships the
lines to Loki, where the request detail that must not become a metric label lives: client address, user agent, path, query, referer, user.
The fields are `ts`, `type` (`access`), `method`, `path`, `query`, `status`, `duration_ms`, `bytes`, `client_ip`, `client_source`
(`socket|cloudflare|forwarded`), `country`, `user_agent`, `referer`, `bot`, `user`, `request_id`, `trace_id` and `span_id` (the
last two null without tracing). The low-cardinality part is also
pushed as metrics: `vempain.site.access.requests{status_class,method,client_source,bot}`, `vempain.site.access.duration{status_class}`
and `vempain.site.access.country{country}`, so request rates, 4xx/5xx error rates and latency are available in Prometheus.

The real client address depends on what runs in front of the service. `VEMPAIN_SITE_PROXY_MODE=none` (the default) takes the socket
address and ignores every proxy header, because a client can send them itself. `cloudflare` reads `CF-Connecting-IP` and the country from
`CF-IPCountry`; `forwarded` reads the first address of `X-Forwarded-For`, then `X-Real-IP`. `VEMPAIN_SITE_PROXY_TRUSTED_HEADER` overrides
the header of the mode. Behind a proxy also set `VEMPAIN_SITE_FORWARD_HEADERS_STRATEGY=framework` so that generated URLs use the
forwarded scheme and host. Without Cloudflare the location comes from an optional MaxMind GeoLite2-City database (`VEMPAIN_SITE_GEOIP_DATABASE`, mounted
read-only; the database is not shipped, it needs a MaxMind account and licence) and is `unknown`
otherwise. Private and loopback addresses are never looked up. `VEMPAIN_SITE_ACCESS_LOG_ENABLED=false` switches the whole filter off.

## Authentication

Cookie-based authentication:

- `POST /api/login` returns `{"token": "..."}` and sets the `HttpOnly` `OXALATE_JWT_TOKEN` cookie (`Path=/`, `SameSite=Lax`).
- The token is also accepted as `Authorization: Bearer <token>`.
- Every issued token is recorded in `web_site_jwt_token`, so `POST /api/logout` really invalidates it.
- A missing or malformed token leaves the request anonymous (user id `-1`), because most content is public. An
  **expired** token ends the request with
  `401 {"error":"Session expired","code":"SESSION_EXPIRED"}` and clears the cookie.
- Authenticated responses carry a refreshed token in the `X-Auth-Token` header, which the frontend mirrors into local
  storage.

## Open Graph and link previews

Single page responses (`GET /api/public/page-content`, `GET /api/pages/{id}`) carry `open_graph`: the type (`article`), site name,
title, description (header or a summary of the body), canonical frontend path, the first image of the page the caller may see (`image_path` below `/file`, with
MIME type and size) and the publish/modify times. The frontend sets its `<head>` tags from it.

`GET /api/public/open-graph/<frontend route>` returns the same metadata for any route (`/pages/trips/summer`, `/galleries/3`, ...),
as JSON or, with `Accept: text/html`, as a small HTML document whose head carries the Open Graph and Twitter card tags. The frontend's
nginx proxies link-preview crawlers (which do not run the single-page application) to the HTML variant and passes the public origin in
`X-Vempain-Origin` (`scheme://host[:port]`) for the absolute URLs. Restricted and unknown routes yield the site level metadata (name and
description from the `site.name`/`site.description` configuration) rather than an error.

## Running

```bash
# from backend-spring/, after the PostgreSQL service is available on port 5434
./start.sh

# or explicitly (the local profile enables development logging)
./gradlew :service:bootRun
./gradlew :service:bootRun --args='--spring.profiles.active=local'

# compile, run all tests, and produce the test report
./gradlew clean test
```

`start.sh` only exports local development settings and starts Spring Boot; it does not start
PostgreSQL. The defaults match the repository's local `.env` configuration. For a different
database, export the variables listed in the configuration table before invoking Gradle.

When running locally with `start.sh`, the API listens on port `10010` and the
management server listens on port `10011`. The API documentation is available at:

- Swagger UI: http://localhost:10010/swagger-ui/index.html
- OpenAPI document: http://localhost:10010/v3/api-docs

Both endpoints are disabled when the `prod` Spring Boot profile is active.

Container image:

The `Dockerfile` is runtime-only, like the admin and file backend images: it copies the
pre-built `service/build/libs/vempain-website-backend.jar` onto `eclipse-temurin:25-jre-alpine`
and compiles nothing itself. CI builds the jar with `./gradlew clean bootJar` in the shared
`spring-boot-service` workflow before running `docker build`; locally do the same:

```bash
./gradlew bootJar
docker build -t vempain-website-backend .
```

The image listens on port 8000 and requires the database, JWT, and file-root environment
variables at runtime. It starts as root and runs the service as `VEMPAIN_UID:VEMPAIN_GID` (default `6666:6666`), so set those to the host
user that owns the mounted files and log directory; a container started with a fixed non-root `--user` keeps that user. Deployment wiring belongs to the current
compose/Swarm configuration
in `vempain-cluster`, which pulls the published `ghcr.io` image.

## Layout

```
api/                REST API contracts (controller/*Api.java) and the request/response DTOs
service/            Spring Boot application and implementation code
  auth/              JWT issuing/verification, cookie handling, current caller
  config/            typed configuration, CORS filter, security headers, OpenAPI
  controller/        REST controller implementations
  entity/            JPA mappings of the vempain_site tables
  repository/        Spring Data repositories plus the ported native queries
  service/           application logic (ACL, pages, files, galleries, search, embeds, DTO mapping)
  exception/         ApiException and the ApiErrorResponse controller advice
```

Every JSON body is a snake_case DTO from the `api` module that mirrors the matching model in
`vempain-website-frontend/src/models`; the OpenAPI document lists the schema of every endpoint.
