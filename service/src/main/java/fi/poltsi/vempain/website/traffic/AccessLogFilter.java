package fi.poltsi.vempain.website.traffic;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.filter.ServerHttpObservationFilter;

import fi.poltsi.vempain.website.auth.AuthenticatedUser;
import fi.poltsi.vempain.website.auth.CurrentUserProvider;
import fi.poltsi.vempain.website.config.SiteProperties;
import fi.poltsi.vempain.website.metrics.WebsiteMetrics;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.TracingObservationHandler;
import tools.jackson.databind.ObjectMapper;

/**
 * Writes one JSON line per completed request through the logger {@value #LOGGER_NAME} (its own stdout appender in {@code logback-spring.xml}, so the line is
 * the whole log record and the host's log collector can parse it) and counts the request in the access meters. The filter is the first of the chain, so
 * rejected, unauthenticated and unmatched (404) requests are logged too. Every response carries an {@code X-Request-Id}. The health probe is not logged. When
 * the service traces, the line also carries the {@code trace_id} and {@code span_id} of the request (the trace started by the browser or Traefik and continued
 * here through the {@code traceparent} header), which is what joins the access line, the application log lines of the request and the spans in Tempo.
 * <p>
 * The client address, user agent and path are personal data: they are kept only in the log, never as metric tags, and the log retention is that of Loki.
 */
@Component
@Order(AccessLogFilter.ORDER)
public class AccessLogFilter extends OncePerRequestFilter {

    public static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 5;
    public static final String LOGGER_NAME = "vempain.site.access";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    static final String HEALTH_PATH = "/health";
    static final String TYPE = "access";

    private static final Logger ACCESS_LOG = LoggerFactory.getLogger(LOGGER_NAME);
    private static final Set<String> METHODS = Set.of("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

    private final boolean enabled;
    private final ClientRequestResolver resolver;
    private final WebsiteMetrics metrics;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    /**
     * The tracer of the service, null when tracing is off: the trace and span ids of the current request go into the line.
     */
    private final Tracer tracer;

    @Autowired
    public AccessLogFilter(SiteProperties siteProperties, ClientRequestResolver resolver, WebsiteMetrics metrics, ObjectMapper objectMapper,
            ObjectProvider<Tracer> tracer) {
        this(siteProperties, resolver, metrics, objectMapper, Clock.systemUTC(), tracer.getIfAvailable());
    }

    AccessLogFilter(SiteProperties siteProperties, ClientRequestResolver resolver, WebsiteMetrics metrics, ObjectMapper objectMapper, Clock clock) {
        this(siteProperties, resolver, metrics, objectMapper, clock, null);
    }

    AccessLogFilter(SiteProperties siteProperties, ClientRequestResolver resolver, WebsiteMetrics metrics, ObjectMapper objectMapper, Clock clock,
            Tracer tracer) {
        this.enabled = siteProperties.getAccessLog()
            .isEnabled();
        this.resolver = resolver;
        this.metrics = metrics;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.tracer = tracer;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled
               || HEALTH_PATH.equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var requestId = UUID.randomUUID()
            .toString();
        response.setHeader(REQUEST_ID_HEADER, requestId);
        var counting = new CountingResponse(response);
        var started = clock.instant();
        var startedNanos = System.nanoTime();
        var failed = false;

        try {
            filterChain.doFilter(request, counting);
        } catch (IOException | ServletException | RuntimeException e) {
            failed = true;
            throw e;
        } finally {
            var durationNanos = System.nanoTime() - startedNanos;
            var status = failed
                         && !response.isCommitted() ? HttpServletResponse.SC_INTERNAL_SERVER_ERROR : response.getStatus();
            record(request, counting, status, started, durationNanos, requestId, currentTrace(request));
        }
    }

    /**
     * The trace and span ids of the request. The server span is opened by Spring's observation filter, which runs after this one, and is closed again before
     * the chain unwinds back here, so the ids are read from the observation context that filter leaves on the request; the current span is the fallback when
     * another component (a test, a wrapping filter) holds the span open. Null ids when the service does not trace.
     */
    private String[] currentTrace(HttpServletRequest request) {
        if (tracer == null) {
            return new String[2];
        }
        var span = ServerHttpObservationFilter.findObservationContext(request)
            .map(context -> context.<TracingObservationHandler.TracingContext>get(TracingObservationHandler.TracingContext.class))
            .map(TracingObservationHandler.TracingContext::getSpan)
            .orElseGet(tracer::currentSpan);
        if (span == null) {
            return new String[2];
        }
        var context = span.context();
        return new String[]{ context.traceId(), context.spanId() };
    }

    private void record(HttpServletRequest request, CountingResponse response, int status, Instant started, long durationNanos, String requestId,
            String[] trace) {
        try {
            var client = resolver.resolve(request);
            var statusClass = statusClass(status);
            var method = method(request);
            metrics.accessRequest(statusClass, method, client.clientSource(), client.bot(), durationNanos);
            metrics.accessCountry(client.country());
            metrics.accessLocation(client.country(), client.city(), client.latitude(), client.longitude());

            var line = new LinkedHashMap<String, Object>();
            line.put("ts", started.toString());
            line.put("type", TYPE);
            line.put("method", request.getMethod());
            line.put("path", request.getRequestURI());
            line.put("query", request.getQueryString());
            line.put("status", status);
            line.put("duration_ms", durationNanos / 1_000_000L);
            line.put("bytes", response.bytes());
            line.put("client_ip", client.clientIp());
            line.put("client_source", client.clientSource());
            line.put("country", client.country());
            line.put("city", client.city());
            line.put("latitude", client.latitude());
            line.put("longitude", client.longitude());
            line.put("user_agent", client.userAgent());
            line.put("referer", request.getHeader("Referer"));
            line.put("bot", client.bot());
            line.put("user", user(request));
            line.put("request_id", requestId);
            line.put("trace_id", trace[0]);
            line.put("span_id", trace[1]);
            ACCESS_LOG.info(objectMapper.writeValueAsString(line));
        } catch (RuntimeException e) {
            // The access log must never break a request
            logger.warn("Access log line could not be written: " + e.getMessage());
        }
    }

    static String statusClass(int status) {
        var hundreds = status / 100;
        return hundreds >= 1
               && hundreds <= 5 ? hundreds + "xx" : "other";
    }

    private static String method(HttpServletRequest request) {
        var method = request.getMethod() == null ? ""
                : request.getMethod()
                    .toUpperCase(Locale.ROOT);
        return METHODS.contains(method) ? method : "other";
    }

    private static String user(HttpServletRequest request) {
        return request.getAttribute(CurrentUserProvider.REQUEST_ATTRIBUTE) instanceof AuthenticatedUser user ? user.username() : null;
    }

    /**
     * Counts the bytes written through the servlet output stream (characters, for the rare response written through the writer); a response that wrote nothing
     * through either falls back to its declared {@code Content-Length}.
     */
    static final class CountingResponse extends HttpServletResponseWrapper {

        private long written;
        private ServletOutputStream stream;
        private PrintWriter writer;

        CountingResponse(HttpServletResponse response) {
            super(response);
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            if (stream == null) {
                var delegate = super.getOutputStream();
                stream = new ServletOutputStream() {

                    @Override
                    public boolean isReady() {
                        return delegate.isReady();
                    }

                    @Override
                    public void setWriteListener(WriteListener writeListener) {
                        delegate.setWriteListener(writeListener);
                    }

                    @Override
                    public void write(int b) throws IOException {
                        delegate.write(b);
                        written++;
                    }

                    @Override
                    public void write(byte[] b, int off, int len) throws IOException {
                        delegate.write(b, off, len);
                        written += len;
                    }

                    @Override
                    public void flush() throws IOException {
                        delegate.flush();
                    }

                    @Override
                    public void close() throws IOException {
                        delegate.close();
                    }
                };
            }
            return stream;
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            if (writer == null) {
                var delegate = super.getWriter();
                writer = new PrintWriter(new Writer() {

                    @Override
                    public void write(char[] buffer, int offset, int length) {
                        delegate.write(buffer, offset, length);
                        written += length;
                    }

                    @Override
                    public void flush() {
                        delegate.flush();
                    }

                    @Override
                    public void close() {
                        delegate.close();
                    }
                });
            }
            return writer;
        }

        /**
         * @return the bytes of the body, or null when nothing went through the stream or writer and no content length was declared
         */
        Long bytes() {
            if (written > 0) {
                return written;
            }
            var declared = getHeader("Content-Length");
            if (declared != null) {
                try {
                    return Long.parseLong(declared.trim());
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
            return null;
        }
    }
}
