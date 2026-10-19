package fi.poltsi.vempain.website.traffic;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import fi.poltsi.vempain.website.auth.AuthCookieWriter;
import fi.poltsi.vempain.website.auth.JwtAuthenticationFilter;
import fi.poltsi.vempain.website.auth.JwtService;
import fi.poltsi.vempain.website.config.SiteProperties;
import fi.poltsi.vempain.website.controller.HealthController;
import fi.poltsi.vempain.website.exception.TokenExpiredException;
import fi.poltsi.vempain.website.metrics.WebsiteMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Through the servlet stack every outcome is logged: a 200, an unmatched path (404) and a request the authentication filter rejects (401); the health probe is
 * not.
 */
class AccessLogFilterCTC {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ListAppender<ILoggingEvent> lines = new ListAppender<>();
    private Logger accessLogger;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        accessLogger = (Logger) LoggerFactory.getLogger(AccessLogFilter.LOGGER_NAME);
        lines.start();
        accessLogger.addAppender(lines);

        var properties = new SiteProperties();
        var resolver = new ClientRequestResolver(properties, new GeoIpCountryLookup(address -> Optional.empty()));
        var accessLog = new AccessLogFilter(properties, resolver, new WebsiteMetrics(new SimpleMeterRegistry()), objectMapper, java.time.Clock.systemUTC());
        var jwt = mock(JwtService.class);
        var cookies = mock(AuthCookieWriter.class);
        when(cookies.readToken(any()))
            .thenAnswer(invocation -> ((jakarta.servlet.http.HttpServletRequest) invocation.getArgument(0)).getHeader("X-Test-Token"));
        when(jwt.verify("expired")).thenThrow(new TokenExpiredException("expired"));
        when(jwt.verify(null)).thenReturn(Optional.empty());
        // The access log is first in the chain, the authentication filter after it, as in the application
        mockMvc = MockMvcBuilders.standaloneSetup(new HealthController())
            .addFilters(accessLog, new JwtAuthenticationFilter(jwt, cookies, objectMapper))
            .build();
    }

    @AfterEach
    void tearDown() {
        accessLogger.detachAppender(lines);
    }

    private Map<String, Object> line(int index) {
        return objectMapper.readValue(lines.list.get(index)
            .getFormattedMessage(), Map.class);
    }

    @Test
    void everyOutcomeExceptTheHealthProbeIsLoggedWithARequestId() throws Exception {
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk())
            .andExpect(header().doesNotExist(AccessLogFilter.REQUEST_ID_HEADER));
        mockMvc.perform(get("/api/does-not-exist").header("User-Agent", "Mozilla/5.0"))
            .andExpect(status().isNotFound())
            .andExpect(header().exists(AccessLogFilter.REQUEST_ID_HEADER));
        mockMvc.perform(get("/api/pages").header("X-Test-Token", "expired"))
            .andExpect(status().isUnauthorized())
            .andExpect(header().exists(AccessLogFilter.REQUEST_ID_HEADER));

        assertEquals(2, lines.list.size(), "the health probe wrote no line");
        assertEquals(404, line(0).get("status"));
        assertEquals("/api/does-not-exist", line(0).get("path"));
        assertEquals(false, line(0).get("bot"));
        assertEquals(401, line(1).get("status"));
        assertEquals("/api/pages", line(1).get("path"));
        assertTrue(line(1).get("bytes") instanceof Number number
                   && number.longValue() > 0,
                "the JSON error body was counted");
    }
}
