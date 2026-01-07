package no.nav.nom.apps.api.common;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Optional;

@Slf4j
@Configuration
public class EndpointMetricsConfiguration implements WebMvcConfigurer {

    private static final String HEADER_CONSUMER_ID = "Nav-Consumer-Id";

    private final EndpointMetricInterceptor endpointMetricInterceptor;

    public EndpointMetricsConfiguration(MeterRegistry meterRegistry){
        log.info("Setup endpoint metrics");
        this.endpointMetricInterceptor = new EndpointMetricInterceptor(meterRegistry);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(endpointMetricInterceptor)
            .addPathPatterns("/**")
            .excludePathPatterns("/internal/**", "/graphql");
    }

    private static class EndpointMetricInterceptor implements HandlerInterceptor {

        private final MeterRegistry meterRegistry;

        public EndpointMetricInterceptor(MeterRegistry meterRegistry){
            this.meterRegistry = meterRegistry;
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            var bestMatchingPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            if(bestMatchingPattern == null){
                log.info("endpoint metrics prehandle: Missing `BEST_MATCHING_PATTERN_ATTRIBUTE`. Skipping update of endpoint consumption metrics");
                return true;
            }

            var maybeConsumerHeader = Optional.ofNullable(request.getHeader(HEADER_CONSUMER_ID)).map(it -> "consumer=" + it);


            var maybeAzpName = Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(authentication -> {
                    if (authentication.getDetails() instanceof JwtAuthenticationToken jwtAuthenticationToken) {
                        var maybeAzpNameClaim = jwtAuthenticationToken.getTokenAttributes().get("azp_name");
                        if (maybeAzpNameClaim instanceof String azpNameString) {
                            return azpNameString;
                        }
                        if(maybeConsumerHeader.isEmpty()){
                            log.info("endpoint metrics preHandle: Could not retrieve claim `azp_name`");
                        }

                        return null;
                    }
                    if (maybeConsumerHeader.isEmpty()) {
                        log.info("endpoint metrics prehandle: Authentication was not instanceof JwtAuthenticationToken");
                    }
                    return null;

                }).orElse(null);

            var maybeAppName = Optional.ofNullable(maybeAzpName).map(it -> "app=" + it);
            var identifier = maybeAppName.orElse(maybeConsumerHeader.orElse("UNKNOWN_CONSUMER"));
            Counter.builder("endpoint.hit").tags("uri", bestMatchingPattern, "identifier", identifier).register(meterRegistry).increment();
            return true;
        }
    }


}