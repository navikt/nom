package no.nav.nom.apps.navident.common;

import lombok.RequiredArgsConstructor;
import no.nav.nom.libs.utils.common.mdc.RestMdcInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Component
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    // Require Nav-[Call-Id/Consumer-Id] headers
    private final RestMdcInterceptor restMdcInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(restMdcInterceptor)
                .excludePathPatterns("/internal/**")
                .addPathPatterns("/navident");
    }

    // TODO: Add Swagger
//    @Override
//    public void addViewControllers(ViewControllerRegistry registry) {
//        registry.addViewController("/internal/api").setViewName("redirect:/swagger-ui.html");
//    }

    // TODO: Add Metrics
//    @Bean
//    InitializingBean prometheusPostProcessor(BeanPostProcessor meterRegistryPostProcessor, PrometheusMeterRegistry registry) {
//        return () -> meterRegistryPostProcessor.postProcessAfterInitialization(registry, "");
//    }
}
