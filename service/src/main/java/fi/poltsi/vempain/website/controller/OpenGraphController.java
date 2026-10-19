package fi.poltsi.vempain.website.controller;

import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import fi.poltsi.vempain.website.api.response.OpenGraphResponse;
import fi.poltsi.vempain.website.metrics.WebsiteMetrics;
import fi.poltsi.vempain.website.service.OpenGraphService;
import fi.poltsi.vempain.website.tools.OpenGraphHtml;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class OpenGraphController implements OpenGraphApi {

    private final OpenGraphService service;
    private final WebsiteMetrics metrics;

    public OpenGraphResponse openGraph(HttpServletRequest request) {
        return metadata("json", request);
    }

    public String openGraphHtml(HttpServletRequest request, @RequestHeader(value = ORIGIN_HEADER, required = false) String origin) {
        String trusted = OpenGraphHtml.validOrigin(origin);
        return OpenGraphHtml.render(metadata("html", request), trusted != null ? trusted : requestOrigin(request));
    }

    /**
     * The metadata of the requested route, counted by the format a crawler or the frontend asked for.
     */
    private OpenGraphResponse metadata(String format, HttpServletRequest request) {
        try {
            OpenGraphResponse response = service.forRoute(route(request));
            metrics.openGraphRequest(format, WebsiteMetrics.RESULT_OK);
            return response;
        } catch (RuntimeException e) {
            metrics.openGraphRequest(format, WebsiteMetrics.RESULT_ERROR);
            throw e;
        }
    }

    /**
     * The frontend route after the base path, percent-decoded.
     */
    private static String route(HttpServletRequest request) {
        String path = request.getRequestURI();
        String context = request.getContextPath();
        if (context != null
            && !context.isEmpty()
            && path.startsWith(context)) {
            path = path.substring(context.length());
        }
        String route = path.startsWith(BASE_PATH) ? path.substring(BASE_PATH.length()) : "";
        return UriUtils.decode(route, StandardCharsets.UTF_8);
    }

    private static String requestOrigin(HttpServletRequest request) {
        return ServletUriComponentsBuilder.fromRequestUri(request)
            .replacePath(null)
            .replaceQuery(null)
            .build()
            .toUriString();
    }
}
