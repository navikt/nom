package no.nav.nom.apps.api.rest.omada;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhetleder.OrgEnhetLederService;
import no.nav.nom.apps.api.remedy.RemedyEnhet;
import no.nav.nom.apps.api.remedy.RemedyEnhetMapperService;
import no.nav.nom.apps.api.telefon.TelefonService;
import no.nav.nom.apps.api.tilgangsenhet.TilgangsenhetMapper;
import no.nav.nom.apps.api.tilgangsenhet.TilgangsenhetRepository;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Eksport av data til Omada")
@RestController
@RequestMapping("/api/v1/omada")
public class OmadaController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final OrganiseringStateRepository organiseringStateRepository;
    private final RemedyEnhetMapperService remedyEnhetMapperService;
    private final OrgEnhetLederService orgEnhetLederService;
    private final OrgenhetService orgenhetService;
    private final TelefonService telefonService;
    private final TilgangsenhetMapper tilgangsenhetMapper;

    public OmadaController(
        OrganiseringStateRepository organiseringStateRepository,
        RemedyEnhetMapperService remedyEnhetMapperService,
        OrgEnhetLederService orgEnhetLederService,
        OrgenhetService orgenhetService,
        TelefonService telefonService, TilgangsenhetMapper tilgangsenhetMapper
    ) {
        this.organiseringStateRepository = organiseringStateRepository;
        this.remedyEnhetMapperService = remedyEnhetMapperService;
        this.orgEnhetLederService = orgEnhetLederService;
        this.orgenhetService = orgenhetService;
        this.telefonService = telefonService;
        this.tilgangsenhetMapper = tilgangsenhetMapper;
    }

    @Operation(
        description = "Hent informasjon Omada trenger om gitt orgEnhet",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping("/orgEnhet/{id}")
    public ResponseEntity<OmadaOrgEnhetInfoDTO> getOrgEnhetInfo(@PathVariable String id) {
        return orgenhetService.getById(id)
            .map(orgEnhet -> {
                var orgEnhetInfo = new OmadaOrgEnhetInfoDTO(
                    id,
                    getLeder(orgEnhet),
                    getOrgenhetOver(id),
                    getNorgId(id)
                );
                logger.debug("Info til Omdada for orgEnhet {}: {}", id, orgEnhetInfo);
                return ResponseEntity.ok(orgEnhetInfo);
            }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/orgEnhet2/{id}")
    public ResponseEntity<OmadaOrgEnhetInfoDTO> getOrgEnhetInfo2(@PathVariable String id) {
        return orgenhetService.getById(id)
            .map(orgEnhet -> {
                var orgEnhetInfo = new OmadaOrgEnhetInfoDTO(
                    id,
                    getLeder(orgEnhet),
                    getOrgenhetOver(id),
                    getTilgangsenhet(id)
                );
                return ResponseEntity.ok(orgEnhetInfo);
            }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/telefonnummer/{navIdent}")
    public ResponseEntity<OmadaRessursTelefonDTO> getRessursTelefon(@PathVariable String navIdent) {
        var prioritertTelefon = telefonService.getPrioritertTelefonnummer(navIdent);

        if (prioritertTelefon == null) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok(new OmadaRessursTelefonDTO(navIdent, prioritertTelefon));
        }
    }

    private String getOrgenhetOver(String id) {
        return organiseringStateRepository.findActiveById(id)
            .map(OrganiseringState::idOver)
            .orElse(null);
    }

    private String getNorgId(String id) {
        return remedyEnhetMapperService.findByNomId(id)
            .map(RemedyEnhet::remedyEnhetId)
            .orElse(null);
    }

    private String getTilgangsenhet(String id) {
        return tilgangsenhetMapper.byNomId(id);
    }

    private String getLeder(IOrgenhet orgEnhet) {
        return orgEnhetLederService.getAktivLederForOrgEnhet(orgEnhet.getId()).map(OrgEnhetLederState::navident).orElse(null);
    }

}
