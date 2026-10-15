package fi.poltsi.vempain.website.controller;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import fi.poltsi.vempain.website.api.response.ApiErrorResponse;
import fi.poltsi.vempain.website.api.response.OpenGraphResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST contract for the Open Graph metadata of website routes. The frontend route follows the base path, for example
 * {@code /api/public/open-graph/pages/trips/summer} or {@code /api/public/open-graph/galleries/3}, so that the frontend proxy can forward a crawler's request
 * for {@code /pages/trips/summer} without rewriting the path. Routes the caller may not see, and routes that do not exist, yield the site level metadata
 * instead of an error, because a link preview should never disclose restricted content and should always show at least the site.
 */
@Tag(name = "Open Graph", description = "Open Graph metadata of website routes for link previews")
public interface OpenGraphApi {

    /**
     * Base path for Open Graph endpoints.
     */
    String BASE_PATH = "/api/public/open-graph";

    /**
     * Request header in which the frontend proxy passes the public origin (scheme and host) the site is served from. Only a plain {@code scheme://host[:port]}
     * value is used; anything else falls back to the origin of the request itself.
     */
    String ORIGIN_HEADER = "X-Vempain-Origin";

    /**
     * Returns the Open Graph metadata of a frontend route.
     *
     * @param request the request; the route is the request path after {@link #BASE_PATH}
     * @return metadata of the route, or of the site when the route is unknown or restricted
     */
    @GetMapping(path = BASE_PATH + "/**", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get the Open Graph metadata of a route",
            description = "The frontend route follows the base path. Unknown and restricted routes yield the site level metadata")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Open Graph metadata returned",
                content = @Content(schema = @Schema(implementation = OpenGraphResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE)),
        @ApiResponse(responseCode = "500", description = "Unexpected server error",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE)) })
    OpenGraphResponse openGraph(HttpServletRequest request);

    /**
     * Returns the same metadata as a minimal HTML document whose head carries the Open Graph and Twitter card tags. The frontend proxy serves it to
     * link-preview crawlers, which do not run the single-page application. This is the one endpoint without a typed JSON body, because its consumers only read
     * HTML.
     *
     * @param request the request; the route is the request path after {@link #BASE_PATH}
     * @param origin  public origin of the site set by the proxy, see {@link #ORIGIN_HEADER}
     * @return HTML document with the metadata in its head
     */
    @GetMapping(path = BASE_PATH + "/**", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "Get the Open Graph metadata of a route as an HTML document",
            description = "Served by the frontend proxy to link-preview crawlers. Absolute URLs use the X-Vempain-Origin header when the proxy sets it")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "HTML document with the metadata in its head",
                content = @Content(schema = @Schema(type = "string"), mediaType = MediaType.TEXT_HTML_VALUE)),
        @ApiResponse(responseCode = "500", description = "Unexpected server error",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE)) })
    String openGraphHtml(HttpServletRequest request, @RequestHeader(value = ORIGIN_HEADER, required = false) String origin);
}
