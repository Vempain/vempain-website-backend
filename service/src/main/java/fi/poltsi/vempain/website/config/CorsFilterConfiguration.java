package fi.poltsi.vempain.website.config;

import java.util.List;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * CORS handling for the frontend, which is served from a different origin during development. The filter runs before the authentication filter so that
 * preflight requests never reach it.
 */
@Configuration
public class CorsFilterConfiguration {

    @Bean
    FilterRegistrationBean<CorsFilter> corsFilterRegistration(SiteProperties siteProperties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // traceparent/tracestate: the W3C trace context the frontend sends so that its API calls join one trace
        configuration.setAllowedHeaders(List.of("Content-Type", "Authorization", "X-Requested-With", "X-Auth-Token", "traceparent", "tracestate"));
        // X-Request-Id: the per-request id of the access log, readable by the frontend for support cases
        configuration.setExposedHeaders(List.of("X-Auth-Token", "X-Request-Id"));
        configuration.setAllowCredentials(true);

        List<String> origins = siteProperties.getCorsAllowedOrigins()
            .stream()
            .map(String::trim)
            .filter(origin -> !origin.isEmpty())
            .toList();
        // An omitted allow-list must fail closed. Reflecting arbitrary origins while
        // allowing credentials would let any website make authenticated requests.
        configuration.setAllowedOrigins(origins.stream()
            .filter(origin -> !"*".equals(origin))
            .toList());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(new CorsFilter(source));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);

        return registration;
    }
}
