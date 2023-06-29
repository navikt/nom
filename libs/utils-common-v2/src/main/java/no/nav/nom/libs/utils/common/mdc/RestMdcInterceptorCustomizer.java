package no.nav.nom.libs.utils.common.mdc;

import org.springframework.web.servlet.config.annotation.InterceptorRegistration;

public interface RestMdcInterceptorCustomizer {
    default void addPathPatterns(InterceptorRegistration registration) { }
    default void excludePathPatterns(InterceptorRegistration registration) { }
}
