# vempain-website-backend — Agent Guide

## Scope

This repository is the current Spring Boot/Java 25 implementation of the
public website backend. The former PHP backend is discarded and must not be
treated as a supported implementation.

## Layout

- `api/`: REST interfaces (`controller/*Api.java`) and the request/response DTOs (`api/request`, `api/response`). Published to GitHub
  Packages as `vempain-website-backend-api`.
- `service/`: Spring Boot application, controllers, security, services,
  repositories, migrations, and tests.
- Public runtime paths include `/api`, `/file`, and `/health`.

## REST contracts (DTOs)

- Every endpoint declares a typed body: a `*Response` class (or `PagedResponse<*Response>` /
  `List<*Response>`) and a `*Request` class for JSON bodies. Never return `Object`, `Map` or
  hand-built maps from a controller or service; the only `Map` body is the public configuration,
  which is a genuine key/value set (`Record<string, string>` on the frontend).
- DTOs follow the admin/file backend style: Lombok `@Data @Builder @NoArgsConstructor
  @AllArgsConstructor`, `@JsonIgnoreProperties(ignoreUnknown = true)`,
  `@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)` (Jackson 3, `tools.jackson`),
  and `@Schema` on the class and on every field. API interfaces document each response code with
  `@ApiResponse(content = @Content(schema = @Schema(implementation = ...)))`; errors are always
  `ApiErrorResponse` (`error`, optional `code`).
- Field names must match the TypeScript models in `vempain-website-frontend/src/models` and
  `vempain-rt-renderer/src/types.ts` one to one (`WebSitePageResponse` <-> `WebSitePage`, and so
  on). `api/src/test/.../api/ResponseContractJTC` pins the serialised key set of every DTO; update it
  together with the frontend model when a field is added.
- Entities are mapped to DTOs in `service/ResponseMapperService` (pages, files, galleries,
  locations, embeds) with bulk subject/location lookups; GPS locations are only resolved for
  authenticated callers. Published data sets are mapped in `PublishedDataService`.

## Conventions

- Preserve snake_case JSON contracts (see REST contracts above).
- Lombok (`io.freefair.lombok`) is enabled in both modules; prefer its annotations for
  constructors (`@RequiredArgsConstructor`), accessors, builders and logging (`@Slf4j`) unless
  they obscure behavior or conflict with Spring. Entities are read-only JPA mappings and keep
  their explicit getters.
- Keep controllers thin and place business rules in services.
- SQL/JPQL text is built from constants only; every request value is a bind parameter. When an identifier must be
  interpolated (publisher `website_data__*` tables) quote it with `tools/SqlIdentifiers` using the name the database
  returned, and let request values select an enum constant (`PublishedDataService.MusicSortColumn`, `SortDirection`)
  rather than appending the request string. Build every `LIKE` pattern with `tools/LikePatterns` (escapes `%`, `_`,
  `\`, caps the length) and cap the number of search terms. See `security/OWASP-2025-audit-report.md` (A05).
- Preserve the repository's tab-indented Java formatting.
- Test suffixes are meaningful: `UTC`, `CTC`, `ITC`, and `JTC` (JSON contract tests in `api/`).

## Validation

Run `./gradlew clean test` for backend changes. Use the focused service or API
test task first when iterating. Review `README.md`, `application*.yaml`,
Dockerfiles, and CI configuration when changing deployment behavior.

## Tag ACL rule

Tags are metadata, not ACL-bearing resources. Tag entities have no ACL information, so tag list, search, and mutation endpoints must not perform ACL checks on tags. ACL checks apply only to resources that explicitly carry an ACL.
