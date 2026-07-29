package no.nav.nom.apps.sfintegrasjon.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    public static final String BEARER_TOKEN = "BearerAuth";

    @Bean
    public GroupedOpenApi zalarisApi() {
        return GroupedOpenApi.builder()
            .group("Zalaris API")
            .displayName("Zalaris SuccessFactors API v1")
            .pathsToMatch("/api/v1/zalaris/**")
            .build();
    }

    /**
     * In dev/prod: Swagger UI shows the "Authorize" button with Bearer token input.
     * All endpoints require a valid Azure AD JWT with NOM_ADMIN group membership.
     */
    @Bean
    @Profile("!local")
    public OpenAPI openApiWithSecurity() {
        return new OpenAPI()
                .info(new Info()
                        .title("nom-sf-integrasjon API")
                        .version("v1"))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_TOKEN))
                .components(new Components()
                        .addSecuritySchemes(BEARER_TOKEN, new SecurityScheme()
                                .name(BEARER_TOKEN)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Azure AD Bearer token. Requires NOM_ADMIN group membership.")
                        ));
    }

    /**
     * Locally: no auth required, so no security scheme is registered in Swagger UI.
     */
    @Bean
    @Profile("local")
    public OpenAPI openApiLocal() {
        return new OpenAPI()
                .info(new Info()
                        .title("nom-sf-integrasjon API (local — no auth)")
                        .version("v1"));
    }
}
