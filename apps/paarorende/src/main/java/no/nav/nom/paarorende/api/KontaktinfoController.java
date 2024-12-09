package no.nav.nom.paarorende.api;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.paarorende.common.SecurityUtil;
import no.nav.nom.paarorende.common.SwaggerConfig;
import no.nav.nom.paarorende.jpa.Adresse;
import no.nav.nom.paarorende.jpa.KontaktinfoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Kontaktinfo")
@RestController
@RequestMapping(
    value = KontaktinfoController.BASE_URL,
    produces = MediaType.APPLICATION_JSON_VALUE
)
public class KontaktinfoController {

    public final static String BASE_URL = "/api/kontaktinfo";

    private final KontaktinfoRepository kontaktInfoRepository;
    private final SecurityUtil securityUtil;

    public KontaktinfoController(KontaktinfoRepository kontaktInfoRepository, SecurityUtil securityUtils) {
        this.kontaktInfoRepository = kontaktInfoRepository;
        this.securityUtil = securityUtils;
    }

    @GetMapping
    @Operation(
        description = "Henter alle kontaktinfo til pårørende av brukeren",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste av kontaktinfo"),
        @ApiResponse(responseCode = "403", description = "Uatorisert"),

    })
    public List<KontaktinfoDTO> getAllKontaktinfo() {
        var navident = securityUtil.getNavIdent();

        return KontaktinfoDTO.fromEntityList(kontaktInfoRepository.findAllByNavidentAndDatoSlettetIsNull(navident));
    }

    @PutMapping
    @Operation(
        description = "Oppdaterer kontaktinfo til en pårørende",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Oppdatert kontaktinfo"),
        @ApiResponse(responseCode = "403", description = "Uatorisert"),

    })
    public KontaktinfoDTO oppdaterKontakinfo(@RequestBody KontaktinfoDTO kontaktInfoDTO) {
        var kontaktinfo = kontaktInfoRepository.findDistinctByKontakId(kontaktInfoDTO.kontaktId())
            .orElseThrow(() -> new AccessDeniedException("Unauthorized"));
        kontaktinfo.setFornavn(kontaktInfoDTO.fornavn());
        kontaktinfo.setEtternavn(kontaktInfoDTO.etternavn());
        kontaktinfo.setTelefon(kontaktInfoDTO.telefon());
        kontaktinfo.setAdresse(
            Adresse.builder().adresse(kontaktInfoDTO.gateadresse())
                .poststed(kontaktInfoDTO.postnr())
                .poststed(kontaktInfoDTO.poststed())
                .land(kontaktInfoDTO.land())
                .build()
        );

        securityUtil.assertAuthorized(kontaktinfo.getNavident());

        return KontaktinfoDTO.fromEntity(kontaktInfoRepository.save(kontaktinfo));
    }

    @PostMapping
    @Operation(
        description = "Oppretter nytt kontaktinfo til en pårørende",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Opprettet kontaktinfo"),
        @ApiResponse(responseCode = "403", description = "Uatorisert"),

    })
    public ResponseEntity<KontaktinfoDTO> opprettPaarorende(@RequestBody KontaktinfoDTO kontaktInfoDTO) {
        securityUtil.assertAuthorized();

        if (kontaktInfoRepository.countActiveByNavident(securityUtil.getNavIdent()) > 2) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        var kontakInfo = KontaktinfoDTO.toEntity(kontaktInfoDTO);
        kontakInfo.setNavident(securityUtil.getNavIdent());

        var response = KontaktinfoDTO.fromEntity(kontaktInfoRepository.save(kontakInfo));

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{kontaktId}")
    @Operation(
        description = "Slettetr kontaktinfo til en pårørende",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "403", description = "Uatorisert")
    })
    public void markForDeletion(@PathVariable String kontaktId) throws AccessDeniedException {
        var kontaktinfo = kontaktInfoRepository.findDistinctByKontakId(kontaktId)
            .orElseThrow(() -> new AccessDeniedException("Unauthorized"));

        securityUtil.assertAuthorized(kontaktinfo.getNavident());

        kontaktinfo.setDatoSlettet(LocalDate.now());

        kontaktInfoRepository.save(kontaktinfo);
    }
}
