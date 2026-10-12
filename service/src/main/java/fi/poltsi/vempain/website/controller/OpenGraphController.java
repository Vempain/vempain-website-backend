package fi.poltsi.vempain.website.controller;

import fi.poltsi.vempain.website.api.response.OpenGraphResponse;
import fi.poltsi.vempain.website.service.OpenGraphService;
import fi.poltsi.vempain.website.tools.OpenGraphHtml;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
public class OpenGraphController implements OpenGraphApi {
	private final OpenGraphService service;

	public OpenGraphResponse openGraph(HttpServletRequest request) {
		return service.forRoute(route(request));
	}

	public String openGraphHtml(HttpServletRequest request, @RequestHeader(value = ORIGIN_HEADER, required = false) String origin) {
		String trusted = OpenGraphHtml.validOrigin(origin);
		return OpenGraphHtml.render(service.forRoute(route(request)), trusted != null ? trusted : requestOrigin(request));
	}

	/**
	 * The frontend route after the base path, percent-decoded.
	 */
	private static String route(HttpServletRequest request) {
		String path    = request.getRequestURI();
		String context = request.getContextPath();
		if (context != null && !context.isEmpty() && path.startsWith(context)) {
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
