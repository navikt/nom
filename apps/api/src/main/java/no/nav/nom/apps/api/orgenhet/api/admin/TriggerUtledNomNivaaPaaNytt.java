package no.nav.nom.apps.api.orgenhet.api.admin;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.dvh.DvhStartupLoadMonitor;
import no.nav.nom.apps.api.orgenhet.OrgEnhetNomNivaaHandler;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@Tag(name = "Admin for å utlede nom nivåer på nytt.")
@RestController
@RequestMapping("/api/admin/utlednomnivaaer")
@PreAuthorize("hasAuthority('ADMIN')")
public class TriggerUtledNomNivaaPaaNytt {

    private final OrgEnhetNomNivaaHandler orgEnhetNomNivaaHandler;
    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private final DvhStartupLoadMonitor dvhStartupLoadMonitor;

    private final Logger logger = LoggerFactory.getLogger(TriggerUtledNomNivaaPaaNytt.class);

    public TriggerUtledNomNivaaPaaNytt(OrgEnhetNomNivaaHandler orgEnhetNomNivaaHandler, OrgEnhetStateRepository orgEnhetStateRepository,
                                       DvhStartupLoadMonitor dvhStartupLoadMonitor
    ) {
        this.orgEnhetNomNivaaHandler = orgEnhetNomNivaaHandler;
        this.orgEnhetStateRepository = orgEnhetStateRepository;
        this.dvhStartupLoadMonitor = dvhStartupLoadMonitor;
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

        dvhStartupLoadMonitor.awaitInitialLoadComplete();

        orgenheterToProcess
            .forEach(orgEnhetState -> {
                if (orgEnhetState.orgEnhetsType() == null || orgEnhetState.orgEnhetsType().isEmpty()) {
                    logger.info("OrgEnhetState {} har ingen orgEnhetsType, kan ikke utlede nomNivaa", orgEnhetState.id());
                }
                    orgEnhetNomNivaaHandler.mapOrgEnhetTypeToNewNomNivaaAndUpdate(orgEnhetState, orgEnhetState.orgEnhetsType().isPresent() ? orgEnhetState.orgEnhetsType().get() : null);
                }
            );

        if (orgEnhetId != null) {
            orgenheterToProcess.forEach(orgEnhetNomNivaaHandler::mapOrgEnhetTypeToNewNomNivaaForChildren);
        }

        orgenheterToProcess.stream()
            .filter(orgEnhetState -> orgEnhetState.nomNivaa().equals(Optional.of(NomNivaa.DRIFTSENHET)))
            .forEach(orgEnhetNomNivaaHandler::addArbeidsomraadeInAllSubPathsIfMissing);


        var message = String.format("Utledet nivå på nytt for %s enhet", orgenheterToProcess.size());

        logger.info(message);

        return String.format("Utledet nivå på nytt for %s enhet", orgenheterToProcess.size());
    }
}
