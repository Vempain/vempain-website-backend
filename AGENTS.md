# vempain-website-backend — Agent Guide

## Scope

- This repository is the Spring Boot/Java implementation of the **public website backend**. It is paired with `vempain-website-frontend`; together they
  serve the published site behind a reverse proxy that routes by path (`/api`, `/file`, `/health`).
- The former PHP backend is discarded and must not be treated as a supported implementation.
- Java and Spring Boot versions are pinned in `gradle/libs.versions.toml` (`java`, `spring-boot`); keep them aligned with the other Vempain backends.

## Layout

| Path                                                | Purpose                                                                                   |
|-----------------------------------------------------|-------------------------------------------------------------------------------------------|
| `api/`                                              | REST interfaces (`*Api.java`) and DTO records under `controller/dto/{request,response}`   |
| `service/.../VempainWebsiteApplication.java`        | Spring Boot entry point                                                                   |
| `service/.../auth/`                                 | HS256 JWT issuing/verification, cookie writer, request filter, current-user provider      |
| `service/.../config/`                               | `SiteProperties` (`vempain.site.*`), CORS filter, security headers filter, OpenAPI config |
| `service/.../controller/`                           | Thin controllers implementing the `api` interfaces, plus `ApiExceptionHandler`            |
| `service/.../entity/` and `service/.../repository/` | JPA mappings of the `vempain_site` tables and Spring Data repositories                    |
| `service/.../service/`                              | Business rules: ACL filtering, pages, published datasets, subject search                  |

## Architecture that matters

- The `vempain_site` schema is **owned by `vempain-admin-backend`**; this service only reads it (`spring.jpa.hibernate.ddl-auto: none`) and never ships
  Flyway migrations. Schema changes start in the admin backend's `db/migration/site` tree.
- Authentication is **cookie based** and self-contained: `POST /api/login` sets the `HttpOnly` JWT cookie, every issued token is persisted in
  `web_site_jwt_token` so logout is effective, and anonymous requests get user id `-1`. This repo intentionally does not depend on `vempain-auth-*`;
  do not introduce those artifacts to replace the local `JwtService`/`AclService`.
- Access control is resource ACL based, like the rest of Vempain: `ResourceAccessService.deniedStatus(aclId)` turns the ACL decision into `401`
  (anonymous) or `403` (authenticated but not listed). Do not add role-based (`hasRole`/`ROLE_*`) checks.
- Page content is never evaluated as code. Bodies are served from `web_site_page.cache` when present, otherwise from `body` verbatim.
- Default ports: API `8000`, management `8081` (`application.yaml`); `start.sh` overrides to `10010`/`10011` for local runs.

## Conventions

- JSON contracts are mandatory snake_case; DTOs are Java records annotated with `@JsonProperty("snake_name")` where the Java name is multi-word.
- Spring Boot 4 ships Jackson 3: databind lives in `tools.jackson.databind.*`, annotations stay in `com.fasterxml.jackson.annotation.*`. Prefer
  `tools.jackson` APIs when a `tools.jackson` equivalent exists.
- Lombok is enabled through the `io.freefair.lombok` plugin. Use `@Getter`/`@Setter` on entities and configuration properties, `@RequiredArgsConstructor`
  for constructor injection, and `@Slf4j` for logging; do not hand-write that boilerplate. Keep explicit constructors only where they carry logic (for example
  `WebSiteJwtToken`).
- Keep controllers thin and place business rules in services.
- Java is tab-indented with a 160-character line limit (`.editorconfig`); do not mass-reformat.

## Testing

- Test suffixes are meaningful and shared across the Vempain Java repos:
    - `*UTC` = unit tests (Mockito, no Spring context)
    - `*CTC` = controller tests (MockMvc or controller slice)
    - `*ITC` = integration tests (Spring Boot context + Testcontainers PostgreSQL; Docker required)
    - `*JTC` = JSON contract tests (serialization naming); none exist here yet, follow the `vempain-auth` examples if you add one
- Run `./gradlew clean test` for backend changes; use `./gradlew :service:test --tests '<Class>'` while iterating. Report the results after every code
  modification.
- Review `README.md`, `application*.yaml`, the `Dockerfile`, and `.github/workflows/ci.yaml` (reusable `spring-boot-service.yaml`) when changing
  deployment behavior.

## Tag ACL rule

Tags are metadata, not ACL-bearing resources. Tag entities have no ACL information, so tag list, search, and mutation endpoints must not perform ACL checks on
tags. ACL checks apply only to resources that explicitly carry an ACL.
