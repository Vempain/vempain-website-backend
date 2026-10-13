package fi.poltsi.vempain.website.controller;

import fi.poltsi.vempain.website.api.response.OpenGraphResponse;
import fi.poltsi.vempain.website.service.OpenGraphService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Route extraction when the backend is served under a servlet context path, which MockMvc's standalone setup does not exercise.
 */
class OpenGraphControllerUTC {
	private final OpenGraphService    service    = mock(OpenGraphService.class);
	private final OpenGraphController controller = new OpenGraphController(service);

	@Test
	void contextPathIsStrippedBeforeTheRouteIsDecoded() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/site/api/public/open-graph/pages/trips/summer%20holiday");
		request.setContextPath("/site");
		OpenGraphResponse metadata = OpenGraphResponse.builder()
													  .title("Summer")
													  .path("/pages/trips/summer holiday")
													  .build();
		when(service.forRoute("/pages/trips/summer holiday")).thenReturn(metadata);

		assertEquals(metadata, controller.openGraph(request));
	}

	@Test
	void requestOutsideTheBasePathMapsToTheSiteRoute() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/site/elsewhere");
		request.setContextPath("/site");
		when(service.forRoute("")).thenReturn(OpenGraphResponse.builder()
															   .title("Site")
															   .path("/pages/index")
															   .build());

		assertEquals("Site", controller.openGraph(request)
									   .getTitle());
		verify(service).forRoute("");
	}

	@Test
	void htmlVariantUsesTheRequestOriginWithoutTheProxyHeader() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/public/open-graph/galleries/3");
		request.setScheme("https");
		request.setServerName("site.example");
		request.setServerPort(8443);
		when(service.forRoute("/galleries/3")).thenReturn(OpenGraphResponse.builder()
																		   .type("website")
																		   .siteName("Site")
																		   .title("Gallery")
																		   .path("/galleries/3")
																		   .build());

		String html = controller.openGraphHtml(request, null);

		assertTrue(html.contains("<meta property=\"og:url\" content=\"https://site.example:8443/galleries/3\">"));
	}
}
