package no.nav.nom.apps.sfintegrasjon.common;

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
    public GroupedOpenApi zalarisApi() {
        return GroupedOpenApi.builder()
            .group("Zalaris API")
            .displayName("Zalaris SuccessFactors API v1")
            .pathsToMatch("/api/v1/zalaris/**")
            .build();
    }

    @Bean
    public OpenAPI zalarisOpenAPI() {
        return new OpenAPI().info(new Info().title("Zalaris REST API")
                .description("A Zalaris SuccessFactors REST API to push and receive data from NOM and NIK")).components(new Components().addSecuritySchemes(
                BEARER_TOKEN,
                new SecurityScheme().type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .in(SecurityScheme.In.HEADER)
                        .description("Bearer Token")
        ));
    }
}
