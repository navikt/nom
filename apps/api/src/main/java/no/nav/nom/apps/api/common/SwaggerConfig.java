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

    // Sort order er på groupid
    public static final String API_V1_ID = "1 - External API v1";
    public static final String API_V1_DISPLAY_NAME = "External API v1";
    public static final String BACKEND_ID = "2 - app";
    public static final String BACKEND_DISPLAY_NAME = "Internal App api";
    public static final String INTERN_ADMIN_ID = "3 - Intern admin";
    public static final String INTERN_ADMIN_DISPLAY_NAME = "Internal admin";

    @Bean
    public GroupedOpenApi v1Api() {
        return GroupedOpenApi.builder()
            .group(API_V1_ID)
            .displayName(API_V1_DISPLAY_NAME)
            .pathsToMatch("/api/v1/**")
            .build();
    }

    @Bean
    public GroupedOpenApi apiBackend() {
        return GroupedOpenApi.builder()
            .group(BACKEND_ID)
            .displayName(BACKEND_DISPLAY_NAME)
            .pathsToMatch("/app/**")
            .build();
    }

    @Bean
    public GroupedOpenApi apiInternAdmin() {
        return GroupedOpenApi.builder()
            .group(INTERN_ADMIN_ID)
            .displayName(INTERN_ADMIN_DISPLAY_NAME)
            .pathsToMatch("/api/admin/**")
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
