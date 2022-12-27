package no.nav.nom.apps.navident.common;

import no.nav.nom.apps.navident.api.NavidentController;
import no.nav.nom.libs.utils.common.mdc.RestMdcInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Component
public class WebConfig implements WebMvcConfigurer {

    // Require Nav-[Call-Id/Consumer-Id] headers
    private final RestMdcInterceptor restMdcInterceptor;

    public WebConfig(RestMdcInterceptor restMdcInterceptor) {
        this.restMdcInterceptor = restMdcInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(restMdcInterceptor)
                .excludePathPatterns("/internal/**")
                .addPathPatterns(NavidentController.BASE_URL);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/api", "/swagger-ui.html");
    }

    // TODO: Add Metrics
//    @Bean
//    InitializingBean prometheusPostProcessor(BeanPostProcessor meterRegistryPostProcessor, PrometheusMeterRegistry registry) {
//        return () -> meterRegistryPostProcessor.postProcessAfterInitialization(registry, "");
//    }
}
