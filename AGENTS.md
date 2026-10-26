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
- Every published resource carries the audit trail the admin backend writes into the site schema: `creator`, `created`,
  `modifier`, `modified`, the admin users named by nick like `web_site_page` (never by admin user ID). The columns were added to
  `web_site_file`, `web_gps_location`, `web_site_subject` and `web_site_configuration` by the admin backend's site migration
  `V1004__site_audit_columns.sql`, and `V1005__gallery_audit_nicks.sql` turned the gallery's creator/modifier from admin user IDs into
  nicks; they are nullable, because rows published before those releases carry NULL until they are published again. The entities map
  them read-only and `WebSiteFileResponse`, `WebSiteGalleryResponse`, `LocationResponse` and `SubjectResponse` expose them (`ResponseContractJTC`,
  `vempain-website-frontend/src/models/WebSiteFile|WebSiteGallery|WebSiteLocation|WebSiteSubject.ts`); the public
  configuration stays a key/value map. Tests: `WebSiteEntitiesUTC`, `ResponseMapperServiceUTC`, `SubjectLookupServiceUTC`.

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

## Metrics

- Metrics are pushed, never scraped: `micrometer-registry-otlp` sends every meter to the Prometheus OTLP receiver of the monitoring host.
  `metrics/MetricsExportEnvironmentPostProcessor` (registered in `META-INF/spring.factories`) maps the deployment settings
  `vempain.metrics.enabled|url|username|password|certificate|step` (the `VEMPAIN_METRICS_*` environment) to the Boot OTLP properties:
  basic auth header, export step and, for the self-signed endpoint, an SSL bundle built from the PEM certificate. It is a deliberate local
  copy of the class the admin and file backends get from `vempain-common`, for the same reason the auth code is local.
- The management port serves only `health`; do not add `prometheus` or other endpoints to the exposure list.
- Tracing (`spring-boot-starter-opentelemetry`): `vempain.tracing.enabled|url|sampling` (`VEMPAIN_TRACING_*`) are mapped by the same
  post-processor to `management.tracing.enabled`, `management.tracing.sampling.probability` and
  `management.opentelemetry.tracing.export.otlp.endpoint|headers.Authorization|ssl.bundle`, reusing the metrics credentials and
  certificate (Tempo sits behind the same basic auth). `application.yaml` keeps `management.tracing.enabled: ${vempain.tracing.enabled}`
  so that nothing is traced or exported by default. Incoming `traceparent` is continued (W3C and B3 consumed, W3C produced); Boot puts
  `traceId`/`spanId` into the MDC and the console pattern. `TraceContinuationCTC` wires the real OpenTelemetry bridge through the
  observation filter and proves the continuation, the MDC and the access line.
- Event meters live in `metrics/WebsiteMetrics` (counters, timer, summary; fixed tag values, the page path is bounded to existing pages
  with `unknown` as the fallback) and the database backed gauges in `metrics/WebsiteGauges` (cached for a minute, never a query per
  export). Controllers call the meters and stay thin; new meters are named `vempain.site.<area>.<thing>` and get a test with a
  `SimpleMeterRegistry`.

## Traffic (access log)

- `traffic/AccessLogFilter` is the first filter of the chain (`Ordered.HIGHEST_PRECEDENCE + 5`, before CORS and the JWT filter) so that
  every outcome is logged, including 404s and the 401 of an expired token. It writes one JSON object per request through the logger
  `vempain.site.access`, which has its own `%msg%n` console appender in `logback-spring.xml` (additivity off), sets `X-Request-Id`, skips
  `/health`, and feeds `WebsiteMetrics.accessRequest`/`accessCountry`. The field names (`ts`, `type`, `method`, `path`, `query`, `status`,
  `duration_ms`, `bytes`, `client_ip`, `client_source`, `country`, `user_agent`, `referer`, `bot`, `user`, `request_id`, `trace_id`,
  `span_id`) are a contract
  with the Alloy pipeline and the Grafana dashboard in `vempain-cluster`; change them there too.
- `traffic/ClientRequestResolver` finds the real client: `vempain.site.proxy.mode` (`none|cloudflare|forwarded`, `ProxyMode`) decides which
  header is trusted, and only that header is read; `none` never reads a proxy header. The country comes from `CF-IPCountry` in Cloudflare
  mode, else from `traffic/GeoIpCountryLookup` (optional GeoLite2-Country database, `vempain.site.geoip.database`; never throws, private
  addresses are not looked up), else `unknown`. The user agent is truncated to 512 characters and classified with the `BOT_MARKERS` list.
- The user is read from the request attribute `CurrentUserProvider.REQUEST_ATTRIBUTE`, not from `CurrentUserProvider`, because the
  request context is already gone when the chain returns to the first filter. For the same reason the trace and span ids are read from
  the observation context that `ServerHttpObservationFilter` leaves on the request (its server span is already closed), with the
  current span as the fallback.
- Addresses and user agents are personal data: keep them in the log line only, never as metric tags (`vempain.site.access.*` carry only
  status class, method, source, bot flag and country).

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
- In the `prod` profile `config/DatabaseTlsVerification` refuses to start unless `spring.datasource.url` requires TLS (`sslmode=require`,
  `verify-ca` or `verify-full`, or `ssl` without an `sslmode`); the `vempain-cluster` website stack connects with `sslmode=require` and its
  database rejects plaintext connections.
- Preserve the repository's Java formatting: 4-space indentation and 160-character lines (`.editorconfig`, `.config/eclipse-formatter.xml`).
- Test suffixes are meaningful: `UTC`, `CTC`, `ITC`, and `JTC` (JSON contract tests in `api/`).

## Validation

Run `./gradlew clean test` for backend changes. Use the focused service or API
test task first when iterating. Review `README.md`, `application*.yaml`,
Dockerfiles, and CI configuration when changing deployment behavior.

## Container image

- `Dockerfile` is runtime-only (the CI-built `service/build/libs/vempain-website-backend.jar` is copied in) and starts
  `container-entrypoint.sh` as root, which gives the image's `vempain` account the ids `VEMPAIN_UID`/`VEMPAIN_GID` (default `6666`,
  `ENV_VEMPAIN_UID`/`ENV_VEMPAIN_GID` in `vempain-cluster/website/.env`) and runs Java through `su-exec` as those ids; a container started
  as a non-root user keeps that user. The ids must be able to read the published files mounted at `/files` and to write the log directory
  `/var/log/vempain`. The user handling is identical to the admin and file backend entrypoints (which additionally build a truststore).

## Tag ACL rule

Tags are metadata, not ACL-bearing resources. Tag entities have no ACL information, so tag list, search, and mutation endpoints must not perform ACL checks on tags. ACL checks apply only to resources that explicitly carry an ACL.
