package no.nav.nom.apps.api.orgenhet.api.app;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import no.nav.nom.apps.api.authorize.AuthResolver;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.api.app.dto.OrgEnhet;
import no.nav.nom.apps.api.orgenhet.api.app.dto.OrgEnhetMedLedernavn;
import no.nav.nom.apps.api.ressurs.api.app.RessursRestService;
import no.nav.nom.apps.api.ressurs.api.dto.Ressurs;
import no.nav.nom.apps.api.rest.common.StandardResponse;
import no.nav.nom.apps.api.topicreader.ReadTopicRequest;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static no.nav.nom.apps.api.common.DateUtils.isPeriodActive;
import static no.nav.nom.apps.api.common.DateUtils.isPeriodFuture;

@Tag(name = "OrgEnheter")
@RestController
@RequestMapping(value = {"/app/orgenheter"})
public class OrgEnhetController {

    private final static Logger log = LoggerFactory.getLogger(OrgEnhetController.class);
    private final OrgenhetService orgenhetService;
    private final OrgEnhetMapper orgEnhetMapper;
    private final KontaktpersonerExcelGenerator kontaktpersonerExcelGenerator;
    private final RessursRestService ressursRestService;
    private final AuthResolver authResolver;

    public OrgEnhetController(
            OrgenhetService orgenhetService,
            OrgEnhetMapper orgEnhetMapper,
            KontaktpersonerExcelGenerator kontaktpersonerExcelGenerator,
            RessursRestService ressursRestService,
            AuthResolver authResolver
    ) {
        this.orgenhetService = orgenhetService;
        this.orgEnhetMapper = orgEnhetMapper;
        this.kontaktpersonerExcelGenerator = kontaktpersonerExcelGenerator;
        this.ressursRestService = ressursRestService;
        this.authResolver = authResolver;
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

    @GetMapping("/checkIfBrukerKanExportKontaktpersonerForOrgenhet/{orgEnhetId}")
    @Operation(
        description = "Sjekk om bruker kan export kontakt personer for gitt orgenhet.",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    public ResponseEntity<Boolean> checkIfBrukerKanExportKontaktpersonerForOrgenhet(@PathVariable("orgEnhetId") String orgEnhetId) {
        return ResponseEntity.ok(authResolver.kanOppretteRessurs(orgEnhetId));
    }


    @GetMapping("/exportKontaktpersonerFor/{orgenhetId}")
    @Operation(
        description = "Genererer regneark med oversikt over kontakt personer for en orgenhet",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Exported file"),
    })
    public void exportKontaktpersonerForOrgenhetId(HttpServletResponse response, @PathVariable String orgenhetId) throws IOException {

        SecurityUtil.printTestToken("OrgEnhetController_");

        try {
            var orgEnhet = orgenhetService.getById(orgenhetId)
                .map(orgEnhetMapper::mapToOrgEnhet);

            if(orgEnhet.isEmpty()) {
                String notFoundError = "Orgenhet not found! " + orgenhetId;
                log.error(notFoundError);
                response.sendError(404, notFoundError);
                return;
            }

            var loggedInUser = SecurityUtil.getAuthenticatedNavIdent();
            var hasAdminRightsForEnhet = authResolver.kanOppretteRessurs(orgenhetId);
            if(!hasAdminRightsForEnhet) {
                var accessDeniedError = "User does not have access to view ressurser for this orgenhet: " + loggedInUser + "-" + orgenhetId;
                log.error(accessDeniedError);
                throw new AccessDeniedException(accessDeniedError);
            }

            log.info("User have access to view ressurser for this orgenhet: {}-{}", loggedInUser, orgenhetId);
            var sortedRessurser = getSortedRessurserInAndUnderliggendeOrgenheter(orgenhetId);

            log.info("Ressurser are retrivedfor this orgenhet: {}-{}", sortedRessurser, orgenhetId);

            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=kontaktpersoner.xlsx");
            kontaktpersonerExcelGenerator.generate(response, orgEnhet.get(), sortedRessurser);
        } catch (Exception e) {
            log.error("Failed to generate export file in exportKontaktpersonerForOrgenhetId. " + e.getMessage());
            response.sendError(500);
        }
    }

    @GetMapping
    @Operation(
        description = "Henter alle orgenheter",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    public List<OrgEnhet> hentOrgEnheter() {
        return orgenhetService.getAllEnheter().stream().map(orgEnhetMapper::mapToOrgEnhet).collect(Collectors.toList());
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
        return orgenhetService.getEnheterByLeder(navident).stream().map(orgEnhetMapper::mapToOrgEnhet).collect(Collectors.toList());
    }

    @GetMapping("/underliggende/{parentId}")
    @Operation(
        description = "Henter ut id for alle underliggende organisasjonsenheter",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    public ResponseEntity<List<String>> getUnderliggendeOrgEnheter(@PathVariable("parentId") String parentId) {
        return ResponseEntity.ok(orgenhetService.findAllUnderliggendeOrgenheter(parentId));
    }

    @GetMapping("/alle-underliggende/{parentId}")
    @Operation(
        description = "Henter ut objekt for alle underliggende organisasjonsenheter",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    public ResponseEntity<List<OrgEnhet>> getUnderliggendeOrgEnheterObjekt(@PathVariable("parentId") String parentId) {
        return ResponseEntity.ok(orgenhetService.getAllUnderliggendeOrgEnhetById(parentId).stream().map(orgEnhetMapper::mapToOrgEnhet).collect(Collectors.toList()));
    }

    @GetMapping("/isArbeidsomraade/{orgEnhetId}")
    @Operation(
        description = "Sjekk om Organisasjonen tilhøerer et arbeidsområde.",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    public ResponseEntity<Boolean> checkIfOrgEnhetIsArbeidsomraade(@PathVariable("orgEnhetId") String orgEnhetId) {
        return ResponseEntity.ok(orgenhetService.isOrgEnhetInArbeidsomraade(orgEnhetId));
    }

    @GetMapping("/isDriftsenhet/{orgEnhetId}")
    @Operation(
        description = "Sjekk om Organisasjonen tilhøerer et arbeidsområde.",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    public ResponseEntity<Boolean> checkIfOrgEnhetIsDriftsenhet(@PathVariable("orgEnhetId") String orgEnhetId) {
        return ResponseEntity.ok(orgenhetService.isOrgenhetInDriftsenhet(orgEnhetId));
    }

    @GetMapping("/read-topic")
    @Operation(
            description = "Henter records fra ressurs event-source",
            security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @PreAuthorize("hasAuthority('ADMIN')")
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

    private List<Ressurs> getSortedRessurserInAndUnderliggendeOrgenheter(String orgenhetId) {
        List<OrgtilknytningState> combinedRessursKoblinger  = new ArrayList<>();

        // get all ressurser under orgenhet
        Optional<OrgEnhet> orgEnhet = orgenhetService.getById(orgenhetId)
            .map(orgEnhetMapper::mapToOrgEnhet);
        orgEnhet.ifPresent(orgEnhet1 -> combinedRessursKoblinger.addAll(orgEnhet1.ressursKoblinger()));

        // first get all underlyinh enheter
        List<no.nav.nom.apps.api.orgenhet.api.app.dto.OrgEnhet> underEnheter = new ArrayList<>(orgenhetService.getAllUnderliggendeOrgEnhetById(orgenhetId)
            .stream()
            .map(orgEnhetMapper::mapToOrgEnhet)
            .toList());
        underEnheter.forEach(underEnhet -> combinedRessursKoblinger.addAll(underEnhet.ressursKoblinger()));

        // then get active ressurser under each underliggende enhet
        List<Ressurs> ressurser = combinedRessursKoblinger.stream()
            .filter(orgtilknytningState -> isPeriodActive(orgtilknytningState.gyldigFom(), orgtilknytningState.gyldigTom()) ||
                isPeriodFuture(orgtilknytningState.gyldigFom(), orgtilknytningState.gyldigTom()))
            .map(orgtilknytningState ->  ressursRestService.findRessursByNavident(orgtilknytningState.navident()))
            .toList();

        // sort ressurser by fornavn and etternavn
        List<Ressurs> sortedRessurser = ressurser.stream().sorted((r1, r2) -> {
            var n1 = r1.state().visningsnavn();
            var n2 = r2.state().visningsnavn();
            return n1.fornavn().compareTo(n2.fornavn()) == 0 ? n1.etternavn().compareTo(n2.etternavn()) : n1.fornavn().compareTo(n2.fornavn());
        }).toList();
        return sortedRessurser;
    }
}