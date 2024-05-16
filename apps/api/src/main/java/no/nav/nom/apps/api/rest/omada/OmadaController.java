package no.nav.nom.apps.api.rest.omada;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.dto.DvhLeder;
import no.nav.nom.apps.api.dvh.dto.DvhTelefon;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.remedy.RemedyEnhet;
import no.nav.nom.apps.api.remedy.RemedyEnhetMapperService;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import no.nav.nom.contract.nom.state.OrganiseringState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static java.util.Objects.isNull;

@Tag(name = "Eksport av data til Omada")
@RestController
@RequestMapping("/api/v1/omada")
public class OmadaController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final OrganiseringStateRepository organiseringStateRepository;
    private final RemedyEnhetMapperService remedyEnhetMapperService;
    private final DvhRepository dvhRepository;
    private final OrgenhetService orgenhetService;
    private final RessursStateRepository ressursStateRepository;

    public OmadaController(
        OrganiseringStateRepository organiseringStateRepository,
        RemedyEnhetMapperService remedyEnhetMapperService,
        DvhRepository dvhRepository, OrgenhetService orgenhetService,
        RessursStateRepository ressursStateRepository
    ) {
        this.organiseringStateRepository = organiseringStateRepository;
        this.remedyEnhetMapperService = remedyEnhetMapperService;
        this.dvhRepository = dvhRepository;
        this.orgenhetService = orgenhetService;
        this.ressursStateRepository = ressursStateRepository;
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

    @GetMapping("/telefonnummer/{navIdent}")
    public ResponseEntity<OmadaRessursTelefonDTO> getRessursTelefon(@PathVariable String navIdent) {
        var ressursState = ressursStateRepository.findByNavident(navIdent).orElse(null);

        if (!isNull(ressursState) && ressursState.isEkstern())
            return ressursState.primaryTelefonnummer().map(telefonnummer -> {
                var ressursInfo = new OmadaRessursTelefonDTO(ressursState.navident(), telefonnummer);
                logger.debug("Telefonnummer til Omada for ressurs {}: {}", navIdent, ressursInfo);
                return ResponseEntity.ok(ressursInfo);
            }).orElse(ResponseEntity.notFound().build());

        return dvhRepository.getTelefonNrListByNavIdent(navIdent).stream()
            .findFirst()
            .map(dvhTelefon -> {
                var ressursInfo = toOmadaRessursInfoDTO(dvhTelefon);

                logger.debug("Telefonnummer til Omada for ressurs {}: {}", navIdent, ressursInfo);
                return ResponseEntity.ok(ressursInfo);
            })
            .orElse(ResponseEntity.notFound().build());
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

    private String getLeder(IOrgenhet orgEnhet) {
        return dvhRepository.getLederByAgressoIdAndNivAndNavn(orgEnhet.getAgressoId(), orgEnhet.getOrgNiv(), orgEnhet.getNavn())
            .stream().findAny()
            .map(DvhLeder::getNavIdent)
            .orElse(null);
    }

    private OmadaRessursTelefonDTO toOmadaRessursInfoDTO(DvhTelefon dvhTelefon) {
        return new OmadaRessursTelefonDTO(
            dvhTelefon.getNavIdent(),
            dvhTelefon.getNummer()
        );
    }

}
