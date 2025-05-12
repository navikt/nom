package no.nav.nom.apps.admin;

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

    @Bean
    public GroupedOpenApi v1Api() {
        return GroupedOpenApi.builder()
            .group("internal")
            .displayName("NOM ADMIN API v1")
            .pathsToMatch("/sinks/**", "/states/**", "/SanityCheck/**", "/RemedyCompare/**", "/sessioninfo")
            .build();
    }

    @Bean
    public OpenAPI adminApi() {
        return new OpenAPI()
            .info(new Info()
                .title("NOM ADMIN")
                .description("REST API for NOM ADMIN")
                .version("v1"))
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
