package no.nav.nom.apps.api.orgenhet.api.admin;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.dvh.DvhStartupLoadMonitor;
import no.nav.nom.apps.api.orgenhet.OrgEnhetNomNivaaHandler;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.rest.common.StandardResponse;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
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
    private final Logger logger = LoggerFactory.getLogger(TriggerUtledNomNivaaPaaNytt.class);

    private final OrgEnhetNomNivaaHandler orgEnhetNomNivaaHandler;
    private final DvhStartupLoadMonitor dvhStartupLoadMonitor;
    private final OrgenhetService orgenhetService;
    private final String nav;


    public TriggerUtledNomNivaaPaaNytt(OrgEnhetNomNivaaHandler orgEnhetNomNivaaHandler,
                                       DvhStartupLoadMonitor dvhStartupLoadMonitor,
                                       OrgenhetService orgenhetService,
                                       @Value("${app.orgenhet.nav}") String nav
                                       ) {
        this.orgEnhetNomNivaaHandler = orgEnhetNomNivaaHandler;
        this.dvhStartupLoadMonitor = dvhStartupLoadMonitor;
        this.orgenhetService = orgenhetService;
        this.nav = nav;
    }

    @Operation(
        description = "Utled og skriv nom nivå for alle orgenheter basert på orgenhet typer",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @GetMapping()
    String utledNomNivaPaaNytt(@RequestParam(required = false) String orgEnhetId) {
        logger.info(nav);
        var orgenheterToProcess = orgenhetService.getAllEnheterMedOrganisering().stream()
            .filter(orgEnhetState -> orgEnhetId == null || orgEnhetState.getId().equals(orgEnhetId))
            .filter(iOrgenhet -> !iOrgenhet.getId().equals(nav))
            .map(iOrgenhet -> OrgEnhetState.builder()
                .fid(iOrgenhet.getFid())
                .id(iOrgenhet.getId())
                .navn(iOrgenhet.getNavn())
                .gyldigFom(iOrgenhet.getGyldigFom())
                .gyldigTom(iOrgenhet.getGyldigTom())
                .overordnetOrgEnhetFid(iOrgenhet.getOverordnetOrgEnhetFid())
                .lederFid(Optional.empty())
                .orgEnhetsType(iOrgenhet.getOrgEnhetsType())
                .nomNivaa(iOrgenhet.getNomNivaa())
                .uniqueAgressoId(Optional.ofNullable(iOrgenhet.getUniqueAgressoId()))
                .agressoId(Optional.ofNullable(iOrgenhet.getAgressoId()))
                .agressoOrgenhetType(Optional.empty())
                .build()).toList();

        dvhStartupLoadMonitor.awaitInitialLoadComplete();

        orgenheterToProcess
            .forEach(orgEnhetState -> {
                if (orgEnhetState.orgEnhetsType() == null || orgEnhetState.orgEnhetsType().isEmpty()) {
                    logger.error("OrgEnhetState {} har ingen orgEnhetsType, kan ikke utlede nomNivaa", orgEnhetState.id());
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

    @ExceptionHandler({
        NullPointerException.class
    })
    public ResponseEntity<StandardResponse> handleException(Exception e) {
        logger.error("Exception occurred: {}", e.getMessage(), e);
        return ResponseEntity.badRequest().body(StandardResponse.builder()
            .message("En feil oppstod under behandling av forespørselen: " + e.getMessage())
            .build());
    }
}
