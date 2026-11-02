package no.nav.nom.apps.api.rest.config;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.common.SwaggerConfig;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@Slf4j
public class EnvironmentConfigController {

    private final Environment environment;
    private final AppProperties appProperties;

    public EnvironmentConfigController(Environment environment, AppProperties appProperties) {
        this.environment = environment;
        this.appProperties = appProperties;
    }

    @Operation(
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @GetMapping(value={"/app/environmentconfig"})
    public EnvironmentConfigResponse environmentconfig() {
        return new EnvironmentConfigResponse(
            deployCluster(),
            appProperties.suffix(),

            appProperties.orgenhet().navId(),
            appProperties.orgenhet().direktoratId(),
            appProperties.orgenhet().teknologiavdelingenId(),

            appProperties.orgenhet().linjeenhetene(),

            appProperties.orgenhet().helfoId(),
            appProperties.orgenhet().skattId(),
            appProperties.orgenhet().riksrevisjonenId(),
            appProperties.orgenhet().vikafossenId()
        );
    }

    DeployCluster deployCluster() {
        var isProd = "prod-gcp".equals(environment.getProperty("NAIS_CLUSTER_NAME"));
        if (isProd) {
            return DeployCluster.PROD;
        }
        return DeployCluster.DEV;
    }

    @Schema(enumAsRef = true)
    public enum DeployCluster {
        PROD,
        DEV
    }

    public record EnvironmentConfigResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "PROD/DEV") DeployCluster deployCluster,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "Suffix for deployment miljø") String environmentSuffix,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Nom OrgEnhetId for NAV Toppnode enhet") String navToppnodeId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Nom OrgEnhetId for DIREKTORATET enhet") String direktoratId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Nom OrgEnhetId for TEKNOLOGIAVDELINGEN enhet") String teknologiavdelingenId,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Nom OrgEnhetId'er for alle linjeenheter") List<String> linjeenhetene,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Nom OrgEnhetId for HELFO Løvnode enhet") String helfoId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Nom OrgEnhetId for SKATTEETATEN Løvnode enhet") String skattId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Nom OrgEnhetId for RIKSREVISJONEN Løvnode enhet") String riksrevisjonenId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Nom OrgEnhetId for VIKAFOSSEN Løvnode enhet") String vikafossenId
    ) {}
}
