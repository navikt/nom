package no.nav.nom.apps.api.orgenhet.api.admin;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.orgenhet.OrgEnhetNomNivaaHandler;
import no.nav.nom.apps.api.orgenhet.eventsourcing.state.OrgEnhetStateRepository;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.OrgEnhetTypeEndretEventV1;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin for å utlede nom nivåer på nytt.")
@RestController
@RequestMapping("/api/admin/utlednomnivaaer")
public class TriggerUtledNomNivaaPaaNytt {

    private final OrgEnhetNomNivaaHandler orgEnhetNomNivaaHandler;
    private final OrgEnhetStateRepository orgEnhetStateRepository;

    public TriggerUtledNomNivaaPaaNytt(OrgEnhetNomNivaaHandler orgEnhetNomNivaaHandler, OrgEnhetStateRepository orgEnhetStateRepository) {
        this.orgEnhetNomNivaaHandler = orgEnhetNomNivaaHandler;
        this.orgEnhetStateRepository = orgEnhetStateRepository;
    }

    @Operation(
        description = "Utled og skriv nom nivå for alle orgenheter basert på orgenhet typer",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @GetMapping()
    String utledNomNivaPaaNytt(@RequestParam(required = false) String orgEnhetId) {
        var orgenheterToProcess =  orgEnhetStateRepository.getAll().stream()
            .filter(orgEnhetState -> orgEnhetId == null || orgEnhetState.id().equals(orgEnhetId))
            .toList();

        orgenheterToProcess
            .forEach(orgEnhetState -> orgEnhetNomNivaaHandler.mapOrgEnhetTypeToNewNomNivaaAndUpdate(
                new OrgEnhetTypeEndretEventV1(
                    orgEnhetState.fid(),
                    new OrgEnhetTypeEndretEventV1.OrgEnhetTypeEndretEventData(orgEnhetState.orgEnhetsType()),
                    new InputMetadata(null,null,null,null)
                )
            )

        );

        return String.format("Utledet nivå på nytt for %s enhet", orgenheterToProcess.size());
    }
}
