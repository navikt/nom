package no.nav.nom.apps.api.orgenhet.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.orgenhet.OrgEnhetDvhService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.EndreOrgEnhetsType;
import no.nav.nom.apps.api.orgenhet.eventsourcing.state.OrgEnhetStateRepository;
import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@Slf4j
@Tag(name = "OrgEnhet")
@RestController
@RequestMapping(value={"/app/orgenhet"})
public class OrgEnhetController {

    private final OrgEnhetCommandService orgEnhetCommandService;
    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private final OrgEnhetDvhService orgEnhetDvhService;

    public OrgEnhetController(OrgEnhetCommandService orgEnhetCommandService, OrgEnhetStateRepository orgEnhetStateRepository,
                              OrgEnhetDvhService orgEnhetDvhService
    ) {
        this.orgEnhetCommandService = orgEnhetCommandService;
        this.orgEnhetStateRepository = orgEnhetStateRepository;

        this.orgEnhetDvhService = orgEnhetDvhService;
    }

    @PostMapping("/orgEnhetsType")
    @Operation(
        description = "Sett orgEnhetsType for en organisasjonsenhet",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<StandardResponse> setOrgEnhetsType(@RequestBody SetOrgEnhetsTypeRequest setOrgEnhetsTypeRequest){

        return orgEnhetStateRepository.findById(setOrgEnhetsTypeRequest.getNomId())
            .map(orgEnhet -> {

                sendEndreOrgEnhetsTypeEvent(orgEnhet, setOrgEnhetsTypeRequest.getOrgEnhetsType());

                return ResponseEntity.ok().body(
                    new StandardResponse(String.format(
                        "Oppdatert orgEnhetsType for %s", setOrgEnhetsTypeRequest.getNomId()
                    ))
                );
            })
            .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(new StandardResponse("NotFound")));

    }

    @GetMapping("/underLiggende/{parentId}")
    @Operation(
        description = "Henter ut id for alle underliggende organisasjonsenheter",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<String>> getUnderliggendeOrgEnheter(@PathVariable("parentId") String parentId) {
        return ResponseEntity.ok(orgEnhetDvhService.findAllUnderliggendeOrgenheter(parentId));

    }

    private void sendEndreOrgEnhetsTypeEvent(OrgEnhetState orgEnhetState, OrgEnhetsType orgEnhetsType) {
        var existingOrgEnhetsTypeIsDifferentFromNewValueOrNotPresent = orgEnhetState.orgEnhetsType()
            .map(existingOrgenhetsType -> existingOrgenhetsType != orgEnhetsType)
            .orElse(true);

        if (existingOrgEnhetsTypeIsDifferentFromNewValueOrNotPresent) {
            orgEnhetCommandService.endreOrgEnhetsType(
                orgEnhetState.fid(),
                new EndreOrgEnhetsType(
                    Optional.ofNullable(orgEnhetsType),
                    new InputMetadata(
                        "nom-api REST",
                        "setOrgEnhetsType",
                        null,
                        new Origination("nom-api", "nom-api"))
                )
            );
        }
    }

}
