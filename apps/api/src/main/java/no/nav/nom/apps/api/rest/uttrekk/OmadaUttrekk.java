package no.nav.nom.apps.api.rest.uttrekk;

import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.dto.DvhLeder;
import no.nav.nom.apps.api.kobling.KoblingDvhService;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.IOrgenhetService;
import no.nav.nom.apps.api.ressurs.IRessursService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.StringWriter;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/uttrekk/omada")
public class OmadaUttrekk {

    private final Logger log = LoggerFactory.getLogger(OmadaUttrekk.class);
    private final IRessursService ressursService;
    private final IOrgenhetService orgenhetService;

    private final KoblingDvhService koblingDvhService;

    private final DvhRepository dvhRepository;

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");


    public OmadaUttrekk(IRessursService ressursService, IOrgenhetService orgenhetService, KoblingDvhService koblingDvhService, DvhRepository dvhRepository) {
        this.ressursService = ressursService;
        this.orgenhetService = orgenhetService;
        this.koblingDvhService = koblingDvhService;
        this.dvhRepository = dvhRepository;
    }

    @GetMapping(value = "/ressurs", produces = "text/csv")
    public @ResponseBody byte[] exportRessurs() throws IOException {
        StringWriter writer = new StringWriter();

        final var cSVFormat = getCSVFormat("nomId", "navident", "fornavn", "mellomnavn", "etternavn", "visningsnavn", "sektor", "sluttdato");

        try(CSVPrinter printer = new CSVPrinter(writer, cSVFormat)) {
            ressursService.getAll().forEach(ressurs -> {
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

    @GetMapping(value = "/orgtilknytning", produces = "text/csv")
    public @ResponseBody byte[] exportOrgtilknytning() throws IOException {
        StringWriter writer = new StringWriter();

        final var cSVFormat = getCSVFormat("navident", "org_enhet_id", "gyldig_fom", "gyldig_tom");

        try(CSVPrinter printer = new CSVPrinter(writer, cSVFormat)) {

            koblingDvhService.getAllKoblinger().forEach(kobling -> {
                try {
                    printer.printRecord(
                        kobling.getNavIdent(),
                        orgenhetService.getEnhetByAgressoIdNivaAndName(kobling.getAgressoId(), kobling.getOrgNiv(), kobling.getOrgenhetNavn()).map(IOrgenhet::getId).orElseThrow(),
                        kobling.getGyldigFom().format(dateTimeFormatter),
                        kobling.getGyldigTom().map(dateTimeFormatter::format).orElse("")
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

    @GetMapping(value = "/telefon", produces = "text/csv")
    public @ResponseBody byte[] exportTelefon() throws IOException {
        StringWriter writer = new StringWriter();

        final var cSVFormat = getCSVFormat("navident", "nummer", "type", "beskrivelse");

        try(CSVPrinter printer = new CSVPrinter(writer, cSVFormat)) {
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

    @GetMapping(value = "/orgenheter", produces = "text/csv")
    public @ResponseBody byte[] exportOrgenhet() throws IOException {
        StringWriter writer = new StringWriter();

        final var cSVFormat = getCSVFormat("org_enhet_id", "agresso_id", "agresso_org_niv", "agresso_orgenhet_type", "navn", "gyldig_fom", "gyldig_tom", "leder_navident", "org_enhets_kategori", "org_enhet_over");

        try(CSVPrinter printer = new CSVPrinter(writer, cSVFormat)) {
            orgenhetService.getAllEnheter().forEach(orgenhet -> {
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
                                .filter(leder -> leder.getOrgEnhetNr().equals(orgenhet.getAgressoId()) && leder.getOrgNiv().equals(orgenhet.getOrgNiv()) && leder.getOrgenhetNavn().equals(orgenhet.getNavn()))
                                    .findAny()
                                    .map(DvhLeder::getNavIdent)
                                    .orElse(""),
                        orgenhet.getOrgEnhetsKategori(),
                        dvhRepository.getOrganiseringer(orgenhet.getAgressoId(), orgenhet.getOrgNiv(), orgenhet.getNavn()).stream()
                            .filter(organisering ->
                                    organisering.getAgressoIdUnder().equals(orgenhet.getAgressoId()) &&
                                    organisering.getOrgNivUnder().equals(orgenhet.getOrgNiv()) &&
                                    organisering.getNavnUnder().equals(orgenhet.getNavn())
                            )
                            .findAny()
                            .map(organisering -> orgenhetService.getEnhetByAgressoIdNivaAndName(organisering.getAgressoIdOver(), organisering.getOrgNivOver(), organisering.getNavnOver()).map(IOrgenhet::getId).orElse(null))
                            .orElse("")
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
