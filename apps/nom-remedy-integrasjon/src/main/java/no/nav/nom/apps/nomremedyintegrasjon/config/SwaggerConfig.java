package no.nav.nom.apps.nomremedyintegrasjon.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    public static final String BEARER_TOKEN = "BearerAuth";

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
