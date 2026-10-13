---
name: cookie-auth-and-visibility
description: Work on the website backend's self-contained cookie authentication and the ACL based visibility of published pages, files and galleries. Use for login, session, token refresh or content visibility changes.
---

# Authentication and visibility in vempain-website-backend

- This backend deliberately has no `vempain-auth-*` dependency: `service/.../auth/` (`JwtService`, `JwtAuthenticationFilter`,
  `AuthCookieWriter`, `CurrentUserProvider`, `PasswordVerifier`) issues an `HttpOnly` session cookie and mirrors refreshed tokens in the
  `X-Auth-Token` header. Do not add bearer-token login or the shared library; the frontend's local `AbstractAPI` (`withCredentials`) is
  the counterpart.
- The service reads the `vempain_site` schema that the admin backend owns and migrates; stay read-only towards those tables. Entities are
  read-only JPA mappings with explicit getters.
- Visibility of pages, files and galleries follows the published site ACL rows for the cookie user; there are no roles. Listings filter
  with those rows, and single-item lookups a caller may not see behave like missing items.
- Open Graph (`OpenGraphService`, `OpenGraphApi` at `/api/public/open-graph/**`): metadata for a route or page, HTML variant for crawlers,
  absolute URLs from `X-Vempain-Origin` or the request origin, every text HTML-escaped, image paths percent-encoded and refused when they
  contain dot segments, backslashes or control characters. A hidden or unknown route yields site-level metadata and never throws.
- In the `prod` profile `config/DatabaseTlsVerification` refuses to start unless the JDBC URL requires TLS; keep it when changing datasource
  configuration.
- Tests cover a visible and a hidden case for every visibility rule; record changes in `security/OWASP-2025-audit-report.md`.
