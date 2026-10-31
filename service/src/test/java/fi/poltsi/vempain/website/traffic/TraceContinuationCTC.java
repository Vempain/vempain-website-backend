package fi.poltsi.vempain.website.traffic;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.ServerHttpObservationFilter;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import fi.poltsi.vempain.website.config.SiteProperties;
import fi.poltsi.vempain.website.metrics.WebsiteMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingReceiverTracingObservationHandler;
import io.micrometer.tracing.otel.bridge.EventPublishingContextWrapper;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.OtelPropagator;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.micrometer.tracing.otel.bridge.Slf4JEventListener;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * An incoming W3C {@code traceparent} is continued through the servlet stack the way Spring Boot wires it (observation filter, Micrometer Tracing with the
 * OpenTelemetry bridge, W3C propagation, the SLF4J MDC listener): the controller runs inside that trace, its log lines carry the trace id in the MDC, and the
 * access log line reports the same trace id. Without the header a new trace is started.
 */
class TraceContinuationCTC {

    private static final String TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final Slf4JEventListener MDC_LISTENER = new Slf4JEventListener();

    static {
        // Boot registers the same wrapper so that scope changes update the MDC; it must be added before the OpenTelemetry context storage is first used
        ContextStorage.addWrapper(new EventPublishingContextWrapper(MDC_LISTENER::onEvent));
    }

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ListAppender<ILoggingEvent> lines = new ListAppender<>();
    private final AtomicReference<String> traceInController = new AtomicReference<>();
    private final AtomicReference<String> mdcInController = new AtomicReference<>();
    private Logger accessLogger;
    private MockMvc mockMvc;
    private Tracer tracer;

    @RestController
    class TracedController {

        @GetMapping("/api/traced")
        Map<String, String> traced() {
            var span = tracer.currentSpan();
            traceInController.set(span == null ? null
                    : span.context()
                        .traceId());
            mdcInController.set(MDC.get("traceId"));
            return Map.of("ok", "true");
        }
    }

    @BeforeEach
    void setUp() {
        accessLogger = (Logger) LoggerFactory.getLogger(AccessLogFilter.LOGGER_NAME);
        lines.start();
        accessLogger.addAppender(lines);

        var sdk = OpenTelemetrySdk.builder()
            .setTracerProvider(SdkTracerProvider.builder()
                .setSampler(Sampler.alwaysOn())
                .build())
            .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
            .build();
        var otelTracer = sdk.getTracer("vempain-website-backend-test");
        tracer = new OtelTracer(otelTracer, new OtelCurrentTraceContext(), MDC_LISTENER::onEvent);
        var propagator = new OtelPropagator(sdk.getPropagators(), otelTracer);
        var observations = ObservationRegistry.create();
        observations.observationConfig()
            .observationHandler(new ObservationHandler.FirstMatchingCompositeObservationHandler(
                    new PropagatingReceiverTracingObservationHandler<>(tracer, propagator), new DefaultTracingObservationHandler(tracer)));

        var properties = new SiteProperties();
        var resolver = new ClientRequestResolver(properties, new GeoIpCountryLookup((GeoIpCountryLookup.CountrySource) address -> Optional.empty()));
        var accessLog = new AccessLogFilter(properties, resolver, new WebsiteMetrics(new SimpleMeterRegistry()), objectMapper, java.time.Clock.systemUTC(),
                tracer);
        // The order of the application: the access log first, then Boot's server observation filter which opens the server span
        mockMvc = MockMvcBuilders.standaloneSetup(new TracedController())
            .addFilters(accessLog, new ServerHttpObservationFilter(observations))
            .build();
    }

    @AfterEach
    void tearDown() {
        accessLogger.detachAppender(lines);
    }

    private Map<String, Object> onlyLine() {
        assertEquals(1, lines.list.size());
        return objectMapper.readValue(lines.list.getFirst()
            .getFormattedMessage(), new TypeReference<Map<String, Object>>() {
            });
    }

    @Test
    void anIncomingTraceparentIsContinuedIntoTheControllerTheMdcAndTheAccessLine() throws Exception {
        mockMvc.perform(get("/api/traced").header("traceparent", "00-" + TRACE_ID + "-00f067aa0ba902b7-01"))
            .andExpect(status().isOk());

        assertEquals(TRACE_ID, traceInController.get(), "the controller runs in the browser's trace");
        assertEquals(TRACE_ID, mdcInController.get(), "log lines of the request carry the trace id");
        var line = onlyLine();
        assertEquals(TRACE_ID, line.get("trace_id"));
        assertNotNull(line.get("span_id"));
        assertNotEquals("00f067aa0ba902b7", line.get("span_id"), "the server span is a child of the browser's span");
    }

    @Test
    void withoutATraceparentANewTraceIsStarted() throws Exception {
        mockMvc.perform(get("/api/traced"))
            .andExpect(status().isOk());

        assertNotNull(traceInController.get());
        assertEquals(traceInController.get(), onlyLine().get("trace_id"));
    }
}
