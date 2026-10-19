# OWASP Top 10:2025 audit — Vempain website backend

**Audit date:** 2026-09-15  
**Scope:** Spring Boot backend in this repository. The React frontend was not modified; see the
[frontend report](../../vempain-website-frontend/security/OWASP-2025-audit-report.md) when present.

## Phase 0 — assets and trust boundaries

The service listens on port 8000 and exposes `/health`, public page/resource/configuration/embed routes, authenticated
ACL-filtered variants under `/api`, `POST /api/login`, and `POST /api/logout`. The `/file/{*path}` and
`/api/public/files/{*path}` routes stream publisher files. Database access is through PostgreSQL/JPA; file access is
through the configured `VEMPAIN_WEBSITE_WEB_ROOT`; no outbound HTTP, SMTP, S3, XML parser, command execution, or
deserialization sink was found. Sensitive data is password hashes, JWTs, ACL membership, page bodies, file metadata and
private file bytes. Runtime secrets are `JWT_SECRET`, database password and CORS origins. The Docker image and reusable
GitHub Actions workflow are deployment boundaries. Gradle dependency versions are managed by the Spring BOM; there is
no frontend or lockfile in this repository.

Endpoint inventory: `/health` is deliberate liveness; `/api/login` is deliberate anonymous authentication;
`/api/logout` is deliberately idempotent; public page, gallery, file, embed, subject and configuration reads are
deliberately anonymous but ACL-filtered where applicable. Page/file ID, content, directory and raw-file routes enforce
ACL checks. The actuator exposes only the health probe on the separate management port (`8081`, not published by the compose
files); application metrics leave the service by authenticated OTLP push and no metrics endpoint is served. Every request is also written as a structured access
line (client address, user agent, path, status, duration, user) to stdout for the host's log collector; see the 2026-10-19 addition. Swagger/OpenAPI is
available only outside the `prod` profile.

## Phase 1 — threat model

* Anonymous users can read published public content and attempt login; malformed/expired tokens must not grant ACL
  access.
* A low-privilege authenticated user may read only ACL memberships and public data permitted to that user.
* A stolen active JWT can act as its owner until expiry or logout; logout must revoke it server-side.
* PostgreSQL and the mounted file root are trusted service boundaries, but database file paths and filesystem symlinks
  are treated as untrusted publisher data.
* CI/deployment credentials and runtime secrets must not be embedded in the image or source.

## Phase 2 — A01–A10 checklist

| Category                                 | Verdict                       | Evidence / rationale                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
|------------------------------------------|-------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| A01 Broken Access Control                | **FIXED / PASS**              | ACL checks are centralized in `ResourceAccessService`/`AclService`; child pages now use an ownership-scoped query; persisted-token validation precedes global permission and refresh; raw files enforce lexical and real-path containment. Public routes are documented above.                                                                                                                                                                                                                                                           |
| A02 Security Misconfiguration            | **FIXED / MEDIUM REMEDIATED** | JWT fallback removed, secure cookies default on, CORS now fails closed, generic malformed-request errors, browser security headers, and non-root runtime image added. Production Swagger is disabled; the actuator serves only the health probe on the management port.                                                                                                                                                                                                                                                                  |
| A03 Software Supply Chain Failures       | **PASS / INFO**               | Gradle dependencies use pinned project/BOM versions and HTTPS Maven registries. CI uses a floating reusable workflow and no SBOM task is present; add SHA pinning and CycloneDX in delivery policy. No dependency scanner was configured, so transitive freshness is **UNVERIFIED**.                                                                                                                                                                                                                                                     |
| A04 Cryptographic Failures               | **FIXED / PASS**              | HS256 requires configured `JWT_SECRET`; tokens expire and are persisted for revocation; auth cookies are HttpOnly, SameSite=Lax and Secure by default. Secret rotation and minimum entropy remain deployment responsibilities.                                                                                                                                                                                                                                                                                                           |
| A05 Injection                            | **FIXED / PASS**              | All request values are bound parameters (JPA/native). Dynamic SQL structure exists only in `PublishedDataService` (publisher table name, sort column, direction) and is now built from server-side constants: `MusicSortColumn`/`SortDirection` enums and `SqlIdentifiers` quoting of the table name returned by `information_schema`. `LIKE`/`ILIKE` patterns are escaped and length-capped through `LikePatterns` (page search, subject autocomplete, music search). No command, URL, XML or template sink. See the A05 finding below. |
| A06 Insecure Design                      | **FIXED / PASS**              | Authentication lifecycle has server-side revocation and bounded paging/limits. Public content and ACL semantics are intentional migration behavior. Login rate limiting is not implemented (deferred MEDIUM).                                                                                                                                                                                                                                                                                                                            |
| A07 Authentication Failures              | **FIXED / PASS**              | Invalid credentials are generic; expired and revoked tokens do not remain authenticated or refresh. JWT TTL defaults to 1200 seconds. Login throttling/account lockout is **UNVERIFIED/DEFERRED**.                                                                                                                                                                                                                                                                                                                                       |
| A08 Software/Data Integrity Failures     | **PASS / INFO**               | No unsafe object deserialization or unsigned update path was found. CI provenance/SBOM and image digest pinning are not evidenced.                                                                                                                                                                                                                                                                                                                                                                                                       |
| A09 Security Logging & Alerting Failures | **PARTIAL**                   | Login failures and unexpected exceptions are logged; invalid JWTs are debug logged. There is no alerting/rate-limit telemetry, so repeated failure detection is deferred.                                                                                                                                                                                                                                                                                                                                                                |
| A10 Mishandling Exceptions               | **FIXED / PASS**              | Unexpected failures return a generic 500; malformed request details no longer disclose converter/internal messages. ACL failures return controlled 401/403 responses.                                                                                                                                                                                                                                                                                                                                                                    |

## Findings and remediations

### HIGH — revoked/global JWT could bypass ACL or be refreshed

The filter previously attached any correctly signed token and refreshed it without checking the persisted token table;
`ResourceAccessService` also trusted the `global_permission` claim before the ACL service. A logged-out global token
could therefore retain access or receive a new token. The filter now requires `isPersistedAndValid`, and global access is
handled only after that check in `AclService`. Regression coverage: `JwtAuthenticationFilterUTC`,
`AclServiceUTC`, and existing JWT revoke tests.

### HIGH — child-page endpoint disclosed ACL-protected bodies

`PageService.children` used an unscoped `findByParentIdOrderByPublishedAsc` query. It now uses
`findByParentIdForUser` with the same ACL condition as other page listing queries, and the controller passes the current
user id. Regression coverage: `PageServiceUTC` and `PageControllerCTC`.

### HIGH — filesystem symlink escape

Raw file serving previously checked only normalized lexical paths. It now resolves both configured root and target with
`toRealPath()` and requires real-target containment. Regression coverage: `ResourceControllerUTC` symlink test.

### MEDIUM — insecure secret/CORS/cookie defaults and browser hardening

Blank `JWT_SECRET` no longer falls back to `"secret"`; production configuration requires `JWT_SECRET`. CORS no longer
reflects arbitrary origins with credentials when the allow-list is empty or `*`; `COOKIE_SECURE` defaults true.
`SecurityHeadersFilter` adds CSP, frame, MIME, referrer, permissions and HTTPS HSTS headers. Malformed errors are
generic. Docker runtime now runs as non-root. Covered by unit/regression tests and configuration review.

### LOW — dynamic SQL structure and unescaped LIKE patterns (A05, review alert)

A GitHub Copilot review flagged `PublishedDataService` for concatenating request-derived values into SQL. Analysis of the
code path: the data-set identifier was validated with `[a-z][a-z0-9_]*` and checked against `information_schema.tables`
before being interpolated, the sort column had to equal an allow-listed name and the direction was one of two literals,
and every search value was a bind parameter. The concatenation was therefore **not exploitable** for SQL injection, so the
alert is rated LOW (pattern risk), not HIGH. Two genuine A05 checklist gaps were found next to it:

- `LIKE`/`ILIKE` patterns were built from raw request text in `WebSitePageSearchRepositoryImpl` (search terms and path
  prefix), `SubjectController.autocomplete` and the music search, so `%`/`_` acted as wildcards and the pattern length was
  unbounded (CWE-89 adjacent, resource exhaustion).
- The request strings themselves were interpolated after validation instead of a server-side constant chosen by them.

**Fix.** `tools/SqlIdentifiers` validates and quotes identifiers; `PublishedDataService` interpolates only the table name
returned by `information_schema` and enum constants for sort column and direction (the request value selects the enum,
the enum supplies the SQL text). `tools/LikePatterns` escapes `\`, `%`, `_`, truncates to 200 characters, and is used by
every LIKE pattern builder; the number of search terms is capped at 10. PostgreSQL's default `LIKE` escape character (`\\`) is relied upon, which is documented
on the helper. Positive input validation at the boundary is now in place:
`spring-boot-starter-validation` is on the service classpath, `LoginRequest`, `SubjectSearchRequest` and
`SubjectIdSearchRequest` carry `@Size`/`@Min`/`@Max`/`@Positive` constraints, the bodies are `@Valid`, the `search`/`q`
request parameters are capped at 200 characters, and `ApiExceptionHandler` maps constraint violations to a generic
`400 {"error":"Invalid request"}` without echoing the constraint details.

**Proof.** `PublishedDataServiceITC.searchWildcardsAreTreatedAsLiteralText` (a `%` or `_` search now matches only the row
containing that character; fails on the previous code where it matched every row) and
`sortAndDirectionAreServerSideConstantsAndIdentifiersAreValidated` (`artist" desc; drop table ...` as sort, `"; drop table`
as identifier), `WebSitePageSearchRepositoryImplUTC` (escaped bound patterns, no request text in the JPQL, term cap),
`LikePatternsUTC`, `SqlIdentifiersUTC`; validation: `SubjectSearchControllerCTC.constraintViolationsAreRejectedWithTheErrorContract`,
`PageControllerCTC.overlongSearchParameterIsRejected`, `AuthControllerCTC.overlongCredentialsAreRejectedBeforeLookup`.

## Phase 3–4 verification

Targeted security tests passed:

```text
./gradlew :service:test --tests '*JwtServiceUTC' --tests '*AclServiceUTC'
  --tests '*ResourceControllerUTC' --tests '*PageServiceUTC' --tests '*PageControllerCTC'
  --tests '*PublishedDataServiceITC' --tests '*WebSitePageSearchRepositoryImplUTC'
  --tests '*LikePatternsUTC' --tests '*SqlIdentifiersUTC'
```

The full `./gradlew clean test` suite passed. Dynamic probes requiring PostgreSQL and deployed reverse-proxy
configuration were not run locally; those remain deployment checks.

## Accepted/deferred risks and migration parity

Login rate limiting, centralized alerting, dependency/SBOM scanning, CI SHA pinning, image digest pinning and minimum
JWT entropy validation are deferred operational improvements. The Spring backend intentionally does **not** evaluate
page content as executable code. Its `/file`, page rendering, cookie/CORS, and ACL controls require verification
together with the frontend deployment. No frontend code was changed.

## Additions after the audit

### 2026-10-12 — Open Graph metadata endpoint and HTML document

`OpenGraphApi` (`/api/public/open-graph/**`) and `WebSitePageResponse.open_graph` were added for link previews. Access control (A01):
the metadata is resolved for the current caller through `ResourceAccessService`/the `ForUser` repository queries, and a restricted or
missing page, gallery or image degrades to the site level metadata instead of an error, so the endpoint does not reveal whether
restricted content exists; the frontend proxy strips credentials, so crawlers are always anonymous. Injection (A03/A05): the HTML
variant (`tools/OpenGraphHtml`) HTML-escapes every text, percent-encodes the image path per segment and refuses dot segments,
backslashes and control characters; the route is read from the request path and only used for repository lookups with bind
parameters; the `X-Vempain-Origin` header is accepted only as a plain `scheme://host[:port]` value (otherwise the request origin is
used), so it cannot inject markup or a `javascript:` URL. Verification: `OpenGraphServiceUTC`, `OpenGraphHtmlUTC`,
`OpenGraphControllerCTC`, `ResponseContractJTC`.

### 2026-10-19 — Access log, proxy-aware client address and request metrics

`traffic/AccessLogFilter` writes one JSON line per request (logger `vempain.site.access`, stdout) with the client address, user agent,
referer, path, query, status, duration, response size, the login of an authenticated visitor and a per-request id that is also returned as
`X-Request-Id`; `WebsiteMetrics` additionally counts requests by status class, method, address source, bot flag and country. Personal
data (A01/A09): the address, user agent and user are personal data under the GDPR; they are kept only in the log stream, never as metric
labels, the health probe is not logged, and retention is that of the Loki instance of the monitoring environment (30 days); the log can be
switched off with `VEMPAIN_SITE_ACCESS_LOG_ENABLED=false`. Header trust (A05/A07): proxy headers are read only in the configured
`vempain.site.proxy.mode` (`cloudflare`: `CF-Connecting-IP`/`CF-IPCountry`; `forwarded`: `X-Forwarded-For`/`X-Real-IP`; an optional
`trusted-header`); in the default `none` mode every such header is ignored, so a client cannot spoof its address or country into the log
unless the deployment declares a proxy that overwrites those headers. Header values are bounded (first address only, 64 characters, user
agent truncated to 512, two-letter country) and the line is produced by the JSON serializer, so log injection through headers is not
possible. The optional GeoLite2 database is read-only and local; a missing or corrupt file disables the lookup instead of failing the
service. Verification: `ClientRequestResolverUTC`, `GeoIpCountryLookupUTC`, `AccessLogFilterUTC`, `AccessLogFilterCTC`,
`WebsiteMetricsUTC`, `SitePropertiesUTC`.

### 2026-10-19 — Distributed tracing

`spring-boot-starter-opentelemetry` continues an incoming W3C `traceparent` (frontend or Traefik) and, with `VEMPAIN_TRACING_ENABLED=true`,
pushes the request spans over OTLP/HTTP to the Tempo receiver of the monitoring environment, authenticated with the same basic auth
credentials and self-signed certificate as the metrics push (A02/A05: the trace export is off by default and never unauthenticated or
unverified). Trace and span ids are random identifiers, not personal data; a span carries the request path and status but no client
address, user agent or body, and the Tempo retention (7 days by default) is shorter than the Loki one. The ids are added to every log
line and to the access log line (`trace_id`, `span_id`); CORS additionally allows the `traceparent`/`tracestate` request headers and
exposes `X-Request-Id`. A caller can choose its own trace id but cannot read or affect other requests through it (A01). Verification:
`TraceContinuationCTC`, `AccessLogFilterUTC`, `MetricsExportEnvironmentPostProcessorUTC`.
