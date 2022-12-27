package no.nav.nom.apps.navident.common;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.media.UUIDSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityScheme;
import no.nav.nom.libs.utils.common.mdc.MdcConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SwaggerConfig {

    public static String[] SWAGGER_API_URLS = {
            "/api",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/swagger-resources/**",
            "/v3/api-docs/**"
    };
    public static final String SECURITY_SCHEME_CONFIGURATION_NAME = "BearerAuth";

    @Bean
    public OpenAPI customOpenAPI(@Value("${spring.application.name}") String appname, @Value("${random.uuid") String uuid) {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("Nav Ident Komponent API")
                                .description("REST API for bestilling av nav-identer")
                )
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        SECURITY_SCHEME_CONFIGURATION_NAME,
                                        new SecurityScheme().type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")
                                                .in(SecurityScheme.In.HEADER)
                                                .description("Bearer token")
                                )
                                .addParameters(
                                        MdcConfig.MDC_CALL_ID_KEY,
                                        new Parameter()
                                                .name(MdcConfig.MDC_CALL_ID_KEY)
                                                .in(ParameterIn.HEADER.toString())
                                                .description("Call/Correlation id for this request")
                                                .example(uuid)
                                                .required(true)
                                                .schema(new UUIDSchema())
                                )
                                .addParameters(
                                        MdcConfig.HEADER_X_REQUEST_ID_KEY,
                                        new Parameter()
                                                .name(MdcConfig.HEADER_X_REQUEST_ID_KEY)
                                                .in(ParameterIn.HEADER.toString())
                                                .description("Alternate call/correlation id, if missing, will be the same value as " + MdcConfig.HEADER_CALL_ID_KEY)
                                                .example(uuid)
                                                .required(false)
                                                .schema(new UUIDSchema())
                                )
                                .addParameters(
                                        MdcConfig.HEADER_CONSUMER_ID_KEY,
                                        new Parameter()
                                                .name(MdcConfig.HEADER_CONSUMER_ID_KEY)
                                                .in(ParameterIn.HEADER.toString())
                                                .description("The calling application identifier")
                                                .example(appname)
                                                .required(true)
                                                .schema(new StringSchema())
                                )
                );
    }
}
