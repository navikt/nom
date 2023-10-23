package no.nav.nom.apps.api.orgenhet.api.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.api.app.dto.OrgEnhet;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "OrgEnheter")
@RestController
@RequestMapping(value = {"/app/orgenheter"})
public class OrgEnheterController {

    private final OrgenhetService orgenhetService;
    private final OrgEnhetMapper orgEnhetMapper;

    public OrgEnheterController(
        OrgenhetService orgenhetService,
        OrgEnhetMapper orgEnhetMapper
    ) {
        this.orgenhetService = orgenhetService;
        this.orgEnhetMapper = orgEnhetMapper;
    }

    @GetMapping("/{orgenhetid}")
    @Operation(
        description = "Henter orgenhet med gitt id",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    public ResponseEntity<OrgEnhet> hentOrgenhet(@PathVariable String orgenhetid) {

        return orgenhetService.getById(orgenhetid)
            .map(orgEnhetMapper::mapFrom)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(
        description = "Henter alle orgenheter",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    public List<OrgEnhet> hentOrgEnheter() {
        return orgenhetService.getAllEnheter().stream().map(orgEnhetMapper::mapFrom).collect(Collectors.toList());
    }

}