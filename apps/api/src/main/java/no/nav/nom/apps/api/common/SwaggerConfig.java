package no.nav.nom.apps.api.common;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    public static final String BEARER_TOKEN = "BearerAuth";

    public static final String API_V1_NAME = "External API v1";
    public static final String BACKEND_NAME = "Backend";

    @Bean
    public GroupedOpenApi v1Api() {
        return GroupedOpenApi.builder()
            .group(API_V1_NAME)
            .pathsToMatch("/api/v1/**")
            .build();
    }

    @Bean
    public GroupedOpenApi apiBackend() {
        return GroupedOpenApi.builder()
            .group(BACKEND_NAME)
            .pathsToMatch("/backend/**")
            .build();
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI().info(new Info().title("NOM API").description("REST API for NOM"))
                .components(new Components().addSecuritySchemes(
                        BEARER_TOKEN,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .description("Bearer token")
                ));
    }
}
