package no.nav.nom.apps.api.ressurs.api.app;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.authorize.AuthResolver;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil;
import no.nav.nom.apps.api.ressurs.api.dto.Ressurs;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

@Slf4j
@Component
public class RessursExcelGenerator {

    private final OrgtilknytningStateRepository orgtilknytningStateRepository;
    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private final OrgenhetService orgenhetService;

    private static final Map<String, String> sektorMap = Map.of(
        "NAV_STATLIG", "Statlig",
        "NAV_KOMMUNAL", "Kommunal",
        "EKSTERN", "Ekstern"
    );
    private final AuthResolver authResolver;

    public RessursExcelGenerator(
        OrgtilknytningStateRepository orgtilknytningStateRepository,
        OrgEnhetStateRepository orgEnhetStateRepository, OrgenhetService orgenhetService,
        AuthResolver authResolver
    ) {
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
        this.orgEnhetStateRepository = orgEnhetStateRepository;
        this.orgenhetService = orgenhetService;
        this.authResolver = authResolver;
    }

    public void generate(HttpServletResponse response, List<Ressurs> ressurser, List<String> navidenter, String topOrgEnhetId) {
        var kanSeSluttdato = authResolver.kanOppretteRessurs(topOrgEnhetId);
        var underliggendeEnheter = orgenhetService.findAllUnderliggendeOrgenheter(topOrgEnhetId);
        var otMap = orgtilknytningStateRepository.findByNavidentIn(navidenter).stream()
            .filter(ot -> ot.orgEnhetId().equals(topOrgEnhetId) || underliggendeEnheter.contains(ot.orgEnhetId()))
            .filter(PeriodeUtil::isCurrentOrFuturePeriod)
            .collect(Collectors.groupingBy(
                OrgtilknytningState::navident
            ));

        var orgenhetNavnMap = orgEnhetStateRepository.findByIdIn(
            otMap.values().stream()
                .flatMap(List::stream)
                .map(OrgtilknytningState::orgEnhetId)
                .collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(OrgEnhetState::id, OrgEnhetState::navn));

        try (Workbook workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Ressurser");
            var rowcount = 0;

            addRow(sheet.createRow(rowcount++), null, null, null, kanSeSluttdato);

            for (Ressurs ressurs : ressurser) {
                var entry = otMap.get(ressurs.state().navident());
                if(entry == null || entry.isEmpty()){
                    log.error("Ressurs {} har ingen OTer.", ressurs.state().navident());
                    addRow(
                        sheet.createRow(rowcount++),
                        ressurs,
                        "UKJENT",
                        null,
                        kanSeSluttdato
                    );
                }else {
                    var gyldigFromCandidates = entry.stream().map(OrgtilknytningState::gyldigFom).filter(Objects::nonNull).toList();
                    var gyldigFom = Collections.min(gyldigFromCandidates);
                    var orgEnhet = otMap.get(ressurs.state().navident()).stream()
                        .map(ot -> String.format("%s (%s)", orgenhetNavnMap.get(ot.orgEnhetId()), ot.orgEnhetId()))
                        .collect(Collectors.joining(", "));

                    addRow(
                        sheet.createRow(rowcount++),
                        ressurs,
                        orgEnhet,
                        gyldigFom,
                        kanSeSluttdato
                    );
                }
            }

            var headerRow = sheet.getRow(0);
            var font = workbook.createFont();
            var style = workbook.createCellStyle();
            font.setBold(true);
            style.setFont(font);
            headerRow.cellIterator().forEachRemaining(cell -> cell.setCellStyle(style));
            sheet.createFreezePane(0, 1);

            workbook.write(response.getOutputStream());
        }catch (IOException ioException) {
            throw new RuntimeException("IO-Exception", ioException);
        }
        catch (Exception e) {
            log.error("Feil ved generering av ressurs-Excel", e);
            throw e;
        }
    }

    private static void addRow(Row row, Ressurs ressurs, String orgEnhetNavn, LocalDate gyldigFom, boolean kanSeSluttdato) {
        var navident = row.createCell(0, CellType.STRING);
        var navn = row.createCell(1, CellType.STRING);
        var sektor = row.createCell(2, CellType.STRING);
        var orgEnhet = row.createCell(3, CellType.STRING);
        var fom = row.createCell(4, CellType.STRING);
        var jobbtelefon = row.createCell(5, CellType.STRING);
        var privatTelefon = row.createCell(6, CellType.STRING);
        var epost = row.createCell(7, CellType.STRING);
        var sluttdato = row.createCell(8, CellType.STRING);


        if (ressurs == null) {
            navident.setCellValue("Navident");
            navn.setCellValue("Navn");
            sektor.setCellValue("Sektor");
            orgEnhet.setCellValue("Organisasjonsenhet");
            fom.setCellValue("Fra og med");
            jobbtelefon.setCellValue("Jobbtelefon");
            privatTelefon.setCellValue("Privat telefon");
            epost.setCellValue("Epost");
            if (kanSeSluttdato) {
                sluttdato.setCellValue("Sluttdato");
            }
        } else {
            navident.setCellValue(ressurs.state().navident());
            navn.setCellValue(ressurs.state().visningsnavn().toString());
            sektor.setCellValue(sektorMap.get(ressurs.state().sektor().name()));
            orgEnhet.setCellValue(orgEnhetNavn);
            fom.setCellValue(gyldigFom == null ? "UKJENT" : gyldigFom.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
            jobbtelefon.setCellValue(isNull(ressurs.state().jobbtelefon().nummer()) ? "" : ressurs.state().jobbtelefon().nummer());
            ressurs.state().primaryTelefonnummer().ifPresentOrElse(privatTelefon::setCellValue, () -> privatTelefon.setCellValue(""));
            epost.setCellValue(ressurs.epost());
            if (kanSeSluttdato) {
                ressurs.state().sluttdato().ifPresentOrElse(
                    gyldigTom -> sluttdato.setCellValue(gyldigTom.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))),
                    () -> sluttdato.setCellValue("")
                );
            }
        }
    }
}
