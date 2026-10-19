package fi.poltsi.vempain.website.controller;

import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import fi.poltsi.vempain.website.api.response.OpenGraphResponse;
import fi.poltsi.vempain.website.exception.ApiExceptionHandler;
import fi.poltsi.vempain.website.metrics.WebsiteMetrics;
import fi.poltsi.vempain.website.service.OpenGraphService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OpenGraphControllerCTC {

    @Mock
    OpenGraphService service;
    private MockMvc mvc;
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final WebsiteMetrics metrics = new WebsiteMetrics(registry);

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new OpenGraphController(service, metrics))
            .setControllerAdvice(new ApiExceptionHandler())
            .build();
    }

    @Test
    void jsonVariantDecodesTheRouteAndReturnsTheMetadata() throws Exception {
        when(service.forRoute("/pages/trips/summer holiday")).thenReturn(OpenGraphResponse.builder()
            .type("article")
            .siteName("Site")
            .title("Summer")
            .path("/pages/trips/summer holiday")
            .build());

        // A URI keeps the percent-encoding as a crawler or browser sends it; the String overload would encode it once more.
        mvc.perform(get(URI.create("/api/public/open-graph/pages/trips/summer%20holiday")).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(content().json("""
                    {"type":"article","site_name":"Site","title":"Summer","path":"/pages/trips/summer holiday","image_path":null}
                    """));
    }

    @Test
    void htmlVariantUsesTheProxyOriginWhenItIsValid() throws Exception {
        when(service.forRoute("/galleries/3")).thenReturn(OpenGraphResponse.builder()
            .type("website")
            .siteName("Site")
            .title("Summer")
            .path("/galleries/3")
            .imagePath("s/1.png")
            .imageMimetype("image/png")
            .build());

        mvc.perform(get("/api/public/open-graph/galleries/3").accept(MediaType.TEXT_HTML)
            .header(OpenGraphApi.ORIGIN_HEADER, "https://site.example"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
            .andExpect(content().string(containsString("<meta property=\"og:url\" content=\"https://site.example/galleries/3\">")))
            .andExpect(content().string(containsString("<meta property=\"og:image\" content=\"https://site.example/file/s/1.png\">")))
            .andExpect(content().string(containsString("<title>Summer</title>")));
    }

    @Test
    void htmlVariantFallsBackToTheRequestOriginWhenTheHeaderIsNotAPlainOrigin() throws Exception {
        when(service.forRoute("")).thenReturn(OpenGraphResponse.builder()
            .type("website")
            .siteName("Site")
            .title("Site")
            .path("/pages/index")
            .build());

        mvc.perform(get("/api/public/open-graph").accept(MediaType.TEXT_HTML)
            .header(OpenGraphApi.ORIGIN_HEADER, "javascript:alert(1)//"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("<meta property=\"og:url\" content=\"http://localhost/pages/index\">")))
            .andExpect(content().string(not(containsString("javascript:"))));
    }
}
