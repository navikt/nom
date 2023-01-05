package no.nav.nom.libs.utils.common.mdc;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@AutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnProperty(value = "nom.libs.utils.common.rest-mdc-interceptor.enabled", havingValue = "true", matchIfMissing = true)
public class RestMdcAutoconfiguration {

    @Bean
    @ConditionalOnMissingBean
    RestMdcInterceptor restMdcInterceptor() {
        return new RestMdcInterceptor();
    }


    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty("nom.libs.utils.common.rest-mdc-interceptor.registration")
    @EnableConfigurationProperties(RestMdcInterceptorProperties.class)
    static class RestMdcInterceptorAutoregistration {

        @Bean
        WebMvcConfigurer RestMdcConfigurer(RestMdcInterceptor interceptor, List<RestMdcInterceptorCustomizer> customizers) {
            return new WebMvcConfigurer() {
                @Override
                public void addInterceptors(InterceptorRegistry registry) {
                    InterceptorRegistration registration = registry.addInterceptor(interceptor);

                    for (RestMdcInterceptorCustomizer customizer : customizers) {
                        customizer.addPathPatterns(registration);
                        customizer.excludePathPatterns(registration);
                    }
                }
            };
        }
    }

}
