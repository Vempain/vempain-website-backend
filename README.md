# Vempain Website backend (Spring Boot)

Complete rewrite of the PHP backend in `../backend` on **Spring Boot 4.1.1** / **Java 25**.

The Gradle wrapper is included, so no system Gradle installation is required. A Java 25 JDK is
required for local builds and runtime. Docker builds use the same Java version.

The module is a drop-in replacement: it listens on the same port (`8000`), serves the same paths (`/api`, `/file`,
`/health`), reads the same environment variables and talks to the same PostgreSQL schema (`vempain_site`). The schema is
owned by the Vempain admin backend and is never modified from here (`spring.jpa.hibernate.ddl-auto: none`).

## The one intentional behavioural difference

The PHP backend could `eval()` PHP fragments embedded in page content
(`backend/src/Application/Service/PageCacheEvaluator.php`, including helper shims from
`backend/legacy/shims/lib`). **This version never evaluates page content.** Page bodies are served from the
publisher-provided `web_site_page.cache` column when it is populated, and from `web_site_page.body` verbatim otherwise.
Everything else - embed parsing, the dynamic
`word_cloud` and `today_random` payload injection, ACL filtering, caching semantics - is reproduced.

## Dependencies

Kept deliberately small; there is no third-party JWT, mapping or utility library:

| Dependency                            | Why                                                                 |
|---------------------------------------|---------------------------------------------------------------------|
| `spring-boot-starter-web`             | REST controllers, JSON, embedded Tomcat                             |
| `spring-boot-starter-data-jpa`        | entity mapping and `JdbcTemplate` for the native PostgreSQL queries |
| `postgresql`                          | JDBC driver                                                         |
| `spring-security-crypto`              | bcrypt verification of the existing password hashes                 |
| `springdoc-openapi-starter-webmvc-ui` | OpenAPI document and Swagger UI                                     |
| `spring-boot-starter-test`            | tests only                                                          |
| `testcontainers-junit-jupiter`        | JUnit 5 integration-test support                                    |
| `testcontainers-postgresql`           | PostgreSQL container support for integration tests                  |

JWT signing and verification (HS256) is implemented directly on the JDK crypto API in
`auth/JwtService`.

> Spring Boot 4 ships **Jackson 3**: databind classes live in `tools.jackson.databind.*`
> while the annotations remain in `com.fasterxml.jackson.annotation.*`.

## Configuration

Every setting is bound to the environment variable the PHP deployment already provides.

| Environment variable             | Property                                            | Default                         |
|----------------------------------|-----------------------------------------------------|---------------------------------|
| `ENV_VEMPAIN_SITE_DB_HOST`       | `spring.datasource.url`                             | `127.0.0.1`                     |
| `ENV_VEMPAIN_SITE_DB_PORT`       | `spring.datasource.url`                             | `5434`                          |
| `ENV_VEMPAIN_SITE_DB_NAME`       | `spring.datasource.url`                             | `vempain_site_db`               |
| `ENV_VEMPAIN_SITE_DB_SCHEMA`     | `spring.datasource.url`, `hibernate.default_schema` | `vempain_site`                  |
| `ENV_VEMPAIN_SITE_DB_USER`       | `spring.datasource.username`                        | `vempain_site`                  |
| `ENV_VEMPAIN_SITE_DB_PASSWORD`   | `spring.datasource.password`                        | -                               |
| `JWT_SECRET`                     | `vempain.site.jwt-secret`                           | required (no fallback)          |
| `JWT_TTL_SECONDS`                | `vempain.site.jwt-ttl-seconds`                      | `1200`                          |
| `COOKIE_SECURE`                  | `vempain.site.jwt-cookie-secure`                    | `true` (local profile: `false`) |
| `VEMPAIN_WEBSITE_WEB_ROOT`       | `vempain.site.files-root`                           | `/files`                        |
| `ENV_VEMPAIN_CORS_ALLOW_ORIGINS` | `vempain.site.cors-allowed-origins`                 | no cross-origin requests        |

## Authentication

Cookie based, mirroring the PHP behaviour:

- `POST /api/login` returns `{"token": "..."}` and sets the `HttpOnly` `OXALATE_JWT_TOKEN` cookie (`Path=/`, `SameSite=Lax`).
- The token is also accepted as `Authorization: Bearer <token>`.
- Every issued token is recorded in `web_site_jwt_token`, so `POST /api/logout` really invalidates it.
- A missing or malformed token leaves the request anonymous (user id `-1`), because most content is public. An
  **expired** token ends the request with
  `401 {"error":"Session expired","code":"SESSION_EXPIRED"}` and clears the cookie.
- Authenticated responses carry a refreshed token in the `X-Auth-Token` header, which the frontend mirrors into local
  storage.

## Running

```bash
# from backend-spring/, after the PostgreSQL service is available on port 5434
./start.sh

# or explicitly (the dev profile enables SQL logging)
./gradlew :service:bootRun
./gradlew :service:bootRun --args='--spring.profiles.active=dev'

# compile, run all tests, and produce the test report
./gradlew clean test
```

`start.sh` only exports local development settings and starts Spring Boot; it does not start
PostgreSQL. The defaults match the repository's local `.env` configuration. For a different
database, export the variables listed in the configuration table before invoking Gradle.

When running locally, the API documentation is available at:

- Swagger UI: http://localhost:8000/swagger-ui/index.html
- OpenAPI document: http://localhost:8000/v3/api-docs

Both endpoints are disabled when the `prod` Spring Boot profile is active.

Container image:

```bash
docker build -t vempain-site-backend-spring backend-spring
```

The image listens on port 8000 and requires the database, JWT, and file-root environment
variables at runtime. To use it instead of the PHP backend in `docker-compose.yml`, change the
`vempain-site-backend.build.context` value to `./backend-spring`, remove the
`./backend:/var/www/backend` bind mount, and add `COOKIE_SECURE` or
`ENV_VEMPAIN_CORS_ALLOW_ORIGINS` if needed. Keep the existing environment block, file/log mounts,
health check, and Traefik labels.

## Layout

```
api/                REST API contracts and shared response DTOs
service/            Spring Boot application and implementation code
  auth/              JWT issuing/verification, cookie handling, current caller
  config/             typed configuration and the CORS filter
  domain/entity/      JPA mappings of the vempain_site tables
  domain/repository/  Spring Data repositories plus the ported native queries
  service/            application logic (ACL, pages, files, galleries, search, embeds)
  web/                REST controller implementations and error handling
```
