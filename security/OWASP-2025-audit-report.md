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
ACL checks. No actuator endpoint is included. Swagger/OpenAPI is available only outside the `prod` profile.

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
| A02 Security Misconfiguration            | **FIXED / MEDIUM REMEDIATED** | JWT fallback removed, secure cookies default on, CORS now fails closed, generic malformed-request errors, browser security headers, and non-root runtime image added. Production Swagger is disabled; no actuator is present.                                                                                                                                                                                                                                                                                                            |
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
