package no.nav.nom.apps.api.rest.uttrekk;

import io.getunleash.Unleash;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.api.clients.slack.SlackService;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.graphql.propertyresolvers.RemedyEnhetIdResolver;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.rest.common.StandardResponse;
import no.nav.nom.apps.api.telefon.TelefonService;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import no.nav.nom.libs.state.orgenhetleder.OrgEnhetLederStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.likEllerEtter;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.today;

@Tag(name = "Omada uttrekk")
@RestController
@RequestMapping("/api/v1/uttrekk/omada")
public class OmadaUttrekk {
    private final String EXCLUDE_ORG_ENHET = "bu282j"; // bu282j;0000000;ORGENHET;;Deltar i Rekruttering;01-05-2019;;;;
    private final Logger log = LoggerFactory.getLogger(OmadaUttrekk.class);
    private final RessursStateRepository ressursStateRepository;
    private final OrgenhetService orgenhetService;
    private final OrgtilknytningService orgtilknytningService;
    private final RemedyEnhetIdResolver remedyEnhetIdResolver;
    private final OrganiseringStateRepository organiseringStateRepository;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private final TelefonService telefonService;
    private final OrgEnhetLederStateRepository orgEnhetLederStateRepository;
    private final Unleash unleash;
    private final AppProperties.Orgenhet orgenhetProperties;
    private final SlackService slackService;
    private volatile LocalDateTime lastCleared = LocalDateTime.now();

    public static final String UNLEASH_OMADA_NYTT_FORMAT = "nom.omada.nytt_format";
    private static final String ORGENHET_KUN_HISTORISK_SLUTTDATO = "nom.omada.kun_historisk_sluttdato";

    private AtomicBoolean diffDvhVsLederRepo = new AtomicBoolean(false);
    private final AtomicBoolean slackSentForCurrentCycle = new AtomicBoolean(false);
    private final Map<String, Integer> orgEnhetMedRessurserUtenforHierarki = new ConcurrentHashMap<>();

    public OmadaUttrekk(
        RessursStateRepository ressursStateRepository,
        OrgenhetService orgenhetService,
        OrgtilknytningService orgtilknytningService,
        RemedyEnhetIdResolver remedyEnhetIdResolver,
        OrganiseringStateRepository organiseringStateRepository,
        TelefonService telefonService,
        OrgEnhetLederStateRepository orgEnhetLederStateRepository,
        Unleash unleash,
        AppProperties appProperties,
        SlackService slackService
    ) {
        this.ressursStateRepository = ressursStateRepository;
        this.orgenhetService = orgenhetService;
        this.orgtilknytningService = orgtilknytningService;
        this.remedyEnhetIdResolver = remedyEnhetIdResolver;
        this.organiseringStateRepository = organiseringStateRepository;
        this.telefonService = telefonService;
        this.orgEnhetLederStateRepository = orgEnhetLederStateRepository;
        this.unleash = unleash;
        this.orgenhetProperties = appProperties.orgenhet();
        this.slackService = slackService;
    }

    @Operation(
        description = "Returnerer csv med alle ressurser",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/ressurs", produces = "text/csv")
    public @ResponseBody byte[] exportRessurs() throws IOException {
        if (lastCleared.isBefore(LocalDateTime.now().minusMinutes(45L)) || orgEnhetMedRessurserUtenforHierarki.isEmpty()) {
            ressurserUtenforHierarki();
        }

        if (!orgEnhetMedRessurserUtenforHierarki.isEmpty()) {
            var antallUtenforHierarki = orgEnhetMedRessurserUtenforHierarki.size();
            var antallRessurserUtenforHierarki = orgEnhetMedRessurserUtenforHierarki.values().stream().mapToInt(Integer::intValue).sum();
            log.error("Kunne ikke generere uttrekk, det er {} enheter med {} ansatte utenfor hierarki.", antallUtenforHierarki, antallRessurserUtenforHierarki);
            sendSlackMessage();
            throw new OrgtilknytningUtenforHierarkiException(String.format(
                "Kunne ikke generere uttrekk, det er %s enheter med %s ansatte utenfor hierarki.",
                antallUtenforHierarki,
                antallRessurserUtenforHierarki
            ));
        }
        StringWriter writer = new StringWriter();

        var brukNyttFormat = unleash.isEnabled(UNLEASH_OMADA_NYTT_FORMAT);

        final var cSVFormat =
            brukNyttFormat
             ? getCSVFormat("nomId", "navident", "fornavn", "mellomnavn", "etternavn", "visningsnavn", "sektor", "sluttdato", "startdato", "personident", "identtype", "tilknytningtype", "primaryTelefonnummer")
             : getCSVFormat("nomId", "navident", "fornavn", "mellomnavn", "etternavn", "visningsnavn", "sektor", "sluttdato", "startdato");

        try (CSVPrinter printer = new CSVPrinter(writer, cSVFormat)) {
            ressursStateRepository.getAll().forEach(ressurs -> {
                try {
                    if (brukNyttFormat) {
                        printer.printRecord(
                            ressurs.fid(),  
                            ressurs.navident(),
                            ressurs.visningsnavn().fornavn().trim(),
                            ressurs.visningsnavn().mellomnavn().filter(s -> !s.isBlank()).orElse(""),
                            ressurs.visningsnavn().etternavn().trim(),
                            ressurs.visningsnavn().getFormattedVisningsnavn(),
                            ressurs.sektor(),
                            ressurs.sluttdato() != null ? ressurs.sluttdato().format(dateTimeFormatter) : "",
                            ressurs.startdato() != null ? ressurs.startdato().format(dateTimeFormatter) : "",
                            ressurs.personident(),
                            ressurs.identType() != null ? ressurs.identType().name() : "",
                            ressurs.tilknytningtype() != null ? ressurs.tilknytningtype().name() : "",
                            getPrimaryTelefonnummer(ressurs)
                        );
                    } else {
                        printer.printRecord(
                            ressurs.fid(),
                            ressurs.navident(),
                            ressurs.visningsnavn().fornavn().trim(),
                            ressurs.visningsnavn().mellomnavn().filter(s -> !s.isBlank()).orElse(""),
                            ressurs.visningsnavn().etternavn().trim(),
                            ressurs.visningsnavn().getFormattedVisningsnavn(),
                            ressurs.sektor(),
                            ressurs.sluttdato() != null ? ressurs.sluttdato().format(dateTimeFormatter) : "",
                            ressurs.startdato() != null ? ressurs.startdato().format(dateTimeFormatter) : ""
                        );
                    }

                    writer.close();
                } catch (IOException e) {
                    log.error("Error writing to CSV", e);
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Kunne ikke generere uttrekk");
                }
            });
            return IOUtils.toByteArray(IOUtils.toInputStream(writer.toString(), "UTF-8"));
        }
    }

    @Operation(
        description = "Returnerer csv med alle orgtilknytninger for ressurser",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/orgtilknytning", produces = "text/csv")
    public @ResponseBody byte[] exportOrgtilknytning(
        @RequestParam(required = false, defaultValue = "false") boolean includeDagligOppfolging
    ) throws IOException {
        if (lastCleared.isBefore(LocalDateTime.now().minusMinutes(45L)) || orgEnhetMedRessurserUtenforHierarki.isEmpty()) {
            ressurserUtenforHierarki();
        }

        if (!orgEnhetMedRessurserUtenforHierarki.isEmpty()) {
            var antallUtenforHierarki = orgEnhetMedRessurserUtenforHierarki.size();
            var antallRessurserUtenforHierarki = orgEnhetMedRessurserUtenforHierarki.values().stream().mapToInt(Integer::intValue).sum();
            log.error("Kunne ikke generere uttrekk, det er {} enheter med {} ansatte utenfor hierarki.", antallUtenforHierarki, antallRessurserUtenforHierarki);
            sendSlackMessage();
            throw new OrgtilknytningUtenforHierarkiException(String.format(
                "Kunne ikke generere uttrekk, det er %s enheter med %s ansatte utenfor hierarki.",
                antallUtenforHierarki,
                antallRessurserUtenforHierarki
            ));
        }
        StringWriter writer = new StringWriter();

        var brukNyttFormat = unleash.isEnabled(UNLEASH_OMADA_NYTT_FORMAT);

        final var cSVFormat = brukNyttFormat ?
            getCSVFormat(
                "navident",
                "org_enhet_id",
                "fid",
                "erDagligOppfolging",
                "gyldig_fom",
                "gyldig_tom",
                "masseflyttet_fra_org_enhet_id",
                "sektor",
                "tilknytningtype"
            )
            : getCSVFormat(
                "navident",
                "org_enhet_id",
                "fid",
                "erDagligOppfolging",
                "gyldig_fom",
                "gyldig_tom",
                "masseflyttet_fra_org_enhet_id"
            );

        try (CSVPrinter printer = new CSVPrinter(writer, cSVFormat)) {
            orgtilknytningService.getAll()
                .stream().filter(ot -> !ot.orgEnhetId().equals(EXCLUDE_ORG_ENHET))
                .forEach(kobling -> {
                    try {
                        if (brukNyttFormat) {
                            printer.printRecord(
                                kobling.navident(),
                                kobling.orgEnhetId(),
                                kobling.fid(),
                                kobling.erDagligOppfolging(),
                                kobling.gyldigFom().format(dateTimeFormatter),
                                nonNull(kobling.gyldigTom()) ? kobling.gyldigTom().format(dateTimeFormatter) : "",
                                Objects.requireNonNullElse(kobling.masseflyttetFraFid(), ""),
                                kobling.sektor() != null ? kobling.sektor() : "",
                                kobling.orgtilknytningType() != null ? kobling.orgtilknytningType() : ""
                            );
                        } else {
                            printer.printRecord(
                                kobling.navident(),
                                kobling.orgEnhetId(),
                                kobling.fid(),
                                kobling.erDagligOppfolging(),
                                kobling.gyldigFom().format(dateTimeFormatter),
                                nonNull(kobling.gyldigTom()) ? kobling.gyldigTom().format(dateTimeFormatter) : "",
                                Objects.requireNonNullElse(kobling.masseflyttetFraFid(), "")
                            );
                        }

                        writer.close();
                    } catch (IOException e) {
                        log.error("Error writing to CSV", e);
                        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Kunne ikke generere uttrekk");
                    } catch (Exception e) {
                        log.error("Error writing to CSV - FID {} - MasseflyttFraFid {}", kobling.fid(), kobling.masseflyttetFraFid(), e);
                        throw e;
                    }
                });

            return IOUtils.toByteArray(IOUtils.toInputStream(writer.toString(), "UTF-8"));
        }
    }

    @Operation(
            description = "Returnerer csv med alle telefonnumre for ressurser",
            security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
            {
                    @ApiResponse(responseCode = "200", description = "Success")
            }
    )
    @GetMapping(value = "/telefon", produces = "text/csv")
    public @ResponseBody byte[] exportTelefon() throws IOException {
        if (lastCleared.isBefore(LocalDateTime.now().minusMinutes(45L)) || orgEnhetMedRessurserUtenforHierarki.isEmpty()) {
            ressurserUtenforHierarki();
        }

        if (!orgEnhetMedRessurserUtenforHierarki.isEmpty()) {
            var antallUtenforHierarki = orgEnhetMedRessurserUtenforHierarki.size();
            var antallRessurserUtenforHierarki = orgEnhetMedRessurserUtenforHierarki.values().stream().mapToInt(Integer::intValue).sum();
            log.error("Kunne ikke generere uttrekk, det er {} enheter med {} ansatte utenfor hierarki.", antallUtenforHierarki, antallRessurserUtenforHierarki);
            sendSlackMessage();
            throw new OrgtilknytningUtenforHierarkiException(String.format(
                "Kunne ikke generere uttrekk, det er %s enheter med %s ansatte utenfor hierarki.",
                antallUtenforHierarki,
                antallRessurserUtenforHierarki
            ));
        }
        StringWriter writer = new StringWriter();

        final var cSVFormat = getCSVFormat("navident", "nummer", "type", "beskrivelse");

        log.info("Eksporterer telefonnummer med ny aggregering");
        try (CSVPrinter printer = new CSVPrinter(writer, cSVFormat)) {
            telefonService.hentTelefonnummreForAktiveRessurser().forEach(
                telefon -> {
                    try {
                        printer.printRecord(
                            telefon.navident(),
                            formaterTelefonnummer(telefon.nummer()),
                            telefon.type(),
                            ""
                        );

                        writer.close();
                    } catch (IOException e) {
                        log.error("Error writing to CSV", e);
                        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Kunne ikke generere uttrekk");
                    }
                }
            );

            return IOUtils.toByteArray(IOUtils.toInputStream(writer.toString(), "UTF-8"));
        }
    }

    @Operation(
        description = "Returnerer csv med alle orgenheter, og deres leder og organisering",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/orgenhet", produces = "text/csv")
    public @ResponseBody byte[] exportOrgenhet() throws IOException {
        if (lastCleared.isBefore(LocalDateTime.now().minusMinutes(45L)) || orgEnhetMedRessurserUtenforHierarki.isEmpty()) {
            ressurserUtenforHierarki();
        }

        if (!orgEnhetMedRessurserUtenforHierarki.isEmpty()) {
            var antallUtenforHierarki = orgEnhetMedRessurserUtenforHierarki.size();
            var antallRessurserUtenforHierarki = orgEnhetMedRessurserUtenforHierarki.values().stream().mapToInt(Integer::intValue).sum();
            log.error("Kunne ikke generere uttrekk, det er {} enheter med {} ansatte utenfor hierarki.", antallUtenforHierarki, antallRessurserUtenforHierarki);
            sendSlackMessage();
            throw new OrgtilknytningUtenforHierarkiException(String.format(
                "Kunne ikke generere uttrekk, det er %s enheter med %s ansatte utenfor hierarki.",
                antallUtenforHierarki,
                antallRessurserUtenforHierarki
            ));
        }

        StringWriter writer = new StringWriter();

        log.debug("Eksporterer orgenheter. Bruker ny organisering");

        var kunHistoriskSluttdato = unleash.isEnabled(ORGENHET_KUN_HISTORISK_SLUTTDATO);

        final var cSVFormat =
            getCSVFormat(
                "org_enhet_id",
                "agresso_id",
                "agresso_org_niv",
                "agresso_orgenhet_type",
                "navn",
                "gyldig_fom",
                "gyldig_tom",
                "leder_navident",
                "org_enhets_nivaa",
                "org_enhet_type",
                "org_enhet_over",
                "remedyEnhetId"
            );

        try (CSVPrinter printer = new CSVPrinter(writer, cSVFormat)) {
            orgenhetService.getAllEnheter()
                .stream().filter(oe -> !oe.getId().equals(EXCLUDE_ORG_ENHET))
                .forEach(orgenhet -> {
                    var sluttdato =
                        nonNull(orgenhet.getGyldigTom()) && (!kunHistoriskSluttdato || orgenhet.getGyldigTom().minusDays(1).isBefore(LocalDate.now()))
                        ? orgenhet.getGyldigTom().format(dateTimeFormatter)
                        : "";
                    try {
                        printer.printRecord(
                            orgenhet.getId(),
                            orgenhet.getAgressoId(),
                            orgenhet.getOrgNiv(),
                            orgenhet.getAgressoOrgenhetType(),
                            orgenhet.getNavn(),
                            orgenhet.getGyldigFom().format(dateTimeFormatter),
                            sluttdato,
                            getLederNavident(orgenhet),
                            nonNull(orgenhet.getNomNivaa()) ? orgenhet.getNomNivaa().name() : "",
                            nonNull(orgenhet.getOrgEnhetsType()) ? orgenhet.getOrgEnhetsType().name() : "",
                            orgenhet.endedOnStartDate() ? "" : // Hvis enhet er avsluttet samme dag som den er opprettet, så er den ikke i organiseringen
                                getOrgenhetOver(orgenhet),
                            getRemedyEnhetId(orgenhet)
                        );
                        writer.close();
                    } catch (IOException e) {
                        log.error("Error writing to CSV", e);
                        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Kunne ikke generere uttrekk");
                    }
                });

            if (diffDvhVsLederRepo.get()) {
                log.warn("Det var forskjell mellom DVH leder og OrgEnhetLederState for en eller flere orgenheter");
            }

            return IOUtils.toByteArray(IOUtils.toInputStream(writer.toString(), "UTF-8"));
        }
    }

    private void sendSlackMessage() {
        if (slackSentForCurrentCycle.compareAndExchange(false, true)) {
            return;
        }
        try {
            slackService.sendOrgbotErrorOmadaUttrekk(
                String.format(
                    "KOI Rest-kallet feilet fordi det er %d enheter utenfor hierarki, det gjelder enhetene med disse NOM-ID: %s",
                    orgEnhetMedRessurserUtenforHierarki.size(),
                    orgEnhetMedRessurserUtenforHierarki.keySet()
                ));
        } catch (Exception e) {
            log.info("Kunne ikke sende slack melding: {}", e.getMessage(), e);
        }

    }

    private String getRemedyEnhetId(IOrgenhet orgenhet) {
        String remedyEnhet = remedyEnhetIdResolver.remedyEnhetId(orgenhet);

        if (unleash.isEnabled(ORGENHET_KUN_HISTORISK_SLUTTDATO) && (remedyEnhet == null || remedyEnhet.isEmpty())) {
            remedyEnhet = remedyEnhetIdResolver.remedyEnhetId(getGarsdagensOverEnhet(orgenhet));
        }
        return remedyEnhet;
    }

    private String getLederNavident(IOrgenhet orgenhet) {
        return orgEnhetLederStateRepository.getActiveForOrgEnhet(orgenhet.getId()).map(OrgEnhetLederState::navident).orElse("");
    }

    private String getPrimaryTelefonnummer(RessursState ressursState) {
        var nummer = ressursState.jobbtelefon() != null ? ressursState.jobbtelefon().nummer() : ressursState.primaryTelefonnummer();
        return formaterTelefonnummer(nummer);
    }

    String formaterTelefonnummer(String telefon) {
        if (telefon == null) {
            return "";
        }
        telefon = telefon.replace(" ", "");

        if (telefon.startsWith("+")) {
            return telefon;
        } else {
            if (telefon.length() == 10) {
                return "+" + telefon;
            } else {
                return "+47" + telefon;
            }
        }
    }

    private String getOrgenhetOver(IOrgenhet orgenhet) {
        var overenhet = organiseringStateRepository.findActiveById(orgenhet.getId())
            .map(OrganiseringState::idOver)
            .orElse("");

        if (!unleash.isEnabled(ORGENHET_KUN_HISTORISK_SLUTTDATO)) {
            return overenhet;
        }

        if (!overenhet.isEmpty()) {
            return overenhet;
        } else {
            // Hvis det ikke er aktiv overenhet, lever overenhet som var gyldig i forigårs, hvis den fantes
            return getGarsdagensOverEnhet(orgenhet);
        }
    }

    private String getGarsdagensOverEnhet(IOrgenhet orgenhet) {
        var today = LocalDate.now().minusDays(1);
        return organiseringStateRepository.findById(orgenhet.getId()).stream()
            .filter(org -> // Aktiv i går
                org.gyldigFom().isBefore(today) &&
                    (org.gyldigTom() == null || org.gyldigTom().isAfter(LocalDate.now().minusDays(3)))
            )
            .map(OrganiseringState::idOver)
            .findFirst()
            .orElse("");
    }

    private CSVFormat getCSVFormat(String... headers) {
        return CSVFormat.Builder.create()
                .setDelimiter(';')
                .setQuoteMode(CSVFormat.DEFAULT.getQuoteMode())
                .setHeader(headers)
                .setRecordSeparator("\n")
                .get();
    }

    private void ressurserUtenforHierarki() {
        orgEnhetMedRessurserUtenforHierarki.clear();
        slackSentForCurrentCycle.set(false);
        var aktiveOrgenheter = orgenhetService.getAktiveEnheterIOrganisasjonen();

        // inkluder HELFO, SKATT, RIKSREVISJONEN og VIKAFOSSEN, da disse kan være gyldige
        List<String> aktiveOrgenhetIder = new ArrayList<>(aktiveOrgenheter.stream().map(IOrgenhet::getId).toList());
        aktiveOrgenhetIder.add(orgenhetProperties.helfoId());
        aktiveOrgenhetIder.add(orgenhetProperties.skattId());
        aktiveOrgenhetIder.add(orgenhetProperties.riksrevisjonenId());
        aktiveOrgenhetIder.add(orgenhetProperties.vikafossenId());

        var oterUtenforHierarki = orgtilknytningService.getAll().stream()
            .filter(ot -> !aktiveOrgenhetIder.contains(ot.orgEnhetId()))
            .filter(ot -> likEllerEtter(ot.gyldigTom(), today()))
            .filter(ot -> !ot.fid().startsWith("FID_MANUELL"))
            .collect(Collectors.groupingBy(OrgtilknytningState::orgEnhetId));

        oterUtenforHierarki.forEach((nomId, orgtilknytningStates) -> orgEnhetMedRessurserUtenforHierarki.put(nomId, orgtilknytningStates.size()));
        lastCleared = LocalDateTime.now();
    }

    @ExceptionHandler(
        {
            OrgtilknytningUtenforHierarkiException.class
        }
    )
    public ResponseEntity<StandardResponse> handleConflict(OrgtilknytningUtenforHierarkiException ex) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(StandardResponse.builder().message(ex.getMessage()).build());
    }

    @ExceptionHandler(
        {
            Exception.class
        }
    )
    public ResponseEntity<StandardResponse> handleConflict(Exception ex) {
        log.error(ex.getMessage(), ex);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(StandardResponse.builder().message(String.format("Uforutsett feil: %s", ex.getMessage())).build());
    }
}
