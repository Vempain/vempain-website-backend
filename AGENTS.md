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
  which is a genuine key/value set (`Record<string, string>` on the frontend). The only non-JSON body
  is the `text/html` variant of `OpenGraphApi` (see Open Graph below), consumed by link-preview crawlers.
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

## Open Graph

- `service/OpenGraphService` derives the Open Graph metadata of a frontend route (`forRoute`: `/pages/<path>`, `/galleries/<id>`,
  `/galleries`, `/search`, anything else maps to `/pages/index`) or of a loaded page (`forPage`): title, description (page header, else
  a 200-character summary of the rendered body, else the site description), canonical path, and the first image the caller may see from
  the page's embeds (`image`, `hero` image/carousel, `gallery`; the stored embed list first, the body's embed comments as fallback) or
  the gallery's first image file. Site name and description come from the `site.name`/`site.description` configuration rows. A route
  the caller may not see, or that does not exist, yields the site level metadata with the route as path; it never throws, because a
  link preview must not reveal whether restricted content exists.
- `PageService.page` attaches it as `WebSitePageResponse.open_graph` (null in listings and `children`); `OpenGraphResponse` mirrors
  `vempain-website-frontend/src/models/OpenGraph.ts` and is pinned by `ResponseContractJTC`.
- `OpenGraphApi` (`/api/public/open-graph/**`, the frontend route after the base path) serves the same metadata as JSON, and as a
  minimal HTML document (`Accept: text/html`, rendered by `tools/OpenGraphHtml`) for the frontend's nginx crawler proxy. Absolute URLs use
  the `X-Vempain-Origin` header when it is a plain `scheme://host[:port]`, otherwise the request's own origin; every text is HTML-escaped
  and the image path is percent-encoded per segment and refused when it contains dot segments, backslashes or control characters.

## Authentication and authorization

- Authentication is cookie based and self-contained: `service/.../auth/` (`JwtService`, `JwtAuthenticationFilter`, `AuthCookieWriter`,
  `CurrentUserProvider`, `PasswordVerifier`) issues an `HttpOnly` session cookie and mirrors refreshed tokens in the `X-Auth-Token` header.
  This backend deliberately has no `vempain-auth-*` dependency; it is the documented exception to the shared-auth rule of the admin and file
  backends. Do not introduce bearer-token login or the shared auth library here.
- The service reads the published `vempain_site` schema that the admin backend owns and migrates; it must stay read-only towards those
  tables. Visibility of pages, files and galleries follows the published site ACL rows for the cookie user; there are no roles.

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
- Request input is validated with Jakarta Validation (`spring-boot-starter-validation`): constraints on the request DTOs (`@Size`, `@Min`/`@Max`,
  `@Positive`, bounded `List` sizes), `@Valid` on request bodies and `@Size` on free-text query parameters. `ApiExceptionHandler` maps
  `MethodArgumentNotValidException`/`HandlerMethodValidationException` to `400` with a generic `ApiErrorResponse` and never echoes the input.
- Preserve the repository's tab-indented Java formatting.
- Test suffixes are meaningful: `UTC`, `CTC`, `ITC`, and `JTC` (JSON contract tests in `api/`).

## Validation

Run `./gradlew clean test` for backend changes. Use the focused service or API
test task first when iterating. Review `README.md`, `application*.yaml`,
Dockerfiles, and CI configuration when changing deployment behavior.

## Tag ACL rule

Tags are metadata, not ACL-bearing resources. Tag entities have no ACL information, so tag list, search, and mutation endpoints must not perform ACL checks on tags. ACL checks apply only to resources that explicitly carry an ACL.
