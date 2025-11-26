package no.nav.nom.apps.api.rest.uttrekk;

import io.getunleash.Unleash;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.graphql.resolvers.RemedyEnhetIdResolver;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.telefon.TelefonService;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import no.nav.nom.libs.state.orgenhetleder.OrgEnhetLederStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.StringWriter;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Objects.nonNull;

@Tag(name = "Omada uttrekk")
@RestController
@RequestMapping("/api/v1/uttrekk/omada")
public class OmadaUttrekk {
    public static final String BRUK_NY_TELEFON_AGGREGERING = "nom.telefon.bruk_ny_telefon_aggregering";
    private final String EXCLUDE_ORG_ENHET = "bu282j"; // bu282j;0000000;ORGENHET;;Deltar i Rekruttering;01-05-2019;;;;
    private final Logger log = LoggerFactory.getLogger(OmadaUttrekk.class);
    private final RessursStateRepository ressursStateRepository;
    private final OrgenhetService orgenhetService;
    private final OrgtilknytningService orgtilknytningService;
    private final RemedyEnhetIdResolver remedyEnhetIdResolver;
    private final DvhRepository dvhRepository;
    private final OrganiseringStateRepository organiseringStateRepository;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private final TelefonService telefonService;
    private final OrgEnhetLederStateRepository orgEnhetLederStateRepository;
    private final Unleash unleash;

    private AtomicBoolean diffDvhVsLederRepo = new AtomicBoolean(false);

    public OmadaUttrekk(
        RessursStateRepository ressursStateRepository,
        OrgenhetService orgenhetService,
        OrgtilknytningService orgtilknytningService,
        RemedyEnhetIdResolver remedyEnhetIdResolver,
        DvhRepository dvhRepository,
        OrganiseringStateRepository organiseringStateRepository,
        TelefonService telefonService, OrgEnhetLederStateRepository orgEnhetLederStateRepository,
        Unleash unleash
    ) {
        this.ressursStateRepository = ressursStateRepository;
        this.orgenhetService = orgenhetService;
        this.orgtilknytningService = orgtilknytningService;
        this.remedyEnhetIdResolver = remedyEnhetIdResolver;
        this.dvhRepository = dvhRepository;
        this.organiseringStateRepository = organiseringStateRepository;
        this.telefonService = telefonService;
        this.orgEnhetLederStateRepository = orgEnhetLederStateRepository;
        this.unleash = unleash;
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
        StringWriter writer = new StringWriter();

        final var cSVFormat = getCSVFormat("nomId", "navident", "fornavn", "mellomnavn", "etternavn", "visningsnavn", "sektor", "sluttdato", "startdato");

        try (CSVPrinter printer = new CSVPrinter(writer, cSVFormat)) {
            ressursStateRepository.getAll().forEach(ressurs -> {
                try {
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
        StringWriter writer = new StringWriter();

        final var cSVFormat = getCSVFormat(
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
                        printer.printRecord(
                            kobling.navident(),
                            kobling.orgEnhetId(),
                            kobling.fid(),
                            kobling.erDagligOppfolging(),
                            kobling.gyldigFom().format(dateTimeFormatter),
                            nonNull(kobling.gyldigTom()) ? kobling.gyldigTom().format(dateTimeFormatter) : "",
                            Objects.requireNonNullElse(kobling.masseflyttetFraFid(), "")
                        );

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
        StringWriter writer = new StringWriter();

        final var cSVFormat = getCSVFormat("navident", "nummer", "type", "beskrivelse");

        if (unleash.isEnabled(BRUK_NY_TELEFON_AGGREGERING)) {
            log.info("Eksporterer telefonnummer med ny aggregering");
            try (CSVPrinter printer = new CSVPrinter(writer, cSVFormat)) {
                telefonService.hentPrioriterteTelefonnummer().forEach(
                        (navident, telefon) -> {
                            var prioritertNummer = telefon.getFirst();
                            try {
                                printer.printRecord(
                                        navident,
                                        formaterTelefonnummer(prioritertNummer.nummer()),
                                        prioritertNummer.type(),
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
        } else {
            log.info("Eksporterer telefonnummer på gammel måte");
            try (CSVPrinter printer = new CSVPrinter(writer, cSVFormat)) {
                dvhRepository.getNavidentTelefonNrList().forEach(telefon -> {
                    try {
                        printer.printRecord(
                                telefon.getNavIdent(),
                                telefon.getNummer(),
                                telefon.getType(),
                                telefon.getBeskrivelse()
                        );

                        writer.close();
                    } catch (IOException e) {
                        log.error("Error writing to CSV", e);
                        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Kunne ikke generere uttrekk");
                    }
                });
                return IOUtils.toByteArray(IOUtils.toInputStream(writer.toString(), "UTF-8"));
            }
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
        StringWriter writer = new StringWriter();

        log.debug("Eksporterer orgenheter. Bruker ny organisering");

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
                    try {
                        printer.printRecord(
                            orgenhet.getId(),
                            orgenhet.getAgressoId(),
                            orgenhet.getOrgNiv(),
                            orgenhet.getAgressoOrgenhetType(),
                            orgenhet.getNavn(),
                            orgenhet.getGyldigFom().format(dateTimeFormatter),
                            nonNull(orgenhet.getGyldigTom()) ? orgenhet.getGyldigTom().format(dateTimeFormatter) : "",
                            getLederNavident(orgenhet),
                            nonNull(orgenhet.getNomNivaa()) ? orgenhet.getNomNivaa().name() : "",
                            nonNull(orgenhet.getOrgEnhetsType()) ? orgenhet.getOrgEnhetsType().name() : "",
                            orgenhet.endedOnStartDate() ? "" : // Hvis enhet er avsluttet samme dag som den er opprettet, så er den ikke i organiseringen
                                getOrgenhetOver(orgenhet),
                            remedyEnhetIdResolver.remedyEnhetId(orgenhet)
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

    private String getLederNavident(IOrgenhet orgenhet) {
        return orgEnhetLederStateRepository.getActiveForOrgEnhet(orgenhet.getId()).map(OrgEnhetLederState::navident).orElse("");
    }

    String formaterTelefonnummer(String telefon) {
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
        return organiseringStateRepository.findActiveById(orgenhet.getId())
            .map(OrganiseringState::idOver)
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
}
