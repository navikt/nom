package no.nav.nom.apps.api.orgenhet.api.app;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.api.app.dto.OrgEnhet;
import no.nav.nom.apps.api.rest.common.StandardResponse;
import no.nav.nom.apps.api.topicreader.ReadTopicRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "OrgEnheter")
@RestController
@RequestMapping(value = {"/app/orgenheter"})
public class OrgEnhetController {

    private final OrgenhetService orgenhetService;
    private final OrgEnhetMapper orgEnhetMapper;

    public OrgEnhetController(
        OrgenhetService orgenhetService,
        OrgEnhetMapper orgEnhetMapper
    ) {
        this.orgenhetService = orgenhetService;
        this.orgEnhetMapper = orgEnhetMapper;
    }

    @GetMapping("/{orgenhetId}")
    @Operation(
        description = "Henter orgenhet med gitt id",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    public ResponseEntity<OrgEnhet> hentOrgenhet(@PathVariable String orgenhetId) {

        return orgenhetService.getById(orgenhetId)
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

    @GetMapping("/ledetAv/{navident}")
    @Operation(
        description = "Henter alle orgenheter som er ledet av en gitt leder",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    public List<OrgEnhet> hentOrgEnheterByLeder(@PathVariable String navident) {
        return orgenhetService.getEnheterByLeder(navident).stream().map(orgEnhetMapper::mapFrom).collect(Collectors.toList());
    }

    @GetMapping("/underliggende/{parentId}")
    @Operation(
        description = "Henter ut id for alle underliggende organisasjonsenheter",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    public ResponseEntity<List<String>> getUnderliggendeOrgEnheter(@PathVariable("parentId") String parentId) {
        return ResponseEntity.ok(orgenhetService.findAllUnderliggendeOrgenheter(parentId));
    }

    @GetMapping("/read-topic")
    @Operation(
            description = "Henter records fra ressurs event-source",
            security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    public ResponseEntity<List<JsonNode>> readRessursTopic(@Valid ReadTopicRequest request){
        return ResponseEntity.ok(orgenhetService.getOrgEnhetEvents(request));
    }

    @PostMapping("/orgEnhetsType")
    @Operation(
        description = "Sett orgEnhetsType for en organisasjonsenhet",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<StandardResponse> setOrgEnhetsType(@RequestBody SetOrgEnhetsTypeRequest setOrgEnhetsTypeRequest){
        return orgenhetService.setOrgEnhetsType(setOrgEnhetsTypeRequest);
    }
}