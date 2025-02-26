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
import no.nav.nom.paarorende.jpa.Relasjon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Tag(name = "Kontaktinfo")
@RestController
@RequestMapping(
    value = KontaktinfoController.BASE_URL,
    produces = MediaType.APPLICATION_JSON_VALUE
)
public class KontaktinfoController {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public final static String BASE_URL = "/api/kontaktinfo";

    private final KontaktinfoRepository kontaktInfoRepository;
    private final SecurityUtil securityUtil;

    public KontaktinfoController(KontaktinfoRepository kontaktInfoRepository, SecurityUtil securityUtils) {
        this.kontaktInfoRepository = kontaktInfoRepository;
        this.securityUtil = securityUtils;
    }

    @GetMapping("/tilgang/{navident}")
    @Operation(
        description = "Sjekker hvorvidt innehaver av tokenet har tilgang til pårørende kontaktinfo til oppgitt ansatt",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Liste av kontaktinfo"),
            @ApiResponse(responseCode = "401", description = "Uautentisert"),
            @ApiResponse(responseCode = "403", description = "Uatorisert")
        }
    )
    public ResponseEntity<Void> verifiserHarTilgang(@PathVariable String navident) {
        securityUtil.assertAuthorizedAccessTo(navident);

        return ResponseEntity.ok().build();
    }

    @GetMapping
    @Operation(
        description = "Henter alle kontaktinfo til pårørende av brukeren",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Liste av kontaktinfo"),
            @ApiResponse(responseCode = "403", description = "Uatorisert")
        }
    )
    public List<KontaktinfoDTO> getKontaktinfo() {
        var navident = securityUtil.getNavIdent();

        return KontaktinfoDTO.fromEntityList(kontaktInfoRepository.findAllByNavidentAndDatoSlettetIsNull(navident));
    }

    @GetMapping("/{navident}")
    @Operation(
        description = "Henter kontaktinfo til pårørende - brukes av admin og ledere",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Opprettet kontaktinfo"),
            @ApiResponse(responseCode = "401", description = "Uautentisert"),
            @ApiResponse(responseCode = "403", description = "Uatorisert")
        }
    )
    public List<KontaktinfoDTO> getKontaktinfoForRessurs(@PathVariable String navident) {
        securityUtil.assertAuthorizedAccessTo(navident);

        return KontaktinfoDTO.fromEntityList(kontaktInfoRepository.findAllByNavidentAndDatoSlettetIsNull(navident));
    }

    @PutMapping
    @Operation(
        description = "Oppdaterer kontaktinfo til en pårørende",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Oppdatert kontaktinfo"),
            @ApiResponse(responseCode = "401", description = "Uautentisert"),
            @ApiResponse(responseCode = "403", description = "Uatorisert")
        }
    )
    public KontaktinfoDTO oppdaterKontakinfo(@RequestBody KontaktinfoDTO kontaktInfoDTO) {
        var kontaktinfo = kontaktInfoRepository.findDistinctByKontakId(kontaktInfoDTO.kontaktId())
            .orElseThrow(() -> new AccessDeniedException("Unauthorized"));
        kontaktinfo.setFornavn(kontaktInfoDTO.fornavn());
        kontaktinfo.setEtternavn(kontaktInfoDTO.etternavn());
        kontaktinfo.setTelefon(kontaktInfoDTO.telefon());
        kontaktinfo.setRelasjon(Relasjon.valueOf(kontaktInfoDTO.relasjon()));
        kontaktinfo.setPrioritet(kontaktInfoDTO.prioritet());
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
    @ApiResponses(
        {
            @ApiResponse(responseCode = "201", description = "Opprettet kontaktinfo"),
            @ApiResponse(responseCode = "401", description = "Uautentisert"),
            @ApiResponse(responseCode = "403", description = "Uatorisert")
        }
    )
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
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "401", description = "Uautentisert"),
            @ApiResponse(responseCode = "403", description = "Uatorisert")
        }
    )
    public void markForDeletion(@PathVariable String kontaktId) throws AccessDeniedException {
        var kontaktinfo = kontaktInfoRepository.findDistinctByKontakId(kontaktId)
            .orElseThrow(() -> new AccessDeniedException("Unauthorized"));

        securityUtil.assertAuthorized(kontaktinfo.getNavident());

        kontaktinfo.setDatoSlettet(LocalDate.now());

        kontaktInfoRepository.save(kontaktinfo);
    }

    @PostMapping("/ressurser")
    @Operation(
        description = "Henter kontaktinfo til pårørende for gitt ressurser - brukes av admin og ledere",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "201", description = "Hentet kontaktinfo"),
            @ApiResponse(responseCode = "401", description = "Uautentisert"),
            @ApiResponse(responseCode = "403", description = "Uatorisert")
        }
    )
    public ResponseEntity<List<RessursKontaktinfoDTO>> getContaktinfoForRessurser(@RequestBody List<String> ressursNavidents) {
        /*
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        JwtAuthenticationToken oauthToken = (JwtAuthenticationToken) authentication;
        String onBehalfOfNavident = String.valueOf(oauthToken.getTokenAttributes().get("NAVident"));
        */

        String onBehalfOfNavident = ((Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getClaimAsString("NAVIDENT");
        log.info("Get Contaktinfo For Ressurser on behalf of: {}", onBehalfOfNavident);
        if (onBehalfOfNavident == null) {
            throw new AccessDeniedException("Unauthorized access to ressurser kontaktinfo");
        }

        // get contact info for ressurser
        List<RessursKontaktinfoDTO> result = new ArrayList<>();
        ressursNavidents.forEach(ressursNavident -> {
            // do authorization check
            securityUtil.assertAuthorizedAccessTo(ressursNavident);

            var kontaktInfo = KontaktinfoDTO.fromEntityList(kontaktInfoRepository.findAllByNavidentAndDatoSlettetIsNull(ressursNavident));
            RessursKontaktinfoDTO ressursKontaktinfoDTO = RessursKontaktinfoDTO.builder()
                .ressursNavident(ressursNavident)
                .kontaktinfo(kontaktInfo)
                .build();
            result.add(ressursKontaktinfoDTO);
        });
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
