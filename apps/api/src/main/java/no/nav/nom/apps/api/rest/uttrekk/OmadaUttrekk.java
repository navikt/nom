package no.nav.nom.apps.api.rest.uttrekk;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.dto.DvhLeder;
import no.nav.nom.apps.api.graphql.organisasjonsenhet.RemedyEnhetIdResolver;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningStateService;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateRepository;
import no.nav.nom.contract.nom.NomNivaa;
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

@Tag(name = "Omada uttrekk")
@RestController
@RequestMapping("/api/v1/uttrekk/omada")
public class OmadaUttrekk {
    private final String EXCLUDE_ORG_ENHET = "bu282j"; // bu282j;0000000;ORGENHET;;Deltar i Rekruttering;01-05-2019;;;;
    private final Logger log = LoggerFactory.getLogger(OmadaUttrekk.class);
    private final RessursStateRepository ressursStateRepository;
    private final OrgenhetService orgenhetService;
    private final OrgtilknytningStateService orgtilknytningStateService;
    private final RemedyEnhetIdResolver remedyEnhetIdResolver;
    private final DvhRepository dvhRepository;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public OmadaUttrekk(
        RessursStateRepository ressursStateRepository, OrgenhetService orgenhetService,
        OrgtilknytningStateService orgtilknytningStateService,
        RemedyEnhetIdResolver remedyEnhetIdResolver, DvhRepository dvhRepository
    ) {
        this.ressursStateRepository = ressursStateRepository;
        this.orgenhetService = orgenhetService;
        this.orgtilknytningStateService = orgtilknytningStateService;
        this.remedyEnhetIdResolver = remedyEnhetIdResolver;
        this.dvhRepository = dvhRepository;
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

        final var cSVFormat = getCSVFormat("nomId", "navident", "fornavn", "mellomnavn", "etternavn", "visningsnavn", "sektor", "sluttdato");

        try (CSVPrinter printer = new CSVPrinter(writer, cSVFormat)) {
            ressursStateRepository.getAll().forEach(ressurs -> {
                try {
                    printer.printRecord(
                        ressurs.fid(),
                        ressurs.navident(),
                        ressurs.visningsnavn().fornavn(),
                        ressurs.visningsnavn().mellomnavn().orElse(""),
                        ressurs.visningsnavn().etternavn(),
                        ressurs.visningsnavn().getFormattedVisningsnavn(),
                        ressurs.sektor(),
                        ressurs.sluttdato().map(dateTimeFormatter::format).orElse("")
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

        final var cSVFormat = getCSVFormat("navident", "org_enhet_id", "fid", "erDagligOppfolging", "gyldig_fom", "gyldig_tom");

        try (CSVPrinter printer = new CSVPrinter(writer, cSVFormat)) {
            orgtilknytningStateService.getAll()
                .stream().filter( ot -> !ot.orgEnhetId().equals(EXCLUDE_ORG_ENHET))
                .forEach(kobling -> {
                    try {
                        printer.printRecord(
                            kobling.navident(),
                            kobling.orgEnhetId(),
                            kobling.fid(),
                            kobling.erDagligOppfolging(),
                            kobling.gyldigFom().format(dateTimeFormatter),
                            kobling.gyldigTom().map(dateTimeFormatter::format).orElse("")
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

        try (CSVPrinter printer = new CSVPrinter(writer, cSVFormat)) {
            dvhRepository.getNavidentTelefonNrMap().forEach(telefon -> {
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

        final var cSVFormat = getCSVFormat(
            "org_enhet_id",
            "agresso_id",
            "agresso_org_niv",
            "agresso_orgenhet_type",
            "navn",
            "gyldig_fom",
            "gyldig_tom",
            "leder_navident",
            "org_enhets_kategori",
            "org_enhet_over",
            "remedyEnhetId"
        );

        try (CSVPrinter printer = new CSVPrinter(writer, cSVFormat)) {
            orgenhetService.getAllEnheter()
                .stream().filter( oe -> !oe.getId().equals(EXCLUDE_ORG_ENHET))
                .forEach(orgenhet -> {
                try {
                    printer.printRecord(
                        orgenhet.getId(),
                        orgenhet.getAgressoId(),
                        orgenhet.getOrgNiv(),
                        orgenhet.getAgressoOrgenhetType(),
                        orgenhet.getNavn(),
                        orgenhet.getGyldigFom().format(dateTimeFormatter),
                        orgenhet.getGyldigTom().map(dateTimeFormatter::format).orElse(""),
                        dvhRepository.getLedere().stream()
                            .filter(leder -> leder.getOrgEnhetNr().equals(orgenhet.getAgressoId()) && leder.getOrgNiv()
                                .equals(orgenhet.getOrgNiv()) && leder.getOrgenhetNavn().equals(orgenhet.getNavn()))
                            .findAny()
                            .map(DvhLeder::getNavIdent)
                            .orElse(""),
                        orgenhet.getNomNivaa().map(NomNivaa::name).orElse(""),
                        dvhRepository.getOrganiseringer(orgenhet.getAgressoId(), orgenhet.getOrgNiv(), orgenhet.getNavn()).stream()
                            .filter(organisering ->
                                organisering.getAgressoIdUnder().equals(orgenhet.getAgressoId()) &&
                                    organisering.getOrgNivUnder().equals(orgenhet.getOrgNiv()) &&
                                    organisering.getNavnUnder().equals(orgenhet.getNavn())
                            )
                            .findAny()
                            .map(organisering -> orgenhetService.getEnhetByAgressoIdNivaAndName(
                                organisering.getAgressoIdOver(),
                                organisering.getOrgNivOver(),
                                organisering.getNavnOver()
                            ).map(IOrgenhet::getId).orElse(null))
                            .orElse(""),
                        remedyEnhetIdResolver.remedyEnhetId(orgenhet)
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


    private CSVFormat getCSVFormat(String... headers) {
        return CSVFormat.Builder.create()
            .setDelimiter(';')
            .setQuoteMode(CSVFormat.DEFAULT.getQuoteMode())
            .setHeader(headers)
            .setRecordSeparator("\n")
            .build();
    }
}
