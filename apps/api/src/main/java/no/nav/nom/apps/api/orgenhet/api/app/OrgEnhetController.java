package no.nav.nom.apps.api.orgenhet.api.app;

import io.getunleash.Unleash;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.authorize.AuthResolver;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.common.security.NomApiAccessScope;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.organisering.exceptions.OrganiseringNotFoundException;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.api.app.dto.AvvikleOrgEnhetRequest;
import no.nav.nom.apps.api.orgenhet.api.app.dto.KorrigerOrgEnhetNavnRequest;
import no.nav.nom.apps.api.orgenhet.api.app.dto.OpprettOrgEnhetRequest;
import no.nav.nom.apps.api.orgenhet.api.app.dto.OpprettOrgEnhetResponse;
import no.nav.nom.apps.api.orgenhet.api.app.dto.OrgEnhet;
import no.nav.nom.apps.api.orgenhet.api.app.dto.OrgEnhetMedLedernavn;
import no.nav.nom.apps.api.orgenhet.api.app.dto.OvrigeEndringerOrgEnhetRequest;
import no.nav.nom.apps.api.orgenhet.exceptions.DuplicateOrgEnhetNameException;
import no.nav.nom.apps.api.orgenhet.exceptions.OrgEnhetNotFoundException;
import no.nav.nom.apps.api.orgenhetleder.OrgEnhetLederService;
import no.nav.nom.apps.api.ressurs.api.exceptions.RessursNotFoundException;
import no.nav.nom.apps.api.rest.common.StandardResponse;
import no.nav.nom.apps.api.topicreader.ReadTopicRequest;
import no.nav.nom.libs.utils.common.excel.ExcelGenerator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

@Slf4j
@Tag(name = "OrgEnheter")
@RestController
@RequestMapping(value = {"/app/orgenheter"})
public class OrgEnhetController {
    private final OrgenhetService orgenhetService;
    private final OrgEnhetRestService orgEnhetRestService;
    private final OrgEnhetLederService orgEnhetLederService;
    private final OrgEnhetMapper orgEnhetMapper;
    private final KontaktpersonerService kontaktpersonerService;
    private final AuthResolver authResolver;
    private final ExcelGenerator excelGenerator;
    private final Unleash unleash;

    public OrgEnhetController(
        OrgenhetService orgenhetService,
        OrgEnhetRestService orgEnhetRestService,
        OrgEnhetLederService orgEnhetLederService,
        OrgEnhetMapper orgEnhetMapper,
        KontaktpersonerService kontaktpersonerService,
        AuthResolver authResolver,
        Unleash unleash
    ) {
        this.orgenhetService = orgenhetService;
        this.orgEnhetRestService = orgEnhetRestService;
        this.orgEnhetLederService = orgEnhetLederService;
        this.orgEnhetMapper = orgEnhetMapper;
        this.kontaktpersonerService = kontaktpersonerService;
        this.authResolver = authResolver;
        this.excelGenerator = new ExcelGenerator();
        this.unleash = unleash;
    }

    @GetMapping("/{orgenhetId}")
    @Operation(
        description = "Henter orgenhet med gitt id",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    public ResponseEntity<OrgEnhet> hentOrgenhet(@PathVariable String orgenhetId) {

        return orgenhetService.getById(orgenhetId)
            .map(orgEnhetMapper::mapToOrgEnhet)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping()
    @Operation(
        description = "Henter orgenheter med for liste av id'er",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200"),
        }
    )
    public ResponseEntity<List<OrgEnhet>> getOrgenheterForIder(
        @RequestBody List<String> orgenhetIder
    ) {
        return ResponseEntity.ok(
            orgenhetService.getByIdBulk(orgenhetIder).values().stream()
                .map(orgEnhetMapper::mapToOrgEnhet).toList()
        );
    }


    @PostMapping("/isNavIdent/{navident}/canEditOrgenhet/{orgEnhetId}")
    @Operation(
        description = "Sjekk om angitt navident er leder for angitt orgenhet.",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    public ResponseEntity<Boolean> kanRessursRedigereOrgenhet(
        @PathVariable String navident,
        @PathVariable String orgEnhetId
    ) {
        OrgEnhet orgEnhet = orgenhetService.getById(orgEnhetId)
            .map(orgEnhetMapper::mapToOrgEnhet)
            .orElse(null);

        if (orgEnhet != null) {
            boolean kanRedigereOrgenhet = authResolver.kanRessursRedigereOrgenhet(navident, orgEnhetId);
            log.info("kanRessursRedigereOrgenhet navident orgEnhetId result: {}-{}-{}", navident, orgEnhetId, kanRedigereOrgenhet);
            return ResponseEntity.ok(kanRedigereOrgenhet);
        }

        return ResponseEntity.ok(false);
    }

    @GetMapping("/exportKontaktpersonerFor/{orgenhetId}")
    @Operation(
        description = "Genererer regneark med oversikt over kontaktpersoner for en orgenhet",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Exported file"),
        }
    )
    public void exportKontaktpersonerForOrgenhetId(HttpServletResponse response, @PathVariable String orgenhetId) throws IOException {

        var loggedInUser = SecurityUtil.getAuthenticatedNavIdent();
        var isAdminToEnhet = SecurityUtil.loggedInUserHasAccessScope(NomApiAccessScope.ADMIN);
        boolean kanRedigereOrgenhet = authResolver.kanRessursRedigereOrgenhet(loggedInUser, orgenhetId);
        if (!isAdminToEnhet && !kanRedigereOrgenhet) {
            var accessDeniedError = String.format("Bruker %s har ikke tilgang til å se ressurser og kontaktpersoner for orgenhet %s", loggedInUser, orgenhetId);
            log.error(accessDeniedError);
            throw new AccessDeniedException(accessDeniedError);
        }

        kontaktpersonerService.generateExcelFile(response, orgenhetId);

    }

    @GetMapping("/kontaktpersoner/{orgenhetId}")
    public List<RessursKontaktperson> getKontaktpersonerForOrgenhetId(@PathVariable String orgenhetId) {
        var loggedInUser = SecurityUtil.getAuthenticatedNavIdent();
        var isAdminToEnhet = SecurityUtil.loggedInUserHasAccessScope(NomApiAccessScope.ADMIN);
        boolean kanRedigereOrgenhet = authResolver.kanRessursRedigereOrgenhet(loggedInUser, orgenhetId);

        if (!isAdminToEnhet && !kanRedigereOrgenhet) {
            var accessDeniedError = String.format("Bruker %s har ikke tilgang til å se ressurser og kontaktpersoner for orgenhet %s", loggedInUser, orgenhetId);
            log.error(accessDeniedError);
            throw new AccessDeniedException(accessDeniedError);
        }

        return kontaktpersonerService.getKontaktpersonerForRessurserUnder(orgenhetId);
    }

    @GetMapping
    @Operation(
        description = "Henter alle orgenheter",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    public List<OrgEnhet> hentOrgEnheter() {
        return orgenhetService.getAllEnheter().stream().map(orgEnhetMapper::mapToOrgEnhet).collect(Collectors.toList());
    }

    @GetMapping("/getRemedyEnhetId/{orgenhetId}")
    @Operation(
        description = "Henter remedy id til en orgenhet med gitt id",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    public ResponseEntity<String> getRemedyEnhetId(@PathVariable String orgenhetId) {
        return ResponseEntity.ok(orgEnhetMapper.getRemedyEnhetId(orgenhetId));
    }

    @GetMapping("/kanHaRessurser")
    @Operation(
        description = "Henter alle aktive orgenheter (har overenhet) som er løvnoder (ikke har underenheter)",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    public List<OrgEnhetMedLedernavn> hentOrgEnheterMedLeder() {
        return orgenhetService.getAllOrgEnheterThatCanHaveRessurser().stream()
            .map(orgEnhetMapper::mapToOrgEnhetMedLedernavn)
            .collect(Collectors.toList());
    }

    @GetMapping("/ledetAv/{navident}")
    @Operation(
        description = "Henter alle orgenheter som er ledet av en gitt leder",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    public List<OrgEnhet> hentOrgEnheterByLeder(@PathVariable String navident) {
        return orgEnhetLederService.getOrgEnheterNavidentErLederFor(navident)
            .stream()
            .map(orgEnhetLederState ->
                orgenhetService.getById(orgEnhetLederState.orgEnhetId())
                    .orElseThrow(() -> OrgEnhetNotFoundException.id(orgEnhetLederState.orgEnhetId())))
            .map(orgEnhetMapper::mapToOrgEnhet)
            .toList();
    }

    @GetMapping("/underliggende/{parentId}")
    @Operation(
        description = "Henter ut id for alle underliggende organisasjonsenheter",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    public ResponseEntity<List<String>> getUnderliggendeOrgEnheter(@PathVariable String parentId) {
        return ResponseEntity.ok(orgenhetService.findAllUnderliggendeOrgenheter(parentId));
    }

    @GetMapping("/alle-underliggende/{parentId}")
    @Operation(
        description = "Henter ut objekt for alle underliggende organisasjonsenheter",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    public ResponseEntity<List<OrgEnhet>> getUnderliggendeOrgEnheterObjekt(@PathVariable String parentId) {
        return ResponseEntity.ok(orgenhetService.getAllUnderliggendeOrgEnhetById(parentId)
            .stream()
            .map(orgEnhetMapper::mapToOrgEnhet)
            .collect(Collectors.toList()));
    }

    @Operation(
        description = "Henter ut alle orgEnheter i tillegg til parent orgEnhet",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @GetMapping("/parent-og-underliggende/{parentId}")
    public ResponseEntity<List<OrgEnhet>> getAlleUnderLiggendeOrgEnheterOgParent(@PathVariable String parentId) {
        var parentOrgEnhet = orgenhetService.getById(parentId)
            .map(orgEnhetMapper::mapToReducedOrgEnhet)
            .orElseThrow(() -> OrgEnhetNotFoundException.id(parentId));
        var underliggendeOrgEnheter = orgenhetService.getAllUnderliggendeOrgEnhetById(parentId)
            .stream().map(orgEnhetMapper::mapToReducedOrgEnhet).collect(Collectors.toList());
        underliggendeOrgEnheter.add(parentOrgEnhet);
        return ResponseEntity.ok(underliggendeOrgEnheter);
    }

    @Operation(
        description = "Excel export av alle orgEnhet i tillegg til parent orgEnhet",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @GetMapping("parent-og-underliggende-excel/{parentId}")
    public void getAlleUnderLiggendeOrgEnheterOgParentExcel(HttpServletResponse response, @PathVariable String parentId) throws IOException {
        var parentOrgEnhet = orgenhetService.getById(parentId)
            .map(orgEnhetMapper::mapToReducedOrgEnhet)
            .orElseThrow(() -> OrgEnhetNotFoundException.id(parentId));
        var underliggendeOrgEnheter = orgenhetService.getAllUnderliggendeOrgEnhetById(parentId)
            .stream().map(orgEnhetMapper::mapToReducedOrgEnhet).collect(Collectors.toList());
        underliggendeOrgEnheter.addFirst(parentOrgEnhet);
        List<Map<String, Object>> result = new ArrayList<>();
        for (OrgEnhet orgEnhet : underliggendeOrgEnheter) {
            LinkedHashMap<String, Object> map = new LinkedHashMap<>();
            map.put("Org Enhet ID", orgEnhet.id());
            map.put("Agresso ID", orgEnhet.agressoId());
            map.put("Agresso Org Nivå", orgEnhet.orgNiv());
            map.put("Agresso Org Enhet Type", nonNull(orgEnhet.agressoOrgenhetType()) ? orgEnhet.agressoOrgenhetType() : "");
            map.put("Org Enhet Navn", orgEnhet.navn());
            map.put("Gyldig FOM", orgEnhet.gyldigFom());
            map.put("Gyldig TOM", nonNull(orgEnhet.gyldigTom()) ? orgEnhet.gyldigTom() : "");
            map.put("Leder Navident", orgEnhet.leder().navident());
            map.put("Leder Navn", orgEnhet.leder().visningsnavn());
            map.put("Org Enhet Nivå", nonNull(orgEnhet.nomNivaa()) ? orgEnhet.nomNivaa() : "");
            map.put("Org Enhet Type", nonNull(orgEnhet.orgEnhetsType()) ? orgEnhet.orgEnhetsType() : "");
            map.put("Org Enhet ID Over", orgEnhet.overordnetEnhetId());
            map.put("Remedy Enhet ID", orgEnhet.remedyEnhetId());
            map.put("Tilgangsenhet ID", orgEnhet.tilgangsenhetId());
            result.add(map);
        }

        var excelHeaders = result.getFirst().keySet().stream().toList();
        var filename = String.format(
            "OrgEnheter underliggende %s-%s",
            parentOrgEnhet.navn(),
            LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
        );
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, String.format("attachment; filename=%s.xlsx", filename));
        excelGenerator.generateWorkbook(response, parentOrgEnhet.id(), excelHeaders, result);
    }

    @GetMapping("/isArbeidsomraade/{orgEnhetId}")
    @Operation(
        description = "Sjekk om Organisasjonen tilhøerer et arbeidsområde.",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    public ResponseEntity<Boolean> checkIfOrgEnhetIsArbeidsomraade(@PathVariable String orgEnhetId) {
        return ResponseEntity.ok(orgenhetService.isOrgEnhetInArbeidsomraade(orgEnhetId));
    }

    @GetMapping("/isDriftsenhet/{orgEnhetId}")
    @Operation(
        description = "Sjekk om Organisasjonen tilhøerer et arbeidsområde.",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    public ResponseEntity<Boolean> checkIfOrgEnhetIsDriftsenhet(@PathVariable String orgEnhetId) {
        return ResponseEntity.ok(orgenhetService.isOrgenhetInDriftsenhet(orgEnhetId));
    }

    @GetMapping("/read-topic")
    @Operation(
        description = "Henter records fra ressurs event-source",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<JsonNode>> readRessursTopic(@Valid ReadTopicRequest request) {
        return ResponseEntity.ok(orgenhetService.getOrgEnhetEvents(request));
    }

    @PostMapping("/orgEnhetsType")
    @Operation(
        description = "Sett orgEnhetsType for en organisasjonsenhet",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<StandardResponse> setOrgEnhetsType(@RequestBody SetOrgEnhetsTypeRequest setOrgEnhetsTypeRequest) {
        return orgenhetService.setOrgEnhetsType(setOrgEnhetsTypeRequest);
    }


    @PostMapping("/opprett")
    @Operation(
        description = "Opprett ny orgenhet",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<OpprettOrgEnhetResponse> opprettOrgEnhet(@Valid @RequestBody OpprettOrgEnhetRequest opprettOrgEnhetRequest) {
        if (unleash.isEnabled("nom-api.orgenhet-mastring"))
            return ResponseEntity.status(HttpStatus.CREATED).body(orgEnhetRestService.opprettOrgEnhet(opprettOrgEnhetRequest));
        else
            return ResponseEntity.status(HttpStatus.CREATED).body(new OpprettOrgEnhetResponse(
                "FID-1",
                "ab123c",
                opprettOrgEnhetRequest.navn(),
                opprettOrgEnhetRequest.overordnetOrgEnhetId(),
                "TEST",
                opprettOrgEnhetRequest.gyldigFom(),
                null,
                opprettOrgEnhetRequest.lederRessursNavident(),
                "Nav Navesen",
                opprettOrgEnhetRequest.tilgangsenhetId(),
                opprettOrgEnhetRequest.orgEnhetsType()
            ));

    }

    @Operation(
        description = "Korriger navn på OrgEnhet",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @PreAuthorize("hasAuthority('ADMIN')")
    @PatchMapping("/korrigerNavn")
    public ResponseEntity<StandardResponse> korrigerOrgEnhetNavn(@RequestBody KorrigerOrgEnhetNavnRequest korrigerOrgEnhetNavnRequest) {
        if (unleash.isEnabled("nom-api.orgenhet-mastring")) {
            return ResponseEntity.status(HttpStatus.OK)
                .body(orgEnhetRestService.korrigerOrgEnhetNavn(korrigerOrgEnhetNavnRequest));
        } else {
            return ResponseEntity.ok().build();
        }
    }

    @Operation(
        description = "Endre øvrige info i orgEnhet",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @PreAuthorize("hasAuthority('ADMIN')")
    @PatchMapping("/endreOvrige")
    public ResponseEntity<StandardResponse> overigeEndringerOrgEnhet(@RequestBody OvrigeEndringerOrgEnhetRequest ovrigeEndringerOrgEnhetRequest) {
        if (unleash.isEnabled("nom-api.orgenhet-mastring")) {
            return ResponseEntity.status(HttpStatus.OK)
                .body(StandardResponse.builder().message(orgEnhetRestService.ovrigeEndringerOrgEnhet(ovrigeEndringerOrgEnhetRequest)).build());
        } else {
            return ResponseEntity.ok().build();
        }
    }

    @Operation(
        description = "Avvikle en OrgEnhet",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @PreAuthorize("hasAuthority('ADMIN')")
    @PatchMapping("/avvikle/{orgEnhetId}")
    public ResponseEntity<StandardResponse> avvikleOrgEnhet(@PathVariable String orgEnhetId, @RequestBody AvvikleOrgEnhetRequest avvikleOrgEnhetRequest) {
        if (unleash.isEnabled("nom-api.orgenhet-mastring")) {
            return ResponseEntity.status(HttpStatus.OK)
                .body(orgEnhetRestService.avvikleOrgEnhet(orgEnhetId, avvikleOrgEnhetRequest));
        } else {
            return ResponseEntity.status(HttpStatus.OK)
                .body(
                    StandardResponse.builder()
                        .message(String.format("Enhet har fått satt sluttdato til %s", LocalDate.now()))
                        .build());
        }
    }

    @ExceptionHandler({
        OrgEnhetNotFoundException.class,
        RessursNotFoundException.class,
        OrganiseringNotFoundException.class
    })
    public ResponseEntity<StandardResponse> handleBadRequest(RuntimeException e) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(StandardResponse.builder().message(e.getMessage()).build());
    }

    @ExceptionHandler({
        IllegalStateException.class
    })
    public ResponseEntity<StandardResponse> handleUnprocessableEntity(RuntimeException e) {
        return ResponseEntity
            .status(HttpStatus.UNPROCESSABLE_CONTENT)
            .body(StandardResponse.builder().message(e.getMessage()).build());
    }

    @ExceptionHandler({
        DuplicateOrgEnhetNameException.class
    })
    public ResponseEntity<StandardResponse> handleDuplicateOrgEnhetName(RuntimeException e) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(StandardResponse.builder().message(e.getMessage()).build());
    }

    @ExceptionHandler(
        {
            IOException.class
        }
    )
    public ResponseEntity<StandardResponse> handleIOException(RuntimeException e) {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(StandardResponse.builder().message(e.getMessage()).build());
    }

}