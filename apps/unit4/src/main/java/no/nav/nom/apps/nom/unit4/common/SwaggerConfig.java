package no.nav.nom.apps.nom.unit4.common;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    public static final String BEARER_TOKEN = "BearerAuth";
    public static final String API_VERSION = "API versjon 1";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI().info(new Info().title("NOM Unit4 API").description("REST API for NOM Unit4 integrasjons prosjekt"))
                .components(new Components().addSecuritySchemes(
                        BEARER_TOKEN,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .description("Bearer token")
                ));
    }

    @Bean
    public GroupedOpenApi v1Api() {
        return GroupedOpenApi.builder().group(API_VERSION).pathsToMatch("/api/v1/**").build();
    }
}
