package no.nav.nom.libs.utils.common.mdc;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;

import java.util.List;

@ConstructorBinding
@ConfigurationProperties(value = "nom.libs.utils.common.rest-mdc-interceptor.registration")
public record RestMdcInterceptorProperties (
    @DefaultValue List<String> pathPatterns,
    @DefaultValue List<String> excludePathPatterns

) implements RestMdcInterceptorCustomizer {
    @Override
    public void addPathPatterns(InterceptorRegistration registration) {
        registration.addPathPatterns(pathPatterns);
    }

    @Override
    public void excludePathPatterns(InterceptorRegistration registration) {
        registration.excludePathPatterns(excludePathPatterns);
    }
}
