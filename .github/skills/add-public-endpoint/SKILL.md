---
name: add-public-endpoint
description: Add or change a public REST endpoint of the website backend with typed DTOs that mirror the website frontend and renderer models (ResponseContractJTC). Use for new endpoints, response fields or query parameters.
---

# Add a public endpoint to vempain-website-backend

1. Contract in `api/`: `controller/<Name>Api.java` plus `api/request`/`api/response` DTOs. Every endpoint returns a typed `*Response`,
   `PagedResponse<*Response>` or `List<*Response>`; never `Object`, `Map` or hand-built maps (the public configuration key/value set is the
   only exception). Errors are `ApiErrorResponse`.
2. DTO style: Lombok `@Data @Builder @NoArgsConstructor @AllArgsConstructor`, `@JsonIgnoreProperties(ignoreUnknown = true)`,
   `@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)` (Jackson 3, `tools.jackson`), `@Schema` on class and fields,
   `@ApiResponse(content = @Content(schema = ...))` per status code.
3. Field names match `vempain-website-frontend/src/models` and `vempain-rt-renderer/src/types.ts` one to one. Update
   `api/src/test/.../api/ResponseContractJTC` (pins the key set of every DTO) and the frontend model in the same change.
4. Implementation in `service/`: thin controller, rules in a service, mapping in `ResponseMapperService` (bulk subject/location lookups;
   GPS locations only for authenticated callers) or `PublishedDataService` for data sets.
5. Input: Jakarta validation on DTOs and `@Size` on free-text query parameters; `ApiExceptionHandler` answers `400` without echoing input.
   SQL/JPQL from constants only, request values as bind parameters, identifiers through `tools/SqlIdentifiers`, `LIKE` through
   `tools/LikePatterns`, sort columns chosen by enum constants.
6. Visibility follows the site ACL rows of the cookie user (see `cookie-auth-and-visibility`); never reveal restricted content through a
   difference in behaviour (Open Graph answers site-level metadata for unknown or hidden routes).
7. Tests: `*UTC` for services, `*CTC` for the controller, `*ITC` with the published schema, `*JTC` in `api/`. Run `./gradlew clean test`.
